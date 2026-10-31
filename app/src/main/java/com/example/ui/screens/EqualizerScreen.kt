package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EqualizerBand
import com.example.data.model.EqualizerPreset
import com.example.data.model.EqualizerState
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlowAmber
import com.example.ui.theme.HighResGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EqualizerScreen(
    state: EqualizerState,
    onToggleEnable: (Boolean) -> Unit,
    onBandLevelChange: (bandIndex: Int, millibels: Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onApplyPreset: (EqualizerPreset) -> Unit,
    onSetReverb: (String) -> Unit,
    onReplayGainToggle: (Boolean) -> Unit = {},
    onReplayGainTargetChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val presets = EqualizerState.PRESETS

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState())
            .testTag("equalizer_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Master Header Card with Power Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.isEnabled) Brush.horizontalGradient(listOf(NeonCyan, NeonPurple))
                else Brush.horizontalGradient(listOf(DarkCardBorder, DarkCardBorder))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isEnabled) Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                                else Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = if (state.isEnabled) DarkBg else TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "EQUALIZER GRAFIS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (state.isEnabled) HighResGreen.copy(alpha = 0.2f)
                                        else TextMuted.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (state.isEnabled) "AKTIF" else "NONAKTIF",
                                    color = if (state.isEnabled) HighResGreen else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Preset saat ini: ${state.currentPreset}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }
                }

                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = onToggleEnable,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.testTag("equalizer_master_switch")
                )
            }
        }

        // Frequency Response Curve Canvas
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kurva Respon Frekuensi (dB)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "-15 dB s/d +15 dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                EqCurveCanvas(
                    bands = state.bands,
                    isEnabled = state.isEnabled,
                    minLvl = state.minLevelMillibels,
                    maxLvl = state.maxLevelMillibels,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            }
        }

        // Preset Chips Row
        Column {
            Text(
                text = "Preset Suara",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    val isSelected = state.currentPreset.equals(preset.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onApplyPreset(preset) },
                        label = {
                            Text(
                                text = preset.name,
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
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        // 5 Graphic Frequency Band Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pita Frekuensi (Graphic Bands)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                state.bands.forEach { band ->
                    BandSliderRow(
                        band = band,
                        isEnabled = state.isEnabled,
                        minLvl = state.minLevelMillibels,
                        maxLvl = state.maxLevelMillibels,
                        onLevelChange = { newLevel ->
                            onBandLevelChange(band.index, newLevel)
                        }
                    )
                }
            }
        }

        // Effects Row: Bass Boost & 3D Surround Virtualizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bass Boost Card
            EffectControlCard(
                title = "Bass Boost",
                subtitle = "${state.bassBoostStrength / 10}%",
                value = state.bassBoostStrength.toFloat() / 1000f,
                icon = Icons.Default.VolumeUp,
                accentColor = NeonCyan,
                isEnabled = state.isEnabled,
                onValueChange = { onBassBoostChange((it * 1000).toInt()) },
                modifier = Modifier.weight(1f)
            )

            // 3D Virtualizer Card
            EffectControlCard(
                title = "3D Surround",
                subtitle = "${state.virtualizerStrength / 10}%",
                value = state.virtualizerStrength.toFloat() / 1000f,
                icon = Icons.Default.SurroundSound,
                accentColor = NeonPurple,
                isEnabled = state.isEnabled,
                onValueChange = { onVirtualizerChange((it * 1000).toInt()) },
                modifier = Modifier.weight(1f)
            )
        }

        // Audio Normalization & ReplayGain Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.isEnabled && state.isReplayGainEnabled) NeonCyan.copy(alpha = 0.5f)
                else DarkCardBorder
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.isEnabled && state.isReplayGainEnabled)
                                        NeonCyan.copy(alpha = 0.2f)
                                    else DarkSurfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (state.isEnabled && state.isReplayGainEnabled) NeonCyan else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.eq_normalization_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "EBU R128",
                                        color = NeonCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = if (state.isEnabled && state.isReplayGainEnabled)
                                    stringResource(R.string.eq_normalization_active)
                                else
                                    "Nonaktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isEnabled && state.isReplayGainEnabled) HighResGreen else TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = state.isReplayGainEnabled,
                        onCheckedChange = onReplayGainToggle,
                        enabled = state.isEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = NeonCyan,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("replaygain_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.eq_normalization_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )

                if (state.isReplayGainEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Kenyaringan (Gain)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.eq_normalization_target_gain, state.replayGainTargetDb),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonCyan
                        )
                    }

                    Slider(
                        value = state.replayGainTargetDb.coerceIn(0f, 12f) / 12f,
                        onValueChange = { norm ->
                            val targetDb = norm * 12f
                            onReplayGainTargetChange(targetDb)
                        },
                        enabled = state.isEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("replaygain_target_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "0 dB (Halus)", fontSize = 10.sp, color = TextMuted)
                        Text(text = "+6 dB (Standar)", fontSize = 10.sp, color = NeonCyan)
                        Text(text = "+12 dB (Maksimal)", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        // Acoustic Reverb Profiles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Profil Reverb Ruang Akustik",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val reverbOptions = listOf("None", "Small Room", "Medium Room", "Large Room", "Hall", "Large Hall", "Plate")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(reverbOptions) { opt ->
                        val isSelected = state.reverbEffect.equals(opt, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetReverb(opt) },
                            label = { Text(opt, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlowAmber.copy(alpha = 0.2f),
                                selectedLabelColor = GlowAmber,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) GlowAmber else DarkCardBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BandSliderRow(
    band: EqualizerBand,
    isEnabled: Boolean,
    minLvl: Int,
    maxLvl: Int,
    onLevelChange: (Int) -> Unit
) {
    val rangeSpan = (maxLvl - minLvl).coerceAtLeast(1)
    val sliderNorm = ((band.levelMillibels - minLvl).toFloat() / rangeSpan.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = band.frequencyLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isEnabled) NeonCyan else TextMuted
            )

            val dbSign = if (band.decibelValue > 0) "+" else ""
            Text(
                text = "$dbSign%.1f dB".format(band.decibelValue),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isEnabled) TextPrimary else TextMuted
            )
        }

        Slider(
            value = sliderNorm,
            onValueChange = { norm ->
                val newMillibels = (minLvl + (norm * rangeSpan)).toInt()
                onLevelChange(newMillibels)
            },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("eq_band_slider_${band.index}"),
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = DarkSurfaceVariant,
                disabledThumbColor = TextMuted,
                disabledActiveTrackColor = TextMuted
            )
        )
    }
}

