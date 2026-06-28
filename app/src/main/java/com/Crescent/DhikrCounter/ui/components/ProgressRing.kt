package com.Crescent.DhikrCounter.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.2f)
) {
    val density = LocalDensity.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "ProgressAnimation"
    )

    // Detect increment for speed boost
    var lastProgress by remember { mutableFloatStateOf(progress) }
    val speedMultiplier = remember { Animatable(1f) }
    
    LaunchedEffect(progress) {
        if (progress > lastProgress) {
            speedMultiplier.snapTo(2.5f)
            speedMultiplier.animateTo(1f, tween(1000))
        }
        lastProgress = progress
    }

    val wavelength = WavyProgressIndicatorDefaults.CircularWavelength
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    
    CircularWavyProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier,
        color = color,
        trackColor = trackColor,
        stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        trackStroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
        amplitude = { p ->
            when {
                p <= 0.01f -> 0f
                p < 0.2f -> 0.25f // Very subtle wave
                p < 0.8f -> 0.6f  // Medium wave
                p < 1.0f -> 1.0f  // Highest wave amplitude
                else -> 0f       // Wave slowly settles to zero after completion
            }
        },
        wavelength = wavelength,
        waveSpeed = wavelength * speedMultiplier.value
    )
}
