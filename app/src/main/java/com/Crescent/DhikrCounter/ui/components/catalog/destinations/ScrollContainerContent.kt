package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.ui.components.catalog.BackdropDemoScaffold
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun ScrollContainerContent() {
    BackdropDemoScaffold { backdrop ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(20) { index ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(24.dp) },
                            effects = {
                                vibrancy()
                                liquidLens(16.dp.toPx(), 16.dp.toPx(), depthEffect = true)
                            },
                            onDrawSurface = { drawRect(Color.White.copy(0.2f)) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("Card $index", style = TextStyle(Color.White, 20.sp))
                }
            }
        }
    }
}
