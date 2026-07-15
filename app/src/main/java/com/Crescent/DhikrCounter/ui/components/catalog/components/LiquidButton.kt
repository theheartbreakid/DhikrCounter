package com.Crescent.DhikrCounter.ui.components.catalog.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
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
fun LiquidButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    isInteractive: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    adaptiveLuminance: Boolean = true,
    content: @Composable RowScope.() -> Unit
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
        Row(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { Capsule() },
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
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 4f.dp.toPx() / size.height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                        (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                        (height / width).fastCoerceAtMost(1f)
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
                .clickable(
                    interactionSource = null,
                    indication = if (isInteractive) null else LocalIndication.current,
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
                )
                .height(48f.dp)
                .padding(horizontal = 16f.dp),
            horizontalArrangement = Arrangement.spacedBy(8f.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
