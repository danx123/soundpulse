package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.R
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.AudioTrack
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.HighResGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentQueueSheet(
    queue: List<AudioTrack>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onPlayTrackAtIndex: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onReorderQueue: (fromIndex: Int, toIndex: Int) -> Unit,
    onShuffleQueue: () -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Cari posisi indeks lagu yang sedang aktif diputar
    val playingIndex = remember(queue, currentTrack?.id) {
        val idx = queue.indexOfFirst { it.id == currentTrack?.id }
        if (idx >= 0) idx else 0
    }

    // Inisialisasi posisi awal daftar agar langsung fokus ke lagu yang sedang diputar (bukan di paling atas)
    val initialScrollIndex = remember {
        (playingIndex - 1).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialScrollIndex
    )

    // Animasi scroll halus ke lagu yang sedang diputar saat antrean dibuka atau saat berpindah lagu
    LaunchedEffect(currentTrack?.id) {
        val targetIdx = queue.indexOfFirst { it.id == currentTrack?.id }
        if (targetIdx >= 0) {
            val targetScroll = (targetIdx - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    val scrollToCurrentTrack: () -> Unit = {
        val targetIdx = queue.indexOfFirst { it.id == currentTrack?.id }
        if (targetIdx >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem((targetIdx - 1).coerceAtLeast(0))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBg,
        dragHandle = null,
        modifier = modifier.testTag("current_queue_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Sheet Handle bar
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Header Row (Dapat diketuk untuk langsung fokus ke lagu aktif)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.35f)))
                        )
                        .clickable(enabled = currentTrack != null) { scrollToCurrentTrack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = currentTrack != null) { scrollToCurrentTrack() }
                ) {
                    Text(
                        text = "Daftar Putar (Antrean)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    val currentPos = queue.indexOfFirst { it.id == currentTrack?.id }
                    val subtitle = if (currentPos >= 0) {
                        stringResource(R.string.queue_playing_now, currentPos + 1, queue.size)
                    } else {
                        stringResource(R.string.queue_order_hint, queue.size)
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentPos >= 0) NeonCyan else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("close_queue_sheet_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Action Toolbar (Fokus Lagu Aktif, Acak Urutan & Kosongkan Antrean)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tombol Cepat: Fokus ke lagu yang sedang diputar
                OutlinedButton(
                    onClick = scrollToCurrentTrack,
                    enabled = currentTrack != null && queue.any { it.id == currentTrack.id },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonCyan,
                        disabledContentColor = TextMuted.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (currentTrack != null) NeonCyan.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.2f)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("queue_focus_current_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.queue_focus_current), fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onShuffleQueue,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextMuted.copy(alpha = 0.35f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("queue_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Acak", fontSize = 11.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onClearQueue,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = NeonPink
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("queue_clear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", fontSize = 11.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Antrean Masih Kosong",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pilih lagu atau playlist untuk memuat daftar putar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("queue_track_list"),
                    contentPadding = PaddingValues(bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = queue,
                        key = { _, track -> track.id }
                    ) { index, track ->
                        val isCurrent = currentTrack?.id == track.id

                        QueueItemRow(
                            track = track,
                            index = index,
                            totalCount = queue.size,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onPlay = { onPlayTrackAtIndex(index) },
                            onRemove = { onRemoveFromQueue(index) },
                            onMoveUp = {
                                if (index > 0) onReorderQueue(index, index - 1)
                            },
                            onMoveDown = {
                                if (index < queue.size - 1) onReorderQueue(index, index + 1)
                            },
                            onDragReorder = { from, to ->
                                onReorderQueue(from, to)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueItemRow(
    track: AudioTrack,
    index: Int,
    totalCount: Int,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 72.dp.toPx() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer {
                translationY = offsetY
                shadowElevation = if (isDragging) 16f else 0f
            }
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isCurrent) 1.5.dp else 0.5.dp,
                color = when {
                    isDragging -> NeonCyan
                    isCurrent -> NeonCyan
                    else -> DarkCardBorder
                },
                shape = RoundedCornerShape(14.dp)
            )
            .background(
                when {
                    isDragging -> DarkSurfaceVariant
                    isCurrent -> NeonCyan.copy(alpha = 0.10f)
                    else -> DarkSurface
                }
            )
            .clickable { onPlay() }
            .testTag("queue_item_$index"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle with vertical drag gesture
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .pointerInput(index, totalCount) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                isDragging = false
                                offsetY = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                offsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y

                                if (offsetY > itemHeightPx && index < totalCount - 1) {
                                    onDragReorder(index, index + 1)
                                    offsetY -= itemHeightPx
                                } else if (offsetY < -itemHeightPx && index > 0) {
                                    onDragReorder(index, index - 1)
                                    offsetY += itemHeightPx
                                }
                            }
                        )
                    }
                    .testTag("queue_drag_handle_$index"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Seret untuk ubah urutan",
                    tint = if (isDragging) NeonCyan else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Track index or playing indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) NeonCyan else DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = DarkBg,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = "${index + 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Artwork Thumbnail
            TrackArtwork(
                track = track,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isCurrent) NeonCyan else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NeonCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, NeonCyan)
                        ) {
                            Text(
                                text = if (isPlaying) "MEMUTAR" else "JEDA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTime(track.durationMs),
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Move Up and Move Down accessibility buttons
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = index > 0,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("queue_move_up_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Pindah ke atas",
                        tint = if (index > 0) TextSecondary else TextMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = index < totalCount - 1,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("queue_move_down_$index")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Pindah ke bawah",
                        tint = if (index < totalCount - 1) TextSecondary else TextMuted.copy(alpha = 0.3f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Remove from queue button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("queue_remove_$index")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hapus dari Antrean",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
