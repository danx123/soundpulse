package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack
import com.example.data.model.CloudProviderPreset
import com.example.ui.components.FormatBadge
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.HighResGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(
    cloudTracks: List<AudioTrack>,
    presets: List<CloudProviderPreset>,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    onTrackSelect: (AudioTrack) -> Unit,
    onAddCloudTrack: (title: String, artist: String, url: String, provider: String, format: AudioFormat) -> Unit,
    onShowTrackDetails: (AudioTrack) -> Unit,
    onAddToPlaylist: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCloudDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(top = 8.dp)
            .testTag("cloud_screen")
    ) {
        // Cloud Hero Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.4f)))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(NeonCyan.copy(alpha = 0.2f), NeonPurple.copy(alpha = 0.3f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Cloud Storage",
                        tint = NeonCyan,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLOUD STORAGE STREAMING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Putar Langsung dari Cloud",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Google Cloud, Nextcloud, WebDAV, atau link audio HTTP langsung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Action Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tersimpan di Cloud (${cloudTracks.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Button(
                onClick = { showAddCloudDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.testTag("add_cloud_track_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tambah URL Cloud", fontSize = 12.sp)
            }
        }

        // List of Cloud Tracks and Presets
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Saved cloud audio tracks
            items(cloudTracks, key = { it.id }) { track ->
                val isCurrent = currentTrack?.id == track.id
                TrackItemRow(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onSelect = { onTrackSelect(track) },
                    onDetails = { onShowTrackDetails(track) },
                    onToggleFavorite = {},
                    onAddToPlaylist = { onAddToPlaylist(track) }
                )
            }

            // Presets Showcase Header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Preset Layanan Cloud yang Didukung",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            items(presets, key = { it.id }) { preset ->
                CloudPresetCard(
                    preset = preset,
                    onTestPlay = {
                        onAddCloudTrack(
                            preset.sampleTrackTitle,
                            "Cloud Demo",
                            preset.demoAudioUrl,
                            preset.name,
                            preset.sampleFormat
                        )
                    }
                )
            }
        }
    }

    // Add Cloud URL Dialog
    if (showAddCloudDialog) {
        var title by remember { mutableStateOf("") }
        var artist by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var provider by remember { mutableStateOf("Google Cloud Storage") }
        var selectedFormat by remember { mutableStateOf(AudioFormat.MP3) }
        var formatExpanded by remember { mutableStateOf(false) }

        val formatOptions = listOf(AudioFormat.MP3, AudioFormat.FLAC, AudioFormat.WAV, AudioFormat.AAC, AudioFormat.OGG)

        AlertDialog(
            onDismissRequest = { showAddCloudDialog = false },
            title = { Text("Tambah Track dari Cloud", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Judul Lagu") },
                        placeholder = { Text("Contoh: Symphony No. 5") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            cursorColor = NeonCyan
                        )
                    )

                    OutlinedTextField(
                        value = artist,
                        onValueChange = { artist = it },
                        label = { Text("Artis / Produser") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            cursorColor = NeonCyan
                        )
                    )

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Direct Audio URL (HTTP/HTTPS)") },
                        placeholder = { Text("https://storage.googleapis.com/.../song.flac") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            cursorColor = NeonCyan
                        )
                    )

                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Penyedia Cloud (GCS, Nextcloud, WebDAV, dll)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            cursorColor = NeonCyan
                        )
                    )

                    // Format selector
                    ExposedDropdownMenuBox(
                        expanded = formatExpanded,
                        onExpandedChange = { formatExpanded = !formatExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFormat.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Format File Audio") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkCardBorder
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = formatExpanded,
                            onDismissRequest = { formatExpanded = false }
                        ) {
                            formatOptions.forEach { fmt ->
                                DropdownMenuItem(
                                    text = { Text("${fmt.displayName} ${if (fmt.isLossless) "(Lossless)" else ""}") },
                                    onClick = {
                                        selectedFormat = fmt
                                        formatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            onAddCloudTrack(
                                title.ifBlank { "Cloud Audio Track" },
                                artist.ifBlank { "Cloud Stream" },
                                url.trim(),
                                provider.ifBlank { "Cloud Storage" },
                                selectedFormat
                            )
                            showAddCloudDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg)
                ) {
                    Text("Tambah ke Library")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCloudDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun CloudPresetCard(
    preset: CloudProviderPreset,
    onTestPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(0.5.dp, DarkCardBorder, RoundedCornerShape(14.dp)),
        color = DarkSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = preset.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = preset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FormatBadge(
                        formatName = preset.sampleFormat.displayName,
                        isLossless = preset.sampleFormat.isLossless
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = preset.providerType,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }

            Button(
                onClick = onTestPlay,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = NeonCyan),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Coba", fontSize = 11.sp)
            }
        }
    }
}
