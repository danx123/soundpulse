package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.RepeatMode
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.HighResGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanVariant
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.rememberDynamicAura

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    shuffleMode: Boolean,
    repeatMode: RepeatMode,
    visualizerAmplitudes: List<Float>,
    sleepTimerSecondsLeft: Int?,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    onShowTrackDetails: (AudioTrack) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler {
        onCollapse()
    }

    if (currentTrack == null) return

    val aura = rememberDynamicAura(currentTrack)

    var isUserSeeking by remember { mutableStateOf(false) }
    var userSeekPosition by remember { mutableFloatStateOf(0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Restart
        ),
        label = "VinylRotation"
    )

    // Breathing glow animation for cinematic lightning halo
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = AnimRepeatMode.Reverse
        ),
        label = "HaloPulse"
    )

    val displayedProgress = if (isUserSeeking) {
        userSeekPosition
    } else if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("now_playing_screen"),
        color = DarkBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // === 1. CINEMATIC LIGHTING (Artwork Blur in Player Area) ===
            TrackArtwork(
                track = currentTrack,
                priority = true,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .alpha(0.38f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_default_album_cover),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(50.dp)
                        .alpha(0.38f)
                )
            }

            // Ambient Dynamic Aura radial light leak
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                aura.dominant.copy(alpha = 0.40f),
                                aura.secondary.copy(alpha = 0.22f),
                                DarkBg.copy(alpha = 0.94f)
                            ),
                            center = Offset(450f, 550f),
                            radius = 1100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("now_playing_collapse_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.collapse_player),
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = aura.dominant,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (currentTrack.sourceType) {
                                    AudioSourceType.LOCAL_FILE -> "LOCAL AUDIO"
                                    AudioSourceType.LOCAL_FOLDER -> "LOCAL FOLDER"
                                    AudioSourceType.CLOUD_STORAGE -> "CLOUD (${currentTrack.cloudProvider ?: "Storage"})"
                                    AudioSourceType.STREAMING_RADIO -> "INTERNET LIVE RADIO"
                                    AudioSourceType.BUILTIN_DEMO -> "STUDIO HI-RES"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                color = aura.dominant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = currentTrack.album,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onOpenQueue,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("now_playing_queue_btn_top")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Daftar Putar",
                                tint = aura.dominant
                            )
                        }

                        IconButton(
                            onClick = { showTimerDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = stringResource(R.string.set_sleep_timer),
                                tint = if (sleepTimerSecondsLeft != null) aura.dominant else TextSecondary
                            )
                        }

                        IconButton(
                            onClick = { onShowTrackDetails(currentTrack) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.track_info),
                                tint = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // === CINEMATIC LIGHTING HALO & VINYL DISC ===
                Box(
                    modifier = Modifier.size(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing ambient illumination halo behind vinyl
                    Box(
                        modifier = Modifier
                            .size(270.dp * (if (isPlaying) haloPulse else 1.0f))
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        aura.dominant.copy(alpha = 0.50f),
                                        aura.secondary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Outer Vinyl Disc with Dynamic Aura Sweep Border
                    Box(
                        modifier = Modifier
                            .size(255.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(aura.dominant, aura.secondary, aura.accent, aura.dominant)
                                ),
                                shape = CircleShape
                            )
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        // Vinyl Grooves
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.92f)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF334155), CircleShape)
                        )

                        // Center rotating artwork
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.65f)
                                .clip(CircleShape)
                                .rotate(if (isPlaying) rotationAngle else 0f)
                                .border(2.dp, aura.dominant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            TrackArtwork(
                                track = currentTrack,
                                priority = true,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_default_album_cover),
                                    contentDescription = "Album Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Center spindle hole
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(DarkBg)
                                .border(1.5.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Track Title & Artist
                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentTrack.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Technical Audio Spec Badges & Dynamic Aura Active Badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatBadge(
                        formatName = currentTrack.format.displayName,
                        isLossless = currentTrack.format.isLossless
                    )

                    // Dynamic Aura Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(aura.dominant.copy(alpha = 0.15f))
                            .border(0.5.dp, aura.dominant.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AURA",
                            color = aura.dominant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (currentTrack.bitrateKbps != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = "${currentTrack.bitrateKbps} kbps",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (currentTrack.sampleRateHz != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = "${currentTrack.sampleRateHz / 1000} kHz",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // === DYNAMIC AURA SPECTRUM VISUALIZER ===
                VisualizerView(
                    amplitudes = visualizerAmplitudes,
                    height = 56.dp,
                    customGradient = listOf(aura.dominant, aura.secondary, aura.accent),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // === DYNAMIC AURA SEEKBAR SLIDER ===
                Slider(
                    value = displayedProgress,
                    onValueChange = { newValue ->
                        isUserSeeking = true
                        userSeekPosition = newValue
                    },
                    onValueChangeFinished = {
                        isUserSeeking = false
                        if (durationMs > 0) {
                            val targetMs = (userSeekPosition * durationMs).toLong()
                            onSeekTo(targetMs)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_seek_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = aura.dominant,
                        activeTrackColor = aura.dominant,
                        inactiveTrackColor = DarkSurfaceVariant
                    )
                )

                // Timestamps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentMs = if (isUserSeeking && durationMs > 0) {
                        (userSeekPosition * durationMs).toLong()
                    } else currentPositionMs

                    Text(
                        text = formatTime(currentMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Text(
                        text = if (durationMs > 0) formatTime(durationMs) else if (currentTrack.sourceType == AudioSourceType.STREAMING_RADIO) "LIVE" else "--:--",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (durationMs <= 0 && currentTrack.sourceType == AudioSourceType.STREAMING_RADIO) GlowAmber else TextMuted,
                        fontWeight = if (durationMs <= 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Playback Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = stringResource(R.string.shuffle),
                            tint = if (shuffleMode) aura.dominant else TextMuted
                        )
                    }

                    // Previous Track
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("now_playing_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // === DYNAMIC AURA PLAY / PAUSE BUTTON ===
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(aura.dominant, aura.secondary)
                                )
                            )
                            .clickable { onTogglePlayPause() }
                            .testTag("now_playing_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = DarkBg,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkBg,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    // Next Track
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("now_playing_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            tint = TextPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Repeat Mode
                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_repeat_btn")
                    ) {
                        Icon(
                            imageVector = when (repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != RepeatMode.OFF) aura.dominant else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Action Tools Row (Speed, Equalizer, Favorite)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback speed pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showSpeedDialog = true },
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = stringResource(R.string.speed),
                                tint = aura.dominant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${playbackSpeed}x",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }

                    // Equalizer launcher button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenEqualizer() },
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, aura.secondary.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = stringResource(R.string.tab_equalizer),
                                tint = aura.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.tab_equalizer),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }

                    // Lyrics launcher button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenLyrics() }
                            .testTag("now_playing_lyrics_btn"),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, aura.dominant.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "Lirik",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    // Playlist / Queue button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onOpenQueue() }
                            .testTag("now_playing_queue_btn"),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, aura.dominant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Daftar Putar",
                                tint = aura.dominant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Antrean",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }

                    // Favorite button
                    IconButton(
                        onClick = { onToggleFavorite(currentTrack.id) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (currentTrack.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (currentTrack.isFavorite) NeonPink else TextMuted
                        )
                    }
                }

                if (sleepTimerSecondsLeft != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val mins = sleepTimerSecondsLeft / 60
                    val secs = sleepTimerSecondsLeft % 60
                    Text(
                        text = stringResource(R.string.sleep_timer_active, mins, secs),
                        style = MaterialTheme.typography.labelSmall,
                        color = aura.dominant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.about_footer_brand),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = TextMuted.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Speed Selector Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text(stringResource(R.string.speed), color = TextPrimary) },
            text = {
                Column {
                    speeds.forEach { spd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetPlaybackSpeed(spd)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${spd}x ${if (spd == 1.0f) "(${stringResource(R.string.normal_speed)})" else ""}",
                                color = if (playbackSpeed == spd) aura.dominant else TextPrimary,
                                fontWeight = if (playbackSpeed == spd) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playbackSpeed == spd) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Selected",
                                    tint = aura.dominant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text(stringResource(R.string.close), color = aura.dominant)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Sleep Timer Dialog
    if (showTimerDialog) {
        AlertDialog(
            onDismissRequest = { showTimerDialog = false },
            title = { Text(stringResource(R.string.set_sleep_timer), color = TextPrimary) },
            text = {
                Column {
                    val timerOptions = listOf(15, 30, 45, 60)
                    timerOptions.forEach { minutes ->
                        TextButton(
                            onClick = {
                                onSetSleepTimer(minutes)
                                showTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.minutes_format, minutes), color = aura.dominant, fontSize = 16.sp)
                        }
                    }
                    if (sleepTimerSecondsLeft != null) {
                        TextButton(
                            onClick = {
                                onSetSleepTimer(0)
                                showTimerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.turn_off_sleep_timer), color = NeonPink, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimerDialog = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
