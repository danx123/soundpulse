package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioTrack
import com.example.data.model.LrcParser
import com.example.data.model.LyricLine
import com.example.data.model.LyricsData
import com.example.data.model.LyricsUiState
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.rememberDynamicAura
import kotlin.math.abs

/** Lirik layar penuh: berinterval waktu (auto-scroll + tap untuk seek) atau teks biasa. */
@Composable
fun LyricsScreen(
    track: AudioTrack?,
    state: LyricsUiState,
    currentPositionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onClose: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    if (track == null) return

    val aura = rememberDynamicAura(track)
    val isLoading = state is LyricsUiState.Loading
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "spinAngle"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("lyrics_screen"),
        color = DarkBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Latar: artwork blur + cahaya aura
            TrackArtwork(
                track = track,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .alpha(0.30f)
            ) {}

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                aura.dominant.copy(alpha = 0.35f),
                                aura.secondary.copy(alpha = 0.18f),
                                DarkBg.copy(alpha = 0.95f)
                            ),
                            center = Offset(450f, 500f),
                            radius = 1200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("lyrics_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Tutup lirik",
                            tint = TextPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val statusSubtitle = when (state) {
                            is LyricsUiState.Success -> "${track.artist} • ${state.data.source}"
                            is LyricsUiState.Loading -> "${track.artist} • Memuat lirik…"
                            else -> track.artist
                        }
                        Text(
                            text = statusSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onRetry,
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("lyrics_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Muat ulang lirik",
                            tint = if (isLoading) aura.dominant else TextSecondary,
                            modifier = Modifier.rotate(if (isLoading) spinAngle else 0f)
                        )
                    }
                }

                // Konten lirik
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is LyricsUiState.Idle,
                        is LyricsUiState.Loading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = aura.dominant, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Mencari lirik…", color = TextSecondary, fontSize = 14.sp)
                            }
                        }

                        is LyricsUiState.Success -> LyricsContent(
                            data = state.data,
                            positionMs = currentPositionMs,
                            accent = aura.dominant,
                            onSeekTo = onSeekTo
                        )

                        is LyricsUiState.NotFound -> MessageBlock(
                            title = "Lirik tidak ditemukan",
                            message = "Lirik untuk lagu ini belum ditemukan di LRCLIB maupun Musixmatch. Pastikan judul dan artis lagu sudah benar, lalu klik coba lagi.",
                            actionLabel = "Coba Lagi (Re-fetch)",
                            accent = aura.dominant,
                            onAction = onRetry
                        )

                        is LyricsUiState.Unavailable -> MessageBlock(
                            title = "Lirik tidak tersedia",
                            message = state.reason,
                            actionLabel = null,
                            accent = aura.dominant,
                            onAction = {}
                        )

                        is LyricsUiState.Error -> MessageBlock(
                            title = "Gagal memuat lirik",
                            message = state.message,
                            actionLabel = "Coba Lagi",
                            accent = aura.dominant,
                            onAction = onRetry
                        )
                    }
                }

                // Kontrol ringkas
                val progress = if (durationMs > 0) {
                    (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = aura.dominant,
                        trackColor = DarkSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPositionMs), color = TextMuted, fontSize = 11.sp)
                        Text(
                            if (durationMs > 0) formatTime(durationMs) else "--:--",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(aura.dominant)
                            .clickable { onTogglePlayPause() }
                            .testTag("lyrics_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DarkBg,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LyricsContent(
    data: LyricsData,
    positionMs: Long,
    accent: Color,
    onSeekTo: (Long) -> Unit
) {
    when {
        data.isSynced -> SyncedLyrics(
            data = data,
            positionMs = positionMs,
            accent = accent,
            onSeekTo = onSeekTo
        )

        data.plainText != null -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SourceChip(label = "Teks • ${data.source}", accent = accent)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = data.plainText,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        else -> MessageBlock(
            title = "Instrumental",
            message = "Lagu ini tidak memiliki lirik.",
            actionLabel = null,
            accent = accent,
            onAction = {}
        )
    }
}

@Composable
private fun SyncedLyrics(
    data: LyricsData,
    positionMs: Long,
    accent: Color,
    onSeekTo: (Long) -> Unit
) {
    val lines = data.syncedLines
    val listState = rememberLazyListState()
    // Sedikit lookahead supaya baris berganti tepat sebelum vokal masuk
    val activeIndex = remember(lines, positionMs) {
        LrcParser.activeIndex(lines, positionMs + LOOKAHEAD_MS)
    }
    val dragged by listState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && !dragged) {
            val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            listState.animateScrollToItem(activeIndex, -(viewport * 0.30f).toInt())
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 220.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                SourceChip(label = "Lirik Sinkron • ${data.source}", accent = accent)
            }
        }

        itemsIndexed(lines) { index, line ->
            LyricLineItem(
                text = line.text,
                isActive = index == activeIndex,
                distance = if (activeIndex < 0) 2 else abs(index - activeIndex),
                accent = accent,
                onClick = { onSeekTo(line.timeMs) }
            )
        }
    }
}

@Composable
private fun LyricLineItem(
    text: String,
    isActive: Boolean,
    distance: Int,
    accent: Color,
    onClick: () -> Unit
) {
    val targetAlpha = when {
        isActive -> 1f
        distance == 1 -> 0.70f
        distance == 2 -> 0.50f
        else -> 0.35f
    }
    val alpha by animateFloatAsState(targetAlpha, tween(250), label = "lyricAlpha")
    val size by animateFloatAsState(if (isActive) 25f else 19f, tween(250), label = "lyricSize")

    val textColor = if (isActive) accent else TextPrimary.copy(alpha = alpha)
    val bgColor = if (isActive) accent.copy(alpha = 0.12f) else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.ifBlank { "♪" },
            fontSize = size.sp,
            lineHeight = (size * 1.35f).sp,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SourceChip(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MessageBlock(
    title: String,
    message: String,
    actionLabel: String?,
    accent: Color,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = DarkBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private const val LOOKAHEAD_MS = 250L
