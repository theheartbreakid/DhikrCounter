package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.Crescent.DhikrCounter.ui.components.catalog.BackdropDemoScaffold
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch

@Composable
fun MagnifierContent() {
    val animationScope = rememberCoroutineScope()
    val offsetAnimation = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    val magnifierBackdrop = rememberLayerBackdrop()

    BackdropDemoScaffold { backdrop ->
        Box(
            Modifier
                .offset {
                    val offset = offsetAnimation.value
                    IntOffset(offset.x.fastRoundToInt(), offset.y.fastRoundToInt())
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        animationScope.launch {
                            offsetAnimation.snapTo(offsetAnimation.value + dragAmount)
                        }
                    }
                }
                .layerBackdrop(magnifierBackdrop)
                .drawBackdrop(
                    backdrop = rememberCombinedBackdrop(backdrop, magnifierBackdrop),
                    shape = { Capsule() },
                    effects = {
                        blur(2f.dp.toPx())
                        liquidLens(32f.dp.toPx(), 48f.dp.toPx(), depthEffect = true)
                    },
                    highlight = { Highlight.Ambient },
                    shadow = { Shadow.Default },
                    onDrawSurface = { drawRect(Color.White.copy(0.1f)) }
                )
                .size(160f.dp, 120f.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(8f.dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { Capsule() },
                        effects = { liquidLens(8f.dp.toPx(), 8f.dp.toPx()) },
                        onDrawSurface = { drawRect(Color.White) }
                    )
            )
        }
    }
}
