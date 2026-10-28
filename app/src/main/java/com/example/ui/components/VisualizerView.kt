package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple

@Composable
fun VisualizerView(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 72.dp,
    barSpacingDp: Float = 3f,
    barCornerRadiusDp: Float = 4f,
    customGradient: List<Color>? = null
) {
    val gradientColors = customGradient ?: listOf(
        NeonCyan,
        NeonPurple,
        NeonPink
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val count = amplitudes.size.coerceAtLeast(1)
        val availableWidth = size.width
        val canvasHeight = size.height

        val totalSpacing = barSpacingDp * (count - 1)
        val barWidth = ((availableWidth - totalSpacing) / count).coerceAtLeast(2f)

        val brush = Brush.verticalGradient(
            colors = gradientColors,
            startY = 0f,
            endY = canvasHeight
        )

        for (i in amplitudes.indices) {
            val amp = amplitudes[i].coerceIn(0.04f, 1.0f)
            val barHeight = (canvasHeight * amp).coerceAtLeast(4f)
            val x = i * (barWidth + barSpacingDp)
            val y = canvasHeight - barHeight

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barCornerRadiusDp, barCornerRadiusDp)
            )

            // Subtle top highlight bead
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = (barWidth / 2.5f).coerceAtMost(3f),
                center = Offset(x + barWidth / 2f, y + 2f)
            )
        }
    }
}