@Composable
fun EffectControlCard(
    title: String,
    subtitle: String,
    value: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isEnabled) 0.15f else 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isEnabled) accentColor else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = if (isEnabled) accentColor else TextMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = value.coerceIn(0f, 1f),
                onValueChange = onValueChange,
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("effect_slider_${title.lowercase().replace(" ", "_")}"),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = DarkSurfaceVariant,
                    disabledThumbColor = TextMuted,
                    disabledActiveTrackColor = TextMuted
                )
            )
        }
    }
}

@Composable
fun EqCurveCanvas(
    bands: List<EqualizerBand>,
    isEnabled: Boolean,
    minLvl: Int,
    maxLvl: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Draw Center Zero dB Reference Line
        drawLine(
            color = Color(0xFF334155),
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1f
        )

        if (bands.isEmpty()) return@Canvas

        val range = (maxLvl - minLvl).coerceAtLeast(1).toFloat()
        val path = Path()

        val points = bands.mapIndexed { idx, band ->
            val x = (idx.toFloat() / (bands.size - 1).coerceAtLeast(1)) * width
            val norm = (band.levelMillibels - minLvl) / range
            val y = height - (norm * height)
            Offset(x, y)
        }

        path.moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val ctrlX1 = (p0.x + p1.x) / 2f
            path.cubicTo(ctrlX1, p0.y, ctrlX1, p1.y, p1.x, p1.y)
        }

        val strokeColor = if (isEnabled) NeonCyan else Color(0xFF64748B)
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw band control dots
        points.forEach { pt ->
            drawCircle(
                color = if (isEnabled) NeonPink else Color(0xFF475569),
                radius = 4.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 1.5.dp.toPx(),
                center = pt
            )
        }
    }
}
