package com.Crescent.DhikrCounter.ui.components.catalog.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy

import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.Crescent.DhikrCounter.ui.components.LocalGlassSettings
import com.Crescent.DhikrCounter.ui.components.AdaptiveLuminanceProvider

@Composable
fun LiquidChip(
    onClick: (() -> Unit)?,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    adaptiveLuminance: Boolean = true,
    content: @Composable () -> Unit
) {
    val glassSettings = LocalGlassSettings.current
    val shape = RoundedCornerShape(16.dp)
    val isLuminanceRoot = !com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceActive.current && adaptiveLuminance
    val layer = if (isLuminanceRoot) rememberGraphicsLayer() else null

    AdaptiveLuminanceProvider(layer = layer, enabled = isLuminanceRoot) {
        Box(
            modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(glassSettings.blurRadius.dp.toPx() * 0.4f)
                        liquidLens(
                            refractionHeight = glassSettings.refractionHeight.dp.toPx() * 0.4f,
                            refractionAmount = glassSettings.refractionAmount.dp.toPx() * 0.4f,
                            chromaticAberration = glassSettings.chromaticAberration * 0.4f
                        )
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        drawBackdrop()
                        if (tint.isSpecified) {
                            drawRect(tint, blendMode = BlendMode.Hue)
                            drawRect(tint.copy(alpha = 0.2f))
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
                                    drawRect(tint.copy(alpha = 0.2f))
                                }
                            }
                        }
                    },
                    onDrawSurface = {
                        // Tint applied in onDrawBackdrop
                    }
                )
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                content()
            }
        }
    }
}
