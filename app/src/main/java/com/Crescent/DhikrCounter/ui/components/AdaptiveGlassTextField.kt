package com.Crescent.DhikrCounter.ui.components

import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.ui.components.catalog.utils.scale
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.Crescent.DhikrCounter.ui.components.LocalGlassSettings
import com.Crescent.DhikrCounter.ui.components.AdaptiveLuminanceProvider
import com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor

@Composable
fun AdaptiveGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    backdrop: Backdrop,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    adaptiveLuminance: Boolean = true
) {
    val isLightTheme = !isSystemInDarkTheme()
    val glassSettings = LocalGlassSettings.current
    val isLuminanceRoot = !com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceActive.current && adaptiveLuminance
    val layer = if (isLuminanceRoot) rememberGraphicsLayer() else null

    val effectsBlock: BackdropEffectScope.() -> Unit = remember(isLightTheme, glassSettings) {
        val block: BackdropEffectScope.() -> Unit = {
            colorControls(
                brightness = if (isLightTheme) 0.05f else -0.05f,
                saturation = 1.1f
            )
            blur(glassSettings.blurRadius.dp.toPx())
            liquidLens(
                refractionHeight = glassSettings.refractionHeight.dp.toPx(),
                refractionAmount = glassSettings.refractionAmount.dp.toPx(),
                chromaticAberration = glassSettings.chromaticAberration,
                depthEffect = true
            )
        }
        block
    }
    val textFieldShape = remember(glassSettings.cornerRadius) {
        RoundedRectangle(glassSettings.cornerRadius.dp)
    }

    val onDrawBackdropBlock: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = remember(isLuminanceRoot, layer) {
        val block: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = { drawBackdrop ->
            drawBackdrop()
            if (isLuminanceRoot) {
                layer?.record(
                    density = this,
                    layoutDirection = layoutDirection,
                    size = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                ) { drawBackdrop() }
            }
        }
        block
    }

    AdaptiveLuminanceProvider(layer = layer, enabled = isLuminanceRoot) {
        val contentColor = LocalPrismalAdaptiveColor.current
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .fillMaxWidth()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { textFieldShape },
                    effects = effectsBlock,
                    highlight = { Highlight.Plain },
                    onDrawBackdrop = onDrawBackdropBlock
                )
                .padding(vertical = 12.dp),
            textStyle = TextStyle(
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = label,
                            color = contentColor.copy(alpha = 0.5f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}
