package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerEngine
import com.example.audio.RepeatMode
import com.example.data.local.SoundPulseDatabase
import com.example.data.local.entity.PlaylistEntity
import com.example.data.local.entity.PlaylistType
import com.example.data.local.entity.PlaylistWithTracks
import com.example.data.local.entity.TrackStatEntity
import com.example.data.model.AiMusicRecommendation
import com.example.data.model.AudioFormat
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.data.model.SmartPlaylist
import com.example.data.model.StreamingStation
import com.example.data.repository.AiMusicRecommender
import com.example.data.repository.AudioRepository
import com.example.data.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    LIBRARY,
    PLAYLISTS,
    AI_HUB,
    STREAMING,
    CLOUD,
    EQUALIZER
}

class AudioPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SoundPulseDatabase.getDatabase(application.applicationContext, viewModelScope)
    private val playlistDao = database.playlistDao()
    private val trackStatDao = database.trackStatDao()
    val playlistRepository = PlaylistRepository(playlistDao)
    private val aiRecommender = AiMusicRecommender()

    private val repository = AudioRepository(application.applicationContext)
    val engine = AudioPlayerEngine(application.applicationContext, viewModelScope)

    // UI Navigation State
    private val _currentTab = MutableStateFlow(MainTab.LIBRARY)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _selectedTrackForDetails = MutableStateFlow<AudioTrack?>(null)
    val selectedTrackForDetails: StateFlow<AudioTrack?> = _selectedTrackForDetails.asStateFlow()

    private val _selectedPlaylistDetail = MutableStateFlow<PlaylistWithTracks?>(null)
    val selectedPlaylistDetail: StateFlow<PlaylistWithTracks?> = _selectedPlaylistDetail.asStateFlow()

    private val _trackToAddToPlaylist = MutableStateFlow<AudioTrack?>(null)
    val trackToAddToPlaylist: StateFlow<AudioTrack?> = _trackToAddToPlaylist.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Library filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _formatFilter = MutableStateFlow<AudioFormat?>(null)
    val formatFilter: StateFlow<AudioFormat?> = _formatFilter.asStateFlow()

    // Queue management
    private val _playQueue = MutableStateFlow<List<AudioTrack>>(emptyList())
    val playQueue: StateFlow<List<AudioTrack>> = _playQueue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Sleep Timer
    private val _sleepTimerSecondsLeft = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsLeft: StateFlow<Int?> = _sleepTimerSecondsLeft.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Exposed Flows from Engine
    val currentTrack: StateFlow<AudioTrack?> = engine.currentTrack
    val isPlaying: StateFlow<Boolean> = engine.isPlaying
    val isBuffering: StateFlow<Boolean> = engine.isBuffering
    val currentPositionMs: StateFlow<Long> = engine.currentPositionMs
    val durationMs: StateFlow<Long> = engine.durationMs
    val playbackSpeed: StateFlow<Float> = engine.playbackSpeed
    val shuffleMode: StateFlow<Boolean> = engine.shuffleMode
    val repeatMode: StateFlow<RepeatMode> = engine.repeatMode
    val equalizerState: StateFlow<EqualizerState> = engine.equalizerState
    val visualizerAmplitudes: StateFlow<List<Float>> = engine.visualizerAmplitudes
    val errorMessage: StateFlow<String?> = engine.errorMessage

    // Repository Flows
    val allTracks: StateFlow<List<AudioTrack>> = repository.tracks
    val cloudTracks: StateFlow<List<AudioTrack>> = repository.cloudTracks
    val streamingStations: StateFlow<List<StreamingStation>> = repository.streamingStations
    val cloudPresets = repository.cloudPresets

    // Room Playlists Flow
    val playlistsWithTracks: StateFlow<List<PlaylistWithTracks>> =
        playlistRepository.allPlaylistsWithTracks
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Room Track Stats (Listening Habits & Most Played)
    val mostPlayedStats: StateFlow<List<TrackStatEntity>> =
        trackStatDao.getMostPlayed()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered tracks
    val filteredTracks: StateFlow<List<AudioTrack>> = combine(
        allTracks,
        _searchQuery,
        _formatFilter
    ) { tracks, query, format ->
        tracks.filter { track ->
            val matchesQuery = query.isBlank() ||
                    track.title.contains(query, ignoreCase = true) ||
                    track.artist.contains(query, ignoreCase = true) ||
                    track.album.contains(query, ignoreCase = true)
            val matchesFormat = format == null || track.format == format
            matchesQuery && matchesFormat
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // AI Smart Playlists (Most Played, Chill Focus, Hi-Res Audiophile, Synthwave)
    val smartPlaylists: StateFlow<List<SmartPlaylist>> = combine(
        allTracks,
        mostPlayedStats
    ) { tracks, stats ->
        aiRecommender.generateSmartPlaylists(tracks, stats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Music Recommendations
    val aiRecommendations: StateFlow<List<AiMusicRecommendation>> = combine(
        allTracks,
        mostPlayedStats
    ) { tracks, stats ->
        aiRecommender.generateRecommendations(tracks, stats)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        engine.setOnTrackCompletedListener {
            playNextTrack()
        }

        // Ensure database has default playlists and initial stats if newly created
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existing = playlistDao.getAllPlaylists().first()
                if (existing.isEmpty()) {
                    SoundPulseDatabase.populateInitialPlaylists(playlistDao)
                    SoundPulseDatabase.populateInitialStats(trackStatDao)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFormatFilter(format: AudioFormat?) {
        _formatFilter.value = format
    }

    fun showTrackDetails(track: AudioTrack?) {
        _selectedTrackForDetails.value = track
    }

    fun openPlaylistDetail(playlist: PlaylistWithTracks?) {
        _selectedPlaylistDetail.value = playlist
    }

    fun openAddToPlaylistDialog(track: AudioTrack?) {
        _trackToAddToPlaylist.value = track
    }

    fun playTrack(track: AudioTrack, queue: List<AudioTrack>? = null) {
        val targetQueue = queue ?: _playQueue.value.ifEmpty { listOf(track) }
        _playQueue.value = targetQueue
        val index = targetQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        _currentIndex.value = index
        engine.playTrack(track)

        // Record play stat in Room database
        recordTrackPlay(track)
    }

    private fun recordTrackPlay(track: AudioTrack) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updated = trackStatDao.incrementPlayCount(track.id)
                if (updated == 0) {
                    trackStatDao.insertOrUpdate(
                        TrackStatEntity(
                            trackId = track.id,
                            title = track.title,
                            artist = track.artist,
                            playCount = 1,
                            lastPlayedAt = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                // ignore transient db errors
            }
        }
    }

    fun playStation(station: StreamingStation) {
        val track = AudioTrack(
            id = "station_${station.id}",
            title = station.name,
            artist = station.genre,
            album = "Radio Pihak Ketiga • ${station.country}",
            durationMs = 0L,
            uri = station.streamUrl,
            sourceType = AudioSourceType.STREAMING_RADIO,
            format = AudioFormat.STREAM,
            bitrateKbps = 128,
            sampleRateHz = 44100,
            fileSizeFormatted = "Live Broadcast",
            artworkUrl = station.artworkUrl
        )
        _playQueue.value = listOf(track)
        _currentIndex.value = 0
        engine.playTrack(track)
        recordTrackPlay(track)
    }

    fun playPlaylist(playlistWithTracks: PlaylistWithTracks, startIndex: Int = 0) {
        val tracks = playlistWithTracks.tracks.map { it.toAudioTrack() }
        if (tracks.isNotEmpty()) {
            val safeIndex = startIndex.coerceIn(0, tracks.size - 1)
            playTrack(tracks[safeIndex], tracks)
            _userMessage.value = "Memutar playlist: ${playlistWithTracks.playlist.title}"
        } else {
            _userMessage.value = "Playlist kosong. Tambahkan lagu terlebih dahulu."
        }
    }

    fun shufflePlaylist(playlistWithTracks: PlaylistWithTracks) {
        val tracks = playlistWithTracks.tracks.map { it.toAudioTrack() }.shuffled()
        if (tracks.isNotEmpty()) {
            if (!engine.shuffleMode.value) {
                engine.toggleShuffle()
            }
            playTrack(tracks.first(), tracks)
            _userMessage.value = "Memutar acak: ${playlistWithTracks.playlist.title}"
        }
    }

    fun playSmartPlaylist(smartPlaylist: SmartPlaylist) {
        if (smartPlaylist.tracks.isNotEmpty()) {
            playTrack(smartPlaylist.tracks.first(), smartPlaylist.tracks)
            _userMessage.value = "Memutar: ${smartPlaylist.title}"
        }
    }

    fun generateCustomAiMix(prompt: String) {
        val mix = aiRecommender.createCustomAiPlaylist(prompt, allTracks.value)
        playSmartPlaylist(mix)
    }

    fun createPlaylist(title: String, description: String, type: PlaylistType) {
        viewModelScope.launch {
            val id = playlistRepository.createPlaylist(title, description, type)
            _userMessage.value = "Playlist '$title' berhasil dibuat"
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            if (_selectedPlaylistDetail.value?.playlist?.id == playlistId) {
                _selectedPlaylistDetail.value = null
            }
            _userMessage.value = "Playlist telah dihapus"
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: AudioTrack) {
        viewModelScope.launch {
            val success = playlistRepository.addTrackToPlaylist(playlistId, track)
            if (success) {
                _userMessage.value = "Ditambahkan ke playlist"
                val updated = playlistRepository.getPlaylist(playlistId).first()
                if (_selectedPlaylistDetail.value?.playlist?.id == playlistId) {
                    _selectedPlaylistDetail.value = updated
                }
            } else {
                _userMessage.value = "Lagu sudah ada di playlist ini"
            }
            _trackToAddToPlaylist.value = null
        }
    }

    fun removeTrackFromPlaylist(playlistTrackId: Long, playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.removeTrack(playlistTrackId, playlistId)
            val updated = playlistRepository.getPlaylist(playlistId).first()
            _selectedPlaylistDetail.value = updated
            _userMessage.value = "Lagu dihapus dari playlist"
        }
    }

    fun playNextTrack() {
        val queue = _playQueue.value
        if (queue.isEmpty()) return

        var nextIndex = _currentIndex.value + 1
        if (engine.shuffleMode.value && queue.size > 1) {
            nextIndex = queue.indices.filter { it != _currentIndex.value }.random()
        } else if (nextIndex >= queue.size) {
            if (engine.repeatMode.value == RepeatMode.ALL) {
                nextIndex = 0
            } else {
                return
            }
        }
        _currentIndex.value = nextIndex
        val nextTrack = queue[nextIndex]
        engine.playTrack(nextTrack)
        recordTrackPlay(nextTrack)
    }

    fun playPreviousTrack() {
        val queue = _playQueue.value
        if (queue.isEmpty()) return

        if (engine.currentPositionMs.value > 4000L) {
            engine.seekTo(0L)
            return
        }

        var prevIndex = _currentIndex.value - 1
        if (prevIndex < 0) {
            prevIndex = if (queue.size > 1) queue.size - 1 else 0
        }
        _currentIndex.value = prevIndex
        val prevTrack = queue[prevIndex]
        engine.playTrack(prevTrack)
        recordTrackPlay(prevTrack)
    }

    fun togglePlayPause() = engine.togglePlayPause()

    fun seekTo(positionMs: Long) = engine.seekTo(positionMs)

    fun toggleShuffle() = engine.toggleShuffle()

    fun cycleRepeatMode() = engine.cycleRepeatMode()

    fun setPlaybackSpeed(speed: Float) = engine.setPlaybackSpeed(speed)

    // Equalizer
    fun setEqualizerEnabled(enabled: Boolean) = engine.setEqualizerEnabled(enabled)

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) = engine.setBandLevel(bandIndex, levelMillibels)

    fun setBassBoost(strength: Int) = engine.setBassBoostStrength(strength)

    fun setVirtualizer(strength: Int) = engine.setVirtualizerStrength(strength)

    fun applyPreset(preset: EqualizerPreset) = engine.applyPreset(preset)

    fun setReverb(reverb: String) = engine.setReverb(reverb)

    // Sleep timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSecondsLeft.value = null
            _userMessage.value = "Sleep timer dinonaktifkan"
            return
        }

        _sleepTimerSecondsLeft.value = minutes * 60
        _userMessage.value = "Sleep timer aktif: $minutes menit"

        sleepTimerJob = viewModelScope.launch {
            while ((_sleepTimerSecondsLeft.value ?: 0) > 0) {
                delay(1000)
                _sleepTimerSecondsLeft.value = (_sleepTimerSecondsLeft.value ?: 1) - 1
            }
            engine.pause()
            _sleepTimerSecondsLeft.value = null
            _userMessage.value = "Sleep timer berakhir: pemutaran dihentikan"
        }
    }

    // Local & Cloud Imports
    fun importLocalFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val count = repository.importLocalUris(uris)
            _userMessage.value = "Berhasil mengimpor $count file audio lokal"
        }
    }

    fun scanFolderTree(treeUri: Uri) {
        viewModelScope.launch {
            val count = repository.scanFolderTree(treeUri)
            _userMessage.value = "Selesai memindai folder: ditemukan $count file audio"
        }
    }

    fun scanMediaStoreAudio() {
        viewModelScope.launch {
            val count = repository.scanMediaStoreAudio()
            _userMessage.value = if (count > 0) {
                "Pindai perangkat selesai: $count lagu ditambahkan ke library"
            } else {
                "Pindai selesai: Semua lagu perangkat sudah ada di library"
            }
        }
    }

    fun addCloudTrack(title: String, artist: String, url: String, provider: String, format: AudioFormat) {
        val track = repository.addCloudTrack(title, artist, url, provider, format)
        _userMessage.value = "Track cloud ditambahkan: ${track.title}"
    }

    fun addCustomStation(name: String, genre: String, url: String, desc: String) {
        repository.addCustomStation(name, genre, url, desc)
        _userMessage.value = "Stasiun radio online ditambahkan!"
    }

    fun toggleFavorite(trackId: String) {
        repository.toggleFavorite(trackId)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
        engine.release()
    }
}
