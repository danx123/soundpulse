package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.local.EqualizerStore
import com.example.data.model.AudioTrack
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "SoundPulseEngine"
private const val REVERB_SEND_LEVEL = 0.8f

enum class RepeatMode {
    OFF, ALL, ONE
}

class AudioPlayerEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var hardwareEqualizer: Equalizer? = null
    private var hardwareBassBoost: BassBoost? = null
    private var hardwareVirtualizer: Virtualizer? = null
    private var hardwareReverb: PresetReverb? = null
    private var reverbPlayer: MediaPlayer? = null
    private var hardwareLoudnessEnhancer: LoudnessEnhancer? = null

    private val visualizerSimulator = VisualizerSimulator()

    // Visualizer asli (FFT dari output audio) + pemroses band beat-reactive
    private val fftProcessor = FftBandProcessor()
    private var hardwareVisualizer: Visualizer? = null
    @Volatile private var latestFft: ByteArray? = null
    @Volatile private var latestFftRate: Int = 44100000
    private var visualizerActive = false
    private var currentAudioSessionId = 0

    // Penyimpanan permanen pengaturan equalizer
    private val equalizerStore = EqualizerStore(context)
    private var saveEqJob: Job? = null

    // State flows
    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _equalizerState = MutableStateFlow(equalizerStore.load())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _visualizerAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var tickerJob: Job? = null
    private var onTrackCompletedListener: (() -> Unit)? = null
    private var onTrackStartedListener: ((AudioTrack, Long) -> Unit)? = null

    private val playerLock = Any()
    private var playJob: Job? = null
    private var currentPlayToken: Long = 0L

    init {
        startTicker()
    }

    fun setOnTrackCompletedListener(listener: () -> Unit) {
        onTrackCompletedListener = listener
    }

    fun setOnTrackStartedListener(listener: (AudioTrack, Long) -> Unit) {
        onTrackStartedListener = listener
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                try {
                    val player = mediaPlayer
                    if (player != null && _isPlaying.value) {
                        val pos = player.currentPosition.toLong()
                        _currentPositionMs.value = pos
                        val dur = player.duration.toLong().coerceAtLeast(0L)
                        if (dur > 0 && _durationMs.value <= 0) {
                            _durationMs.value = dur
                        }
                    }

                    // Update visualizer: pakai FFT asli bila tersedia, kalau tidak pakai simulasi
                    val realFrame = visualizerFrame(_isPlaying.value)
                    _visualizerAmplitudes.value = realFrame ?: run {
                        val eqBands = _equalizerState.value.bands.map { it.levelMillibels }
                        visualizerSimulator.updateAmplitudes(
                            isPlaying = _isPlaying.value,
                            positionMs = _currentPositionMs.value,
                            bassBoost = _equalizerState.value.bassBoostStrength,
                            equalizerBands = eqBands
                        )
                    }
                } catch (e: Exception) {
                    // Ignore transient playback state exceptions
                }
                delay(40) // ~25 fps agar bar responsif terhadap beat
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        val token: Long
        synchronized(playerLock) {
            currentPlayToken++
            token = currentPlayToken
        }
        playJob?.cancel()

        _currentTrack.value = track
        _currentPositionMs.value = 0L
        _durationMs.value = if (track.durationMs > 0) track.durationMs else 0L
        _isBuffering.value = true
        _errorMessage.value = null

        playJob = scope.launch(Dispatchers.IO) {
            try {
                synchronized(playerLock) {
                    if (token != currentPlayToken) return@launch
                    releaseMediaPlayerLocked()
                }

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )

                    val uri = Uri.parse(track.uri)
                    if (track.uri.startsWith("content://") || track.uri.startsWith("android.resource://")) {
                        setDataSource(context, uri)
                    } else {
                        // HTTP/HTTPS stream or local path
                        setDataSource(track.uri)
                    }

                    setOnPreparedListener { mp ->
                        synchronized(playerLock) {
                            if (token != currentPlayToken) {
                                try { mp.stop() } catch (_: Exception) {}
                                try { mp.release() } catch (_: Exception) {}
                                return@setOnPreparedListener
                            }
                            mediaPlayer = mp
                        }
                        scope.launch(Dispatchers.Main) {
                            synchronized(playerLock) {
                                if (token != currentPlayToken || mp != mediaPlayer) {
                                    try { mp.stop() } catch (_: Exception) {}
                                    try { mp.release() } catch (_: Exception) {}
                                    return@launch
                                }
                            }
                            _isBuffering.value = false
                            val dur = if (mp.duration > 0) mp.duration.toLong() else track.durationMs
                            _durationMs.value = dur
                            _currentPositionMs.value = 0L
                            setupAudioEffects(mp.audioSessionId, mp)
                            setupVisualizer(mp.audioSessionId)
                            applyPlaybackSpeedInternal(_playbackSpeed.value)
                            mp.start()
                            _isPlaying.value = true
                            onTrackStartedListener?.invoke(track, dur)
                        }
                    }

                    setOnCompletionListener {
                        synchronized(playerLock) {
                            if (token != currentPlayToken) return@setOnCompletionListener
                        }
                        scope.launch(Dispatchers.Main) {
                            when (_repeatMode.value) {
                                RepeatMode.ONE -> {
                                    seekTo(0L)
                                    start()
                                }
                                else -> {
                                    _isPlaying.value = false
                                    onTrackCompletedListener?.invoke()
                                }
                            }
                        }
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                        synchronized(playerLock) {
                            if (token != currentPlayToken) return@setOnErrorListener true
                        }
                        scope.launch(Dispatchers.Main) {
                            _isBuffering.value = false
                            _isPlaying.value = false
                            _errorMessage.value = "Playback error ($what, $extra). Coba putar track lain atau cek koneksi."
                        }
                        true
                    }

                    prepareAsync()
                }

                synchronized(playerLock) {
                    if (token != currentPlayToken) {
                        try { player.release() } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating playback", e)
                synchronized(playerLock) {
                    if (token != currentPlayToken) return@launch
                }
                scope.launch(Dispatchers.Main) {
                    _isBuffering.value = false
                    _isPlaying.value = false
                    _errorMessage.value = "Gagal memutar audio: ${e.localizedMessage ?: "File tidak valid"}"
                }
            }
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            _currentTrack.value?.let { playTrack(it) }
            return
        }
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause", e)
        }
    }

    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking", e)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeedInternal(speed)
    }

    private fun applyPlaybackSpeedInternal(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    val params = player.playbackParams
                    params.speed = speed
                    player.playbackParams = params
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to set playback speed", e)
            }
        }
    }

    fun toggleShuffle() {
        _shuffleMode.value = !_shuffleMode.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    // --- Audio Effects & Equalizer ---

    private fun setupAudioEffects(audioSessionId: Int, player: MediaPlayer? = null) {
        if (audioSessionId == 0) return

        try {
            releaseAudioEffects()

            val eq = Equalizer(0, audioSessionId).apply {
                enabled = _equalizerState.value.isEnabled
            }
            hardwareEqualizer = eq

            val minLvl = eq.bandLevelRange?.getOrNull(0)?.toInt() ?: -1500
            val maxLvl = eq.bandLevelRange?.getOrNull(1)?.toInt() ?: 1500

            val currentBands = _equalizerState.value.bands.mapIndexed { idx, oldBand ->
                if (idx < eq.numberOfBands) {
                    val centerFreq = eq.getCenterFreq(idx.toShort()) / 1000 // Convert mHz to Hz
                    EqualizerBand(idx, centerFreq, oldBand.levelMillibels)
                } else {
                    oldBand
                }
            }

            _equalizerState.value = _equalizerState.value.copy(
                bands = currentBands,
                minLevelMillibels = minLvl,
                maxLevelMillibels = maxLvl
            )

            // Apply current band levels
            currentBands.forEach { band ->
                if (band.index < eq.numberOfBands) {
                    eq.setBandLevel(band.index.toShort(), band.levelMillibels.toShort())
                }
            }

            // BassBoost
            try {
                val bb = BassBoost(0, audioSessionId).apply {
                    enabled = _equalizerState.value.isEnabled
                    if (strengthSupported) {
                        setStrength(_equalizerState.value.bassBoostStrength.toShort())
                    }
                }
                hardwareBassBoost = bb
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost not supported on this device/session", e)
            }

            // Virtualizer
            try {
                val virt = Virtualizer(0, audioSessionId).apply {
                    enabled = _equalizerState.value.isEnabled
                    if (strengthSupported) {
                        setStrength(_equalizerState.value.virtualizerStrength.toShort())
                    }
                }
                hardwareVirtualizer = virt
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer not supported", e)
            }

            // PresetReverb (auxiliary effect)
            setupReverb(player)

            // LoudnessEnhancer (ReplayGain / Normalisasi Audio)
            try {
                val le = LoudnessEnhancer(audioSessionId).apply {
                    val targetmB = (_equalizerState.value.replayGainTargetDb * 100).toInt().coerceIn(0, 3000)
                    setTargetGain(targetmB)
                    enabled = _equalizerState.value.isEnabled && _equalizerState.value.isReplayGainEnabled
                }
                hardwareLoudnessEnhancer = le
            } catch (e: Exception) {
                Log.w(TAG, "LoudnessEnhancer not supported on this session", e)
            }
            applyTrackVolumeNormalization(player)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to setup hardware audio effects, using simulated equalizer", e)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
        persistEqualizer()
        try {
            hardwareEqualizer?.enabled = enabled
            // Terapkan ulang level band yang diubah saat EQ nonaktif
            val eq = hardwareEqualizer
            if (enabled && eq != null) {
                val hwBands = eq.numberOfBands.toInt()
                _equalizerState.value.bands.forEach { band ->
                    if (band.index < hwBands) {
                        eq.setBandLevel(band.index.toShort(), band.levelMillibels.toShort())
                    }
                }
            }
            hardwareBassBoost?.enabled = enabled
            hardwareVirtualizer?.enabled = enabled
            hardwareLoudnessEnhancer?.enabled = enabled && _equalizerState.value.isReplayGainEnabled
            applyTrackVolumeNormalization()
        } catch (e: Exception) {
            Log.w(TAG, "Error enabling/disabling audiofx", e)
        }
        applyReverbToHardware()
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val updatedBands = _equalizerState.value.bands.map { band ->
            if (band.index == bandIndex) band.copy(levelMillibels = levelMillibels) else band
        }
        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            currentPreset = "Custom"
        )
        persistEqualizer()

        try {
            if (_equalizerState.value.isEnabled) {
                hardwareEqualizer?.setBandLevel(bandIndex.toShort(), levelMillibels.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting band level $bandIndex", e)
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        val updatedBands = _equalizerState.value.bands.mapIndexed { idx, band ->
            val newLvl = preset.levelsMillibels.getOrElse(idx) { 0 }
            try {
                if (_equalizerState.value.isEnabled) {
                    hardwareEqualizer?.setBandLevel(idx.toShort(), newLvl.toShort())
                }
            } catch (e: Exception) {
                // ignore
            }
            band.copy(levelMillibels = newLvl)
        }

        setBassBoostStrength(preset.bassBoost)
        setVirtualizerStrength(preset.virtualizer)

        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            currentPreset = preset.name,
            bassBoostStrength = preset.bassBoost,
            virtualizerStrength = preset.virtualizer,
            reverbEffect = preset.reverb
        )
        persistEqualizer()
        applyReverbToHardware()
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = clamped)
        persistEqualizer()
        try {
            if (hardwareBassBoost?.strengthSupported == true) {
                hardwareBassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting BassBoost", e)
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(virtualizerStrength = clamped)
        persistEqualizer()
        try {
            if (hardwareVirtualizer?.strengthSupported == true) {
                hardwareVirtualizer?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting Virtualizer", e)
        }
    }

    fun setReverb(reverbName: String) {
        _equalizerState.value = _equalizerState.value.copy(reverbEffect = reverbName)
        persistEqualizer()
        applyReverbToHardware()
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isReplayGainEnabled = enabled)
        persistEqualizer()
        applyReplayGainToHardware()
    }

    fun setReplayGainTargetDb(targetDb: Float) {
        val clamped = targetDb.coerceIn(0f, 12f)
        _equalizerState.value = _equalizerState.value.copy(replayGainTargetDb = clamped)
        persistEqualizer()
        applyReplayGainToHardware()
    }

    private fun applyReplayGainToHardware() {
        try {
            val state = _equalizerState.value
            val active = state.isEnabled && state.isReplayGainEnabled
            hardwareLoudnessEnhancer?.let { le ->
                val targetmB = (state.replayGainTargetDb * 100).toInt().coerceIn(0, 3000)
                le.setTargetGain(targetmB)
                le.enabled = active
            }
            applyTrackVolumeNormalization()
        } catch (e: Exception) {
            Log.w(TAG, "Error applying ReplayGain / LoudnessEnhancer", e)
        }
    }

    private fun applyTrackVolumeNormalization(player: MediaPlayer? = mediaPlayer) {
        val p = player ?: return
        val state = _equalizerState.value
        try {
            if (state.isEnabled && state.isReplayGainEnabled) {
                // Headroom 0.95f untuk mencegah distorsi puncak saat penguatan dinamis aktif
                val vol = 0.95f
                p.setVolume(vol, vol)
            } else {
                p.setVolume(1.0f, 1.0f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying track volume normalization", e)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun releaseAudioEffects() {
        try {
            hardwareEqualizer?.release()
            hardwareBassBoost?.release()
            hardwareVirtualizer?.release()
            hardwareReverb?.release()
            hardwareLoudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio effects", e)
        } finally {
            hardwareEqualizer = null
            hardwareBassBoost = null
            hardwareVirtualizer = null
            hardwareReverb = null
            hardwareLoudnessEnhancer = null
            reverbPlayer = null
        }
    }

    private fun releaseMediaPlayerLocked() {
        try {
            releaseVisualizer()
            releaseAudioEffects()
            mediaPlayer?.let {
                try { it.stop() } catch (_: Exception) {}
                try { it.release() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun releaseMediaPlayer() {
        synchronized(playerLock) {
            releaseMediaPlayerLocked()
        }
    }

    fun release() {
        tickerJob?.cancel()
        saveEqJob?.cancel()
        playJob?.cancel()
        equalizerStore.save(_equalizerState.value)
        releaseMediaPlayer()
    }

    // --- Reverb (PresetReverb sebagai auxiliary effect) ---

    private fun reverbPresetFor(name: String): Short? = when (name.trim().lowercase()) {
        "small room" -> PresetReverb.PRESET_SMALLROOM
        "medium room" -> PresetReverb.PRESET_MEDIUMROOM
        "large room" -> PresetReverb.PRESET_LARGEROOM
        "hall", "medium hall" -> PresetReverb.PRESET_MEDIUMHALL
        "large hall" -> PresetReverb.PRESET_LARGEHALL
        "plate" -> PresetReverb.PRESET_PLATE
        else -> null // "None"
    }

    /**
     * Reverb di Android sebaiknya dipasang sebagai auxiliary effect:
     * dibuat di session 0 lalu di-attach ke MediaPlayer dengan send level.
     */
    private fun setupReverb(player: MediaPlayer?) {
        if (player == null) return
        try {
            val reverb = PresetReverb(0, 0)
            hardwareReverb = reverb
            reverbPlayer = player
            player.attachAuxEffect(reverb.id)
            applyReverbToHardware()
        } catch (e: Exception) {
            Log.w(TAG, "PresetReverb tidak didukung di perangkat ini", e)
            hardwareReverb = null
            reverbPlayer = null
        }
    }

    private fun applyReverbToHardware() {
        val reverb = hardwareReverb ?: return
        val state = _equalizerState.value
        val preset = reverbPresetFor(state.reverbEffect)
        val active = state.isEnabled && preset != null
        try {
            reverb.preset = preset ?: PresetReverb.PRESET_NONE
            reverb.enabled = active
            reverbPlayer?.setAuxEffectSendLevel(if (active) REVERB_SEND_LEVEL else 0f)
        } catch (e: Exception) {
            Log.w(TAG, "Error menerapkan reverb", e)
        }
    }

    // --- Persistensi equalizer ---

    /** Simpan dengan debounce agar drag slider tidak menulis disk berkali-kali. */
    private fun persistEqualizer() {
        saveEqJob?.cancel()
        saveEqJob = scope.launch(Dispatchers.IO) {
            delay(300)
            equalizerStore.save(_equalizerState.value)
        }
    }

    // --- Visualizer asli (FFT) ---

    /** Dipanggil setelah user memberi izin RECORD_AUDIO saat lagu sedang diputar. */
    fun onVisualizerPermissionGranted() {
        setupVisualizer(currentAudioSessionId)
    }

    @Synchronized
    private fun setupVisualizer(sessionId: Int) {
        releaseVisualizer()
        currentAudioSessionId = sessionId
        if (sessionId == 0) return

        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return // fallback ke visualizer simulasi

        try {
            val viz = Visualizer(sessionId)
            viz.enabled = false
            viz.captureSize = Visualizer.getCaptureSizeRange()[1]
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) {
                            latestFft = fft.copyOf()
                            latestFftRate = samplingRate
                        }
                    }
                },
                Visualizer.getMaxCaptureRate(),
                false,
                true
            )
            hardwareVisualizer = viz
            visualizerActive = false
            fftProcessor.reset()
        } catch (e: Exception) {
            Log.w(TAG, "Visualizer tidak tersedia, memakai visualizer simulasi", e)
            hardwareVisualizer = null
        }
    }

    /** Mengembalikan amplitudo dari FFT asli, atau null bila visualizer asli tidak aktif. */
    @Synchronized
    private fun visualizerFrame(playing: Boolean): List<Float>? {
        val viz = hardwareVisualizer ?: return null
        try {
            if (visualizerActive != playing) {
                viz.enabled = playing
                visualizerActive = playing
                if (!playing) latestFft = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Visualizer error, fallback ke simulasi", e)
            releaseVisualizer()
            return null
        }
        return fftProcessor.process(if (playing) latestFft else null, latestFftRate)
    }

    @Synchronized
    private fun releaseVisualizer() {
        try {
            hardwareVisualizer?.let {
                it.enabled = false
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing visualizer", e)
        } finally {
            hardwareVisualizer = null
            visualizerActive = false
            latestFft = null
        }
    }
}
