package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.AudioFormat
import com.example.data.model.AudioTrack

data class TrackAuraColors(
    val dominant: Color,
    val secondary: Color,
    val accent: Color,
    val backgroundTint: Color,
    val surfaceHighlight: Color,
    val onButtonColor: Color = Color(0xFF090D14)
) {
    val auraLinearBrush: Brush
        get() = Brush.linearGradient(listOf(dominant, secondary, accent))

    val radialGlowBrush: Brush
        get() = Brush.radialGradient(
            colors = listOf(
                dominant.copy(alpha = 0.40f),
                secondary.copy(alpha = 0.20f),
                Color.Transparent
            ),
            center = Offset(400f, 400f),
            radius = 900f
        )
}

object DynamicAuraExtractor {
    fun extract(track: AudioTrack?): TrackAuraColors {
        if (track == null) {
            return TrackAuraColors(
                dominant = NeonCyan,
                secondary = NeonPurple,
                accent = NeonPink,
                backgroundTint = DarkBg,
                surfaceHighlight = DarkSurfaceVariant
            )
        }

        // Palette presets based on format and title keywords
        val titleLower = track.title.lowercase()
        val genreLower = track.artist.lowercase()

        return when {
            titleLower.contains("lo-fi") || titleLower.contains("chill") || genreLower.contains("ambient") -> {
                TrackAuraColors(
                    dominant = Color(0xFFFF9E00), // Sunset Amber
                    secondary = Color(0xFFFF5470), // Warm Coral Pink
                    accent = Color(0xFFFFD166), // Golden Glow
                    backgroundTint = Color(0xFF160E08),
                    surfaceHighlight = Color(0xFF281810)
                )
            }
            titleLower.contains("synth") || titleLower.contains("cyber") || genreLower.contains("defcon") -> {
                TrackAuraColors(
                    dominant = Color(0xFFE024C3), // Synthwave Magenta
                    secondary = Color(0xFF00F0FF), // Cyber Cyan
                    accent = Color(0xFF7209B7), // Deep Violet
                    backgroundTint = Color(0xFF130919),
                    surfaceHighlight = Color(0xFF23112E)
                )
            }
            track.format == AudioFormat.FLAC || titleLower.contains("lossless") || track.format == AudioFormat.WAV -> {
                TrackAuraColors(
                    dominant = Color(0xFF10B981), // Emerald Hi-Res Master
                    secondary = Color(0xFF00F0FF), // Electric Cyan
                    accent = Color(0xFF34D399), // Mint Aurora
                    backgroundTint = Color(0xFF071712),
                    surfaceHighlight = Color(0xFF102820)
                )
            }
            titleLower.contains("space") || titleLower.contains("drone") || titleLower.contains("ethereal") -> {
                TrackAuraColors(
                    dominant = Color(0xFF8B5CF6), // Cosmic Violet
                    secondary = Color(0xFF3B82F6), // Deep Space Blue
                    accent = Color(0xFF06B6D4), // Cyan Star
                    backgroundTint = Color(0xFF0D0B1A),
                    surfaceHighlight = Color(0xFF1B1733)
                )
            }
            titleLower.contains("jazz") || titleLower.contains("groove") || titleLower.contains("lounge") -> {
                TrackAuraColors(
                    dominant = Color(0xFFF59E0B), // Warm Brass Gold
                    secondary = Color(0xFFD97706), // Bourbon Amber
                    accent = Color(0xFFFCD34D), // Soft Champagne
                    backgroundTint = Color(0xFF181206),
                    surfaceHighlight = Color(0xFF2B200C)
                )
            }
            else -> {
                // Deterministic signature color based on track ID hash
                val hash = kotlin.math.abs(track.id.hashCode() + track.title.hashCode())
                val hueShift = hash % 5
                when (hueShift) {
                    0 -> TrackAuraColors(
                        dominant = Color(0xFF00F0FF),
                        secondary = Color(0xFF9D4EDD),
                        accent = Color(0xFFFF5470),
                        backgroundTint = Color(0xFF090E17),
                        surfaceHighlight = Color(0xFF141F30)
                    )
                    1 -> TrackAuraColors(
                        dominant = Color(0xFFFF007A), // Hot Neon Pink
                        secondary = Color(0xFF7928CA), // Purple Plasma
                        accent = Color(0xFFFF8A00), // Fire Orange
                        backgroundTint = Color(0xFF170912),
                        surfaceHighlight = Color(0xFF2D1123)
                    )
                    2 -> TrackAuraColors(
                        dominant = Color(0xFF00E5FF), // Electric Aqua
                        secondary = Color(0xFF10B981), // Emerald Green
                        accent = Color(0xFF76FF03), // Lime Accent
                        backgroundTint = Color(0xFF081517),
                        surfaceHighlight = Color(0xFF0F272B)
                    )
                    3 -> TrackAuraColors(
                        dominant = Color(0xFFFF3366), // Crimson Scarlet
                        secondary = Color(0xFFFF9900), // Tangerine
                        accent = Color(0xFFFFD600), // Solar Flare
                        backgroundTint = Color(0xFF1A090D),
                        surfaceHighlight = Color(0xFF331219)
                    )
                    else -> TrackAuraColors(
                        dominant = Color(0xFF8A2BE2), // Blue Violet
                        secondary = Color(0xFF00C9FF), // Ice Cyan
                        accent = Color(0xFF92FE9D), // Spring Aura
                        backgroundTint = Color(0xFF0E0B19),
                        surfaceHighlight = Color(0xFF1C1632)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberDynamicAura(track: AudioTrack?): TrackAuraColors {
    val targetColors = remember(track?.id, track?.title) {
        DynamicAuraExtractor.extract(track)
    }

    val animatedDominant by animateColorAsState(
        targetValue = targetColors.dominant,
        animationSpec = tween(durationMillis = 650),
        label = "AuraDominant"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = targetColors.secondary,
        animationSpec = tween(durationMillis = 650),
        label = "AuraSecondary"
    )

    val animatedAccent by animateColorAsState(
        targetValue = targetColors.accent,
        animationSpec = tween(durationMillis = 650),
        label = "AuraAccent"
    )

    val animatedBgTint by animateColorAsState(
        targetValue = targetColors.backgroundTint,
        animationSpec = tween(durationMillis = 650),
        label = "AuraBgTint"
    )

    val animatedSurface by animateColorAsState(
        targetValue = targetColors.surfaceHighlight,
        animationSpec = tween(durationMillis = 650),
        label = "AuraSurface"
    )

    return TrackAuraColors(
        dominant = animatedDominant,
        secondary = animatedSecondary,
        accent = animatedAccent,
        backgroundTint = animatedBgTint,
        surfaceHighlight = animatedSurface
    )
}
