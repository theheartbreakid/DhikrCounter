package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.Crescent.DhikrCounter.ui.components.catalog.BackdropDemoScaffold
import com.Crescent.DhikrCounter.ui.components.catalog.Block
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidSlider
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.shapes.RoundedRectangle

@Composable
fun GlassPlaygroundContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    var blurRadius by rememberSaveable { mutableFloatStateOf(8f) }
    var lensRadius by rememberSaveable { mutableFloatStateOf(24f) }
    var lensHeight by rememberSaveable { mutableFloatStateOf(24f) }
    var transparency by rememberSaveable { mutableFloatStateOf(0.4f) }
    var cornerRadius by rememberSaveable { mutableFloatStateOf(32f) }

    val contentBackdrop = rememberLayerBackdrop()

    BackdropDemoScaffold { backdrop ->
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .padding(24f.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(cornerRadius.dp) },
                        effects = {
                            vibrancy()
                            blur(blurRadius.dp.toPx())
                            liquidLens(lensRadius.dp.toPx(), lensHeight.dp.toPx(), depthEffect = true)
                        },
                        onDrawSurface = { drawRect(containerColor.copy(alpha = transparency)) }
                    )
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    Modifier.padding(24f.dp),
                    verticalArrangement = Arrangement.spacedBy(24f.dp)
                ) {
                    BasicText(
                        "Glass Playground",
                        style = TextStyle(contentColor, 28f.sp, FontWeight.Bold)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Blur Radius: ${blurRadius.toInt()}dp", style = TextStyle(contentColor, 14f.sp))
                        LiquidSlider(
                            value = { blurRadius },
                            onValueChange = { blurRadius = it },
                            valueRange = 0f..64f,
                            visibilityThreshold = 0.1f,
                            backdrop = backdrop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Lens Radius: ${lensRadius.toInt()}dp", style = TextStyle(contentColor, 14f.sp))
                        LiquidSlider(
                            value = { lensRadius },
                            onValueChange = { lensRadius = it },
                            valueRange = 0f..128f,
                            visibilityThreshold = 0.1f,
                            backdrop = backdrop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Lens Height: ${lensHeight.toInt()}dp", style = TextStyle(contentColor, 14f.sp))
                        LiquidSlider(
                            value = { lensHeight },
                            onValueChange = { lensHeight = it },
                            valueRange = 0f..128f,
                            visibilityThreshold = 0.1f,
                            backdrop = backdrop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Transparency: ${(transparency * 100).toInt()}%", style = TextStyle(contentColor, 14f.sp))
                        LiquidSlider(
                            value = { transparency },
                            onValueChange = { transparency = it },
                            valueRange = 0f..1f,
                            visibilityThreshold = 0.01f,
                            backdrop = backdrop
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8f.dp)) {
                        BasicText("Corner Radius: ${cornerRadius.toInt()}dp", style = TextStyle(contentColor, 14f.sp))
                        LiquidSlider(
                            value = { cornerRadius },
                            onValueChange = { cornerRadius = it },
                            valueRange = 0f..64f,
                            visibilityThreshold = 0.1f,
                            backdrop = backdrop
                        )
                    }

                    Block {
                        LiquidButton(
                            onClick = {},
                            backdrop = backdrop,
                            tint = Color(0xFF0088FF)
                        ) {
                            BasicText("Liquid Button", style = TextStyle(Color.White, 16f.sp))
                        }
                    }
                }
            }
        }
    }
}
