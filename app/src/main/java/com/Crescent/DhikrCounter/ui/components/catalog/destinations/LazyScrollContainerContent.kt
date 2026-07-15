package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.RoundedRectangle

@Composable
fun LazyScrollContainerContent() {
    BackdropDemoScaffold { backdrop ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(50) { index ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(24.dp) },
                            effects = {
                                vibrancy()
                                liquidLens(16.dp.toPx(), 16.dp.toPx(), depthEffect = true)
                            },
                            onDrawSurface = { 
                                this.drawRect(Color.White.copy(0.2f)) 
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("Item $index", style = TextStyle(Color.White, 18.sp))
                }
            }
        }
    }
}
