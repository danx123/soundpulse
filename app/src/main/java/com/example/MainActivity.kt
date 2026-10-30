package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AboutDialog
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CurrentQueueSheet
import com.example.ui.components.FolderLoadedPlaylistDialog
import com.example.ui.components.LyricsScreen
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NowPlayingScreen
import com.example.ui.components.TrackDetailsDialog
import com.example.util.configureAppCheck
import com.example.ui.screens.AiHubScreen
import com.example.ui.screens.CloudScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailSheet
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.StreamingScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AudioPlayerViewModel
import com.example.ui.viewmodel.MainTab
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SoundPulseMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPulseApp(
    viewModel: AudioPlayerViewModel = viewModel()
) {
    SoundPulseMainScreen(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPulseMainScreen(
    viewModel: AudioPlayerViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val equalizerState by viewModel.equalizerState.collectAsState()
    val visualizerAmplitudes by viewModel.visualizerAmplitudes.collectAsState()
    val sleepTimerSecondsLeft by viewModel.sleepTimerSecondsLeft.collectAsState()
    val selectedTrackForDetails by viewModel.selectedTrackForDetails.collectAsState()
    val selectedPlaylistDetail by viewModel.selectedPlaylistDetail.collectAsState()
    val trackToAddToPlaylist by viewModel.trackToAddToPlaylist.collectAsState()
    val isQueueVisible by viewModel.isQueueVisible.collectAsState()
    val playQueue by viewModel.playQueue.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val cloudTracks by viewModel.cloudTracks.collectAsState()
    val cloudPresets by viewModel.cloudPresets.collectAsState()
    val streamingStations by viewModel.streamingStations.collectAsState()
    val playlistsWithTracks by viewModel.playlistsWithTracks.collectAsState()
    val smartPlaylists by viewModel.smartPlaylists.collectAsState()
    val aiRecommendations by viewModel.aiRecommendations.collectAsState()
    val mostPlayedStats by viewModel.mostPlayedStats.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val formatFilter by viewModel.formatFilter.collectAsState()
    val folderLoadedState by viewModel.folderLoadedState.collectAsState()
    val isLyricsVisible by viewModel.isLyricsVisible.collectAsState()
    val lyricsState by viewModel.lyricsState.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val isAiMixLoading by viewModel.isAiMixLoading.collectAsState()
    val radioState by viewModel.radioState.collectAsState()
    val radioCountries by viewModel.radioCountries.collectAsState()
    val selectedRadioCountry by viewModel.selectedRadioCountry.collectAsState()
    val radioQuery by viewModel.radioQuery.collectAsState()

    var showAboutDialog by remember { mutableStateOf(false) }

    // Izin RECORD_AUDIO dibutuhkan Visualizer agar bar mengikuti beat (audio tidak direkam)
    val appContext = LocalContext.current
    var showVisualizerPermissionDialog by remember { mutableStateOf(false) }
    var visualizerPermissionAsked by rememberSaveable { mutableStateOf(false) }
    val recordAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.engine.onVisualizerPermissionGranted()
    }
    LaunchedEffect(currentTrack?.id) {
        if (currentTrack != null && !visualizerPermissionAsked &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            visualizerPermissionAsked = true
            showVisualizerPermissionDialog = true
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.engine.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isPlayerExpanded && selectedPlaylistDetail == null) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        Brush.linearGradient(listOf(NeonCyan, NeonPurple)),
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = DarkBg,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = TextPrimary
                                )
                                Text(
                                    text = stringResource(R.string.app_subtitle),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.testTag("about_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = stringResource(R.string.about),
                                tint = NeonCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = TextPrimary
                    ),
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
                )
            }
        },
        bottomBar = {
            if (!isPlayerExpanded && selectedPlaylistDetail == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Persistent Mini Player above bottom navigation
                    if (currentTrack != null) {
                        MiniPlayer(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            isBuffering = isBuffering,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            visualizerAmplitudes = visualizerAmplitudes,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNextTrack() },
                            onClick = { viewModel.setPlayerExpanded(true) }
                        )
                    }

                    // Bottom Navigation Bar with 6 streamlined tabs
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextSecondary,
                        tonalElevation = 8.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.LIBRARY,
                            onClick = { viewModel.setTab(MainTab.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = stringResource(R.string.tab_library)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_library), fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBg,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_library")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.PLAYLISTS,
                            onClick = { viewModel.setTab(MainTab.PLAYLISTS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = stringResource(R.string.tab_playlists)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_playlists), fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBg,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_playlists")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.AI_HUB,
                            onClick = { viewModel.setTab(MainTab.AI_HUB) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = stringResource(R.string.tab_ai_hub)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_ai_hub), fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBg,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_ai_hub")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.STREAMING,
                            onClick = { viewModel.setTab(MainTab.STREAMING) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Radio,
                                    contentDescription = stringResource(R.string.tab_streaming)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_streaming), fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBg,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_streaming")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.CLOUD,
                            onClick = { viewModel.setTab(MainTab.CLOUD) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Cloud,
                                    contentDescription = stringResource(R.string.tab_cloud)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_cloud), fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBg,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_cloud")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.EQUALIZER,
                            onClick = { viewModel.setTab(MainTab.EQUALIZER) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = stringResource(R.string.tab_equalizer)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_equalizer), fontSize = 10.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DarkBg,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_item_equalizer")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.LIBRARY -> LibraryScreen(
                    tracks = filteredTracks,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    searchQuery = searchQuery,
                    selectedFormat = formatFilter,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onFormatFilterSelect = { viewModel.setFormatFilter(it) },
                    onTrackSelect = { track -> viewModel.playTrack(track, filteredTracks) },
                    onImportLocalFiles = { uris -> viewModel.importLocalFiles(uris) },
                    onScanFolder = { uri -> viewModel.scanFolderTree(uri) },
                    onPlayFromFolder = { uri -> viewModel.playFromFolder(uri) },
                    onShowTrackDetails = { track -> viewModel.showTrackDetails(track) },
                    onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                    onAddToPlaylist = { track -> viewModel.openAddToPlaylistDialog(track) },
                    onScanMediaStore = { viewModel.scanMediaStoreAudio() },
                    onAboutClick = { showAboutDialog = true }
                )

                MainTab.PLAYLISTS -> PlaylistScreen(
                    playlists = playlistsWithTracks,
                    onSelectPlaylist = { p -> viewModel.openPlaylistDetail(p) },
                    onPlayPlaylist = { p -> viewModel.playPlaylist(p) },
                    onCreatePlaylist = { title, desc, type ->
                        viewModel.createPlaylist(title, desc, type)
                    },
                    onDeletePlaylist = { id -> viewModel.deletePlaylist(id) }
                )

                MainTab.AI_HUB -> AiHubScreen(
                    smartPlaylists = smartPlaylists,
                    recommendations = aiRecommendations,
                    mostPlayedStats = mostPlayedStats,
                    allTracks = filteredTracks,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onPlaySmartPlaylist = { p -> viewModel.playSmartPlaylist(p) },
                    onPlayTrack = { track -> viewModel.playTrack(track) },
                    onGenerateCustomAiMix = { prompt -> viewModel.generateCustomAiMix(prompt) },
                    authState = authState,
                    isAiMixLoading = isAiMixLoading,
                    onSignInWithGoogle = { viewModel.signInWithGoogle(appContext) },
                    onSignOut = { viewModel.signOutGoogle() },
                    onAboutClick = { showAboutDialog = true }
                )

                MainTab.STREAMING -> StreamingScreen(
                    stations = streamingStations,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onStationSelect = { station -> viewModel.playStation(station) },
                    onAddCustomStation = { name, genre, url, desc ->
                        viewModel.addCustomStation(name, genre, url, desc)
                    },
                    radioState = radioState,
                    radioCountries = radioCountries,
                    selectedRadioCountry = selectedRadioCountry,
                    radioQuery = radioQuery,
                    onRadioEnter = { viewModel.ensureRadioLoaded() },
                    onSelectRadioCountry = { country -> viewModel.selectRadioCountry(country) },
                    onRadioQueryChange = { q -> viewModel.setRadioQuery(q) },
                    onLoadMoreRadio = { viewModel.loadMoreRadio() },
                    onRetryRadio = { viewModel.retryRadio() }
                )

                MainTab.CLOUD -> CloudScreen(
                    cloudTracks = cloudTracks,
                    presets = cloudPresets,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    onTrackSelect = { track -> viewModel.playTrack(track, cloudTracks) },
                    onAddCloudTrack = { title, artist, url, provider, format ->
                        viewModel.addCloudTrack(title, artist, url, provider, format)
                    },
                    onShowTrackDetails = { track -> viewModel.showTrackDetails(track) },
                    onAddToPlaylist = { track -> viewModel.openAddToPlaylistDialog(track) }
                )

                MainTab.EQUALIZER -> EqualizerScreen(
                    state = equalizerState,
                    onToggleEnable = { viewModel.setEqualizerEnabled(it) },
                    onBandLevelChange = { band, lvl -> viewModel.setBandLevel(band, lvl) },
                    onBassBoostChange = { strength -> viewModel.setBassBoost(strength) },
                    onVirtualizerChange = { strength -> viewModel.setVirtualizer(strength) },
                    onApplyPreset = { preset -> viewModel.applyPreset(preset) },
                    onSetReverb = { reverb -> viewModel.setReverb(reverb) }
                )
            }
        }
    }

    // Playlist Detail Sheet
    AnimatedVisibility(
        visible = selectedPlaylistDetail != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        selectedPlaylistDetail?.let { playlist ->
            PlaylistDetailSheet(
                playlistWithTracks = playlist,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onBack = { viewModel.openPlaylistDetail(null) },
                onPlayTrackAtIndex = { idx -> viewModel.playPlaylist(playlist, idx) },
                onPlayAll = { viewModel.playPlaylist(playlist) },
                onShuffleAll = { viewModel.shufflePlaylist(playlist) },
                onRemoveTrack = { trackId -> viewModel.removeTrackFromPlaylist(trackId, playlist.playlist.id) },
                onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) },
                onShowTrackDetails = { track -> viewModel.showTrackDetails(track) }
            )
        }
    }

    // Fullscreen Animated Expandable Now Playing Screen with Cinematic Lighting & Dynamic Aura
    AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        NowPlayingScreen(
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            playbackSpeed = playbackSpeed,
            shuffleMode = shuffleMode,
            repeatMode = repeatMode,
            visualizerAmplitudes = visualizerAmplitudes,
            sleepTimerSecondsLeft = sleepTimerSecondsLeft,
            onCollapse = { viewModel.setPlayerExpanded(false) },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onNext = { viewModel.playNextTrack() },
            onPrevious = { viewModel.playPreviousTrack() },
            onSeekTo = { pos -> viewModel.seekTo(pos) },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onSetPlaybackSpeed = { spd -> viewModel.setPlaybackSpeed(spd) },
            onSetSleepTimer = { mins -> viewModel.setSleepTimer(mins) },
            onOpenEqualizer = {
                viewModel.setPlayerExpanded(false)
                viewModel.setTab(MainTab.EQUALIZER)
            },
            onShowTrackDetails = { track -> viewModel.showTrackDetails(track) },
            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
            onOpenLyrics = { viewModel.openLyrics() },
            onOpenQueue = { viewModel.setQueueVisible(true) }
        )
    }

    // Lirik layar penuh (di atas Now Playing)
    AnimatedVisibility(
        visible = isLyricsVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        LyricsScreen(
            track = currentTrack,
            state = lyricsState,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onClose = { viewModel.closeLyrics() },
            onSeekTo = { pos -> viewModel.seekTo(pos) },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onRetry = { viewModel.retryLyrics() }
        )
    }

    // Add To Playlist Dialog
    trackToAddToPlaylist?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = playlistsWithTracks,
            onAddToPlaylist = { playlistId -> viewModel.addTrackToPlaylist(playlistId, track) },
            onCreateAndAdd = { title, type ->
                viewModel.viewModelScope.launch {
                    val newId = viewModel.playlistRepository.createPlaylist(title, "", type)
                    viewModel.addTrackToPlaylist(newId, track)
                }
            },
            onDismiss = { viewModel.openAddToPlaylistDialog(null) }
        )
    }

    // Dialog "Tambahkan folder ke playlist" setelah daftar lagu folder selesai dimuat
    folderLoadedState?.let { state ->
        FolderLoadedPlaylistDialog(
            folderName = state.folderName,
            tracks = state.tracks,
            existingPlaylists = playlistsWithTracks,
            onCreateNewPlaylist = { name -> viewModel.addFolderToNewPlaylist(name, state.tracks) },
            onAddToExistingPlaylist = { id, name ->
                viewModel.addFolderToExistingPlaylist(id, name, state.tracks)
            },
            onDismiss = { viewModel.dismissFolderLoadedDialog() }
        )
    }

    // Track Details Technical Modal
    selectedTrackForDetails?.let { track ->
        TrackDetailsDialog(
            track = track,
            onDismiss = { viewModel.showTrackDetails(null) }
        )
    }

    // Modal Sheet Antrean / Daftar Putar dengan dukungan drag reorder
    if (isQueueVisible) {
        CurrentQueueSheet(
            queue = playQueue,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            onDismiss = { viewModel.setQueueVisible(false) },
            onPlayTrackAtIndex = { idx -> viewModel.playTrackFromQueue(idx) },
            onRemoveFromQueue = { idx -> viewModel.removeFromQueue(idx) },
            onReorderQueue = { from, to -> viewModel.reorderQueue(from, to) },
            onShuffleQueue = { viewModel.shuffleQueue() },
            onClearQueue = { viewModel.clearQueue() }
        )
    }

    // Penjelasan izin untuk visualizer beat
    if (showVisualizerPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showVisualizerPermissionDialog = false },
            title = { Text("Aktifkan Visualizer Beat", color = TextPrimary) },
            text = {
                Text(
                    text = "Agar visualizer bergerak mengikuti beat lagu, Android mewajibkan izin Mikrofon. " +
                        "SoundPulse hanya membaca spektrum audio yang sedang diputar dan tidak merekam suara apa pun. " +
                        "Tanpa izin ini, visualizer memakai animasi simulasi.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showVisualizerPermissionDialog = false
                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Izinkan", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showVisualizerPermissionDialog = false }) {
                    Text("Nanti", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // About SoundPulse & Macan Angkasa Modal
    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}
