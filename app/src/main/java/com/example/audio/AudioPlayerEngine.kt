package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.util.Log
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

    private val visualizerSimulator = VisualizerSimulator()

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

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _visualizerAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val visualizerAmplitudes: StateFlow<List<Float>> = _visualizerAmplitudes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var tickerJob: Job? = null
    private var onTrackCompletedListener: (() -> Unit)? = null

    init {
        startTicker()
    }

    fun setOnTrackCompletedListener(listener: () -> Unit) {
        onTrackCompletedListener = listener
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

                    // Update visualizer values
                    val eqBands = _equalizerState.value.bands.map { it.levelMillibels }
                    _visualizerAmplitudes.value = visualizerSimulator.updateAmplitudes(
                        isPlaying = _isPlaying.value,
                        positionMs = _currentPositionMs.value,
                        bassBoost = _equalizerState.value.bassBoostStrength,
                        equalizerBands = eqBands
                    )
                } catch (e: Exception) {
                    // Ignore transient playback state exceptions
                }
                delay(80) // 12.5 fps updates for smooth waveform
            }
        }
    }

    fun playTrack(track: AudioTrack) {
        _currentTrack.value = track
        _isBuffering.value = true
        _errorMessage.value = null

        scope.launch(Dispatchers.IO) {
            try {
                releaseMediaPlayer()

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
                        scope.launch(Dispatchers.Main) {
                            _isBuffering.value = false
                            _durationMs.value = if (mp.duration > 0) mp.duration.toLong() else track.durationMs
                            setupAudioEffects(mp.audioSessionId)
                            applyPlaybackSpeedInternal(_playbackSpeed.value)
                            mp.start()
                            _isPlaying.value = true
                        }
                    }

                    setOnCompletionListener {
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
                        scope.launch(Dispatchers.Main) {
                            _isBuffering.value = false
                            _isPlaying.value = false
                            _errorMessage.value = "Playback error ($what, $extra). Coba putar track lain atau cek koneksi."
                        }
                        true
                    }

                    prepareAsync()
                }

                mediaPlayer = player
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating playback", e)
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

    private fun setupAudioEffects(audioSessionId: Int) {
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

        } catch (e: Exception) {
            Log.w(TAG, "Failed to setup hardware audio effects, using simulated equalizer", e)
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
        try {
            hardwareEqualizer?.enabled = enabled
            hardwareBassBoost?.enabled = enabled
            hardwareVirtualizer?.enabled = enabled
        } catch (e: Exception) {
            Log.w(TAG, "Error enabling/disabling audiofx", e)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val updatedBands = _equalizerState.value.bands.map { band ->
            if (band.index == bandIndex) band.copy(levelMillibels = levelMillibels) else band
        }
        _equalizerState.value = _equalizerState.value.copy(
            bands = updatedBands,
            currentPreset = "Custom"
        )

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
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _equalizerState.value = _equalizerState.value.copy(bassBoostStrength = clamped)
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
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio effects", e)
        } finally {
            hardwareEqualizer = null
            hardwareBassBoost = null
            hardwareVirtualizer = null
            hardwareReverb = null
        }
    }

    private fun releaseMediaPlayer() {
        try {
            releaseAudioEffects()
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun release() {
        tickerJob?.cancel()
        releaseMediaPlayer()
    }
}
