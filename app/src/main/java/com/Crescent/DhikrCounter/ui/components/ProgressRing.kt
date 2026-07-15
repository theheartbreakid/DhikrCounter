package com.Crescent.DhikrCounter.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 12f,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.2f),
    gradient: Brush? = null,
    isWavy: Boolean = false,
    amplitude: Float = 1.0f,
    wavelength: Float = 20f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "ProgressAnimation"
    )

    if (isWavy) {
        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = modifier,
            color = color,
            trackColor = trackColor,
            stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth),
            amplitude = { amplitude },
            wavelength = wavelength.dp
        )
    } else {
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val strokeWidthPx = strokeWidth
            val diameter = size.minDimension
            val radius = (diameter - strokeWidthPx) / 2

            // Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx)
            )
            // Progress
            val arcSize = radius * 2
            if (gradient != null) {
                drawArc(
                    brush = gradient,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            } else {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        center.x - radius,
                        center.y - radius
                    ),
                    size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }
        }
    }
}
