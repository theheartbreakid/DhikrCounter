package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.ui.components.catalog.BackdropDemoScaffold
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.shapes.RoundedRectangle

@Composable
fun ProgressiveBlurContent() {
    BackdropDemoScaffold { backdrop ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(300.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedRectangle(32.dp) },
                        effects = {
                            blur(32.dp.toPx())
                        },
                        onDrawSurface = { drawRect(Color.White.copy(0.1f)) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicText("Progressive Blur", style = TextStyle(Color.White, 24.sp))
            }
        }
    }
}
