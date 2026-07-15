package com.Crescent.DhikrCounter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.Crescent.DhikrCounter.DhikrApplication
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext

@Composable
fun GoalCelebration(
    trigger: SharedFlow<Unit>,
    hapticIntensity: Float = 1.0f,
    hapticEnabled: Boolean = true,
    onAnimationUpdate: (ringScale: Float, numberScale: Float, glassIntensity: Float) -> Unit
) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) > 0f
        } catch (_: Exception) {
            true
        }
    }

    val hapticManager = remember(context) {
        (context.applicationContext as DhikrApplication).hapticManager
    }

    val ringScale = remember { Animatable(1f) }
    val numberScale = remember { Animatable(1f) }
    val waveRotation = remember { Animatable(0f) }
    val waveAlpha = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }
    val glowScale = remember { Animatable(1f) }
    val glassMultiplier = remember { Animatable(1f) }
    
    val scope = rememberCoroutineScope()

    fun vibrate(type: String) {
        val duration = when (type) {
            "tick" -> 30L
            "pulse" -> 60L
            "tap" -> 20L
            else -> 40L
        }
        val amplitudeMultiplier = when (type) {
            "tick" -> 0.4f
            "pulse" -> 0.8f
            "tap" -> 0.2f
            else -> 0.5f
        }
        hapticManager.vibrate(duration, hapticIntensity * amplitudeMultiplier)
    }

    LaunchedEffect(trigger) {
        trigger.collect {
            if (!animationsEnabled) {
                vibrate("pulse")
                return@collect
            }

            scope.launch {
                // 1. Initial trigger
                vibrate("tick")
                
                // 2. Ring enlargement & Glass intensify
                launch {
                    ringScale.animateTo(
                        1.05f,
                        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
                    )
                    ringScale.animateTo(
                        1f,
                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
                    )
                }

                // 2b. Number spring animation (1.0 -> 1.10 -> 0.98 -> 1.0)
                launch {
                    numberScale.animateTo(1.10f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                    numberScale.animateTo(0.98f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                    numberScale.animateTo(1.0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                }
                
                launch {
                    glassMultiplier.animateTo(1.4f, tween(300))
                    glassMultiplier.animateTo(1f, tween(600))
                }

                // 3. Energy wave
                launch {
                    waveAlpha.animateTo(1f, tween(200))
                    waveRotation.animateTo(360f, tween(800, easing = FastOutSlowInEasing))
                    vibrate("pulse")
                    waveAlpha.animateTo(0f, tween(200))
                    waveRotation.snapTo(0f)
                }

                // 4. Glow expansion
                launch {
                    delay(600) // Start as wave completes
                    glowAlpha.animateTo(0.6f, tween(200))
                    glowScale.animateTo(1.3f, tween(600, easing = LinearOutSlowInEasing))
                    glowAlpha.animateTo(0f, tween(400))
                    glowScale.snapTo(1f)
                    vibrate("tap")
                }
            }
        }
    }

    // Effect propagation to parent
    SideEffect {
        onAnimationUpdate(ringScale.value, numberScale.value, glassMultiplier.value)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Energy Wave
        if (waveAlpha.value > 0) {
            val accentColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize(0.92f)) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.5f to accentColor.copy(alpha = waveAlpha.value),
                        1.0f to Color.Transparent,
                        center = center
                    ),
                    startAngle = waveRotation.value - 90f,
                    sweepAngle = 60f,
                    useCenter = false,
                    style = Stroke(width = 24f, cap = StrokeCap.Round)
                )
            }
        }

        // Glow
        if (glowAlpha.value > 0) {
            val glowColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize(0.9f).scale(glowScale.value)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = glowAlpha.value),
                            glowColor.copy(alpha = glowAlpha.value * 0.5f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension / 2
                    ),
                    radius = size.minDimension / 2,
                    center = center
                )
            }
        }
    }
}

private suspend fun delay(timeMillis: Long) {
    kotlinx.coroutines.delay(timeMillis)
}
