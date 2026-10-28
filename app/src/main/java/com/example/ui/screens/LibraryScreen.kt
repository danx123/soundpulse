package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioFormat
import com.example.data.model.AudioSourceType
import com.example.data.model.AudioTrack
import com.example.ui.components.AppFooterCard
import com.example.ui.components.FormatBadge
import com.example.ui.components.formatTime
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

@Composable
fun LibraryScreen(
    tracks: List<AudioTrack>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    searchQuery: String,
    selectedFormat: AudioFormat?,
    onSearchQueryChange: (String) -> Unit,
    onFormatFilterSelect: (AudioFormat?) -> Unit,
    onTrackSelect: (AudioTrack) -> Unit,
    onImportLocalFiles: (List<Uri>) -> Unit,
    onScanFolder: (Uri) -> Unit,
    onShowTrackDetails: (AudioTrack) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    onAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Open Document Tree launcher for folder scanning
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onScanFolder(uri)
        }
    }

    // Open Multiple Documents launcher for individual audio file selection
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportLocalFiles(uris)
        }
    }

    val availableFormats = listOf(null) + AudioFormat.values().filter { it != AudioFormat.STREAM }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(top = 8.dp)
            .testTag("library_screen")
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("library_search_input"),
            placeholder = { Text("Cari lagu, artis, atau album...", color = TextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = NeonCyan
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextMuted
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = DarkCardBorder,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                cursorColor = NeonCyan
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row: Pindai Folder & Impor File
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("scan_folder_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pindai Folder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    filePicker.launch(
                        arrayOf(
                            "audio/*",
                            "application/ogg"
                        )
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_files_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = NeonPurple
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Impor File Audio", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Format Filter Chips (All, FLAC, MP3, WAV, AAC, OGG, OPUS)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableFormats) { format ->
                val isSelected = selectedFormat == format
                val label = format?.displayName ?: "Semua Format"
                FilterChip(
                    selected = isSelected,
                    onClick = { onFormatFilterSelect(format) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) NeonCyan else DarkCardBorder
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Track Count & Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${tracks.size} Audio Track",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            val losslessCount = tracks.count { it.format.isLossless }
            if (losslessCount > 0) {
                Text(
                    text = "$losslessCount Hi-Res Lossless",
                    style = MaterialTheme.typography.bodySmall,
                    color = HighResGreen
                )
            }
        }

        // Track List
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tidak ada lagu yang cocok",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gunakan tombol di atas untuk memilih folder atau impor file audio dari memori perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppFooterCard(onAboutClick = onAboutClick)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    TrackItemRow(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        onSelect = { onTrackSelect(track) },
                        onDetails = { onShowTrackDetails(track) },
                        onToggleFavorite = { onToggleFavorite(track.id) },
                        onAddToPlaylist = { onAddToPlaylist(track) }
                    )
                }

                item {
                    AppFooterCard(onAboutClick = onAboutClick)
                }
            }
        }
    }
}

@Composable
fun TrackItemRow(
    track: AudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onDetails: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isCurrent) 1.dp else 0.5.dp,
                color = if (isCurrent) NeonCyan else DarkCardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .testTag("track_row_${track.id}"),
        color = if (isCurrent) DarkSurfaceVariant.copy(alpha = 0.85f) else DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon or Soundwave indicator
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isCurrent) Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.3f), NeonPurple.copy(alpha = 0.3f)))
                        else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent && isPlaying) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Playing",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isCurrent) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Selected",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isCurrent) NeonCyan else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${track.artist} • ${track.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    FormatBadge(
                        formatName = track.format.displayName,
                        isLossless = track.format.isLossless
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Duration
            if (track.durationMs > 0) {
                Text(
                    text = formatTime(track.durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // More Menu
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tambahkan ke Playlist", color = TextPrimary) },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Spesifikasi Audio", color = TextPrimary) },
                        onClick = {
                            showMenu = false
                            onDetails()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (track.isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                                color = TextPrimary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                }
            }
        }
    }
}
