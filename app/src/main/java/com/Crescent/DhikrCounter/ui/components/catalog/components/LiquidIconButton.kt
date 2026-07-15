package com.Crescent.DhikrCounter.ui.components.catalog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.Crescent.DhikrCounter.ui.components.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.Crescent.DhikrCounter.ui.components.LocalGlassSettings
import com.Crescent.DhikrCounter.ui.components.AdaptiveLuminanceProvider
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidIconButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    adaptiveLuminance: Boolean = true,
    content: @Composable () -> Unit
) {
    val haptic = com.Crescent.DhikrCounter.ui.components.rememberPrismalHaptic()
    val animationScope = rememberCoroutineScope()

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }
    val glassSettings = LocalGlassSettings.current
    val isLuminanceRoot = !com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceActive.current && adaptiveLuminance
    val layer = if (isLuminanceRoot) rememberGraphicsLayer() else null

    AdaptiveLuminanceProvider(layer = layer, enabled = isLuminanceRoot) {
        Box(
            modifier
                .size(iconSize)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(glassSettings.blurRadius.dp.toPx())
                        liquidLens(
                            refractionHeight = glassSettings.refractionHeight.dp.toPx(),
                            refractionAmount = glassSettings.refractionAmount.dp.toPx(),
                            chromaticAberration = glassSettings.chromaticAberration
                        )
                    },
                    layerBlock = if (isInteractive) {
                        {
                            val width = size.width
                            val height = size.height

                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 4f.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) * (width / height).fastCoerceAtMost(1f)
                            scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) * (height / width).fastCoerceAtMost(1f)
                        }
                    } else {
                        null
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        drawBackdrop()
                        if (tint.isSpecified) {
                            drawRect(tint, blendMode = BlendMode.Hue)
                            drawRect(tint.copy(alpha = 0.75f))
                        }
                        if (surfaceColor.isSpecified) {
                            drawRect(surfaceColor)
                        }
                        if (isLuminanceRoot) {
                            layer?.record(
                                density = this,
                                layoutDirection = layoutDirection,
                                size = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                            ) {
                                drawBackdrop()
                                if (tint.isSpecified) {
                                    drawRect(tint, blendMode = BlendMode.Hue)
                                    drawRect(tint.copy(alpha = 0.75f))
                                }
                                if (surfaceColor.isSpecified) {
                                    drawRect(surfaceColor)
                                }
                            }
                        }
                    },
                    onDrawSurface = {
                        // Tint applied in onDrawBackdrop
                    }
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        haptic()
                        onClick()
                    }
                )
                .then(
                    if (isInteractive) {
                        Modifier
                            .then(interactiveHighlight.modifier)
                            .then(interactiveHighlight.gestureModifier)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
