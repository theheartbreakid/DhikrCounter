package com.Crescent.DhikrCounter.ui.components.catalog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.Crescent.DhikrCounter.ui.components.rememberPrismalHaptic
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.Crescent.DhikrCounter.ui.components.LocalGlassSettings
import com.Crescent.DhikrCounter.ui.components.AdaptiveLuminanceProvider
import com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalDensity

@Composable
fun LiquidCard(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    onClick: (() -> Unit)? = null,
    tint: Color = Color.Unspecified,
    adaptiveLuminance: Boolean = true,
    content: @Composable () -> Unit
) {
    val glassSettings = LocalGlassSettings.current
    val glassIntensity = com.Crescent.DhikrCounter.ui.components.LocalGlassIntensity.current
    val actualShape = remember(shape, glassSettings.cornerRadius) {
        shape ?: RoundedCornerShape(glassSettings.cornerRadius.dp)
    }
    val isLuminanceRoot = !com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceActive.current && adaptiveLuminance
    val layer = if (isLuminanceRoot) rememberGraphicsLayer() else null
    val haptic = rememberPrismalHaptic()

    val effectsBlock: BackdropEffectScope.() -> Unit = remember(glassSettings, glassIntensity) {
        val block: BackdropEffectScope.() -> Unit = {
            vibrancy()
            blur(glassSettings.blurRadius.dp.toPx() * glassIntensity)
            liquidLens(
                refractionHeight = glassSettings.refractionHeight.dp.toPx(),
                refractionAmount = glassSettings.refractionAmount.dp.toPx(),
                chromaticAberration = glassSettings.chromaticAberration
            )
        }
        block
    }

    val onDrawBackdropBlock: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = remember(tint, isLuminanceRoot, layer) {
        val block: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = { drawBackdrop ->
            drawBackdrop()
            if (tint.isSpecified) {
                drawRect(tint, blendMode = BlendMode.Hue)
                drawRect(tint.copy(alpha = 0.05f))
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
                        drawRect(tint.copy(alpha = 0.05f))
                    }
                }
            }
        }
        block
    }

    AdaptiveLuminanceProvider(layer = layer, enabled = isLuminanceRoot) {
        Box(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { actualShape },
                    effects = effectsBlock,
                    onDrawBackdrop = onDrawBackdropBlock,
                    onDrawSurface = {
                        // Tint is now applied in onDrawBackdrop for true glass material rendering
                    }
                )
                .clip(actualShape)
                .then(if (onClick != null) Modifier.clickable { 
                    haptic()
                    onClick() 
                } else Modifier),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }
    }
}
