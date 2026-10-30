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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AudioTrack
import com.example.data.model.RadioBrowserState
import com.example.data.model.RadioCountry
import com.example.data.model.StreamingStation
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/** Radio Dunia: jelajah stasiun dari Radio Browser, bisa difilter per negara dan nama. */
@Composable
fun RadioBrowserSection(
    state: RadioBrowserState,
    countries: List<RadioCountry>,
    selectedCountry: RadioCountry?,
    query: String,
    currentTrack: AudioTrack?,
    isPlaying: Boolean,
    onSelectCountry: (RadioCountry?) -> Unit,
    onQueryChange: (String) -> Unit,
    onStationSelect: (StreamingStation) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCountryPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("radio_search_input"),
            placeholder = { Text("Cari nama stasiun...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Cari", tint = NeonCyan)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = TextMuted)
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
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeonCyan
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pemilih negara
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                .clickable { showCountryPicker = true }
                .testTag("radio_country_picker"),
            color = DarkSurface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Public, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Negara", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text(
                        text = if (selectedCountry != null) {
                            "${flagEmoji(selectedCountry.code)} ${selectedCountry.name} (${selectedCountry.stationCount})"
                        } else {
                            "Semua Negara"
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when {
            state.stations.isEmpty() && state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                }
            }

            state.stations.isEmpty() && state.error != null -> {
                CenterMessage(
                    title = "Gagal memuat stasiun",
                    message = state.error,
                    actionLabel = "Coba Lagi",
                    onAction = onRetry
                )
            }

            state.stations.isEmpty() -> {
                CenterMessage(
                    title = "Stasiun tidak ditemukan",
                    message = "Coba kata kunci lain atau pilih negara yang berbeda.",
                    actionLabel = null,
                    onAction = {}
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(state.stations, key = { _, s -> s.id }) { index, station ->
                        // Muat halaman berikutnya saat mendekati akhir daftar
                        if (index >= state.stations.size - 4 && state.canLoadMore && !state.isLoading) {
                            LaunchedEffect(state.stations.size) { onLoadMore() }
                        }
                        val isCurrent = currentTrack?.uri == station.streamUrl
                        StationItemCard(
                            station = station,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            onSelect = { onStationSelect(station) }
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = NeonCyan,
                                    strokeWidth = 2.dp
                                )
                            } else if (state.error != null) {
                                Text(state.error, color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                                TextButton(onClick = onLoadMore) { Text("Coba Lagi", color = NeonCyan) }
                            }
                            Text(
                                text = "Data stasiun: radio-browser.info",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            countries = countries,
            selected = selectedCountry,
            onSelect = {
                onSelectCountry(it)
                showCountryPicker = false
            },
            onDismiss = { showCountryPicker = false }
        )
    }
}

@Composable
private fun CountryPickerDialog(
    countries: List<RadioCountry>,
    selected: RadioCountry?,
    onSelect: (RadioCountry?) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(countries, filter) {
        if (filter.isBlank()) countries
        else countries.filter {
            it.name.contains(filter, ignoreCase = true) || it.code.equals(filter.trim(), ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("radio_country_dialog")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pilih Negara",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cari negara...", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkBg,
                        unfocusedContainerColor = DarkBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonCyan
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filter.isBlank()) {
                        item {
                            CountryRow(
                                label = "Semua Negara",
                                trailing = null,
                                isSelected = selected == null,
                                onClick = { onSelect(null) }
                            )
                        }
                    }
                    items(filtered, key = { it.code }) { c ->
                        CountryRow(
                            label = "${flagEmoji(c.code)}  ${c.name}",
                            trailing = c.stationCount.toString(),
                            isSelected = selected?.code == c.code,
                            onClick = { onSelect(c) }
                        )
                    }
                    if (filtered.isEmpty() && filter.isNotBlank()) {
                        item {
                            Text(
                                text = "Negara tidak ditemukan",
                                color = TextMuted,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Tutup", color = TextSecondary) }
                }
            }
        }
    }
}

@Composable
private fun CountryRow(label: String, trailing: String?, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSurfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (isSelected) NeonCyan else TextPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Text(text = trailing, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CenterMessage(title: String, message: String, actionLabel: String?, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
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
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Kode negara 2 huruf -> emoji bendera (regional indicator). */
private fun flagEmoji(code: String): String {
    if (code.length != 2) return "🌐"
    val base = 0x1F1E6
    return code.uppercase().map { String(Character.toChars(base + (it - 'A'))) }.joinToString("")
}
