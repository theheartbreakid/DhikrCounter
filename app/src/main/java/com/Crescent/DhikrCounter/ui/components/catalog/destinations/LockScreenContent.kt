package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.ui.components.catalog.BackdropDemoScaffold
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.shapes.RoundedRectangle

@Composable
fun LockScreenContent() {
    BackdropDemoScaffold { backdrop ->
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                "12:34",
                style = TextStyle(Color.White, 80.sp, FontWeight.Bold)
            )
            BasicText(
                "Monday, July 6",
                style = TextStyle(Color.White, 20.sp)
            )
            
            Spacer(Modifier.weight(1f))
            
            Row(
                Modifier.fillMaxWidth().padding(bottom = 48.dp, start = 48.dp, end = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .drawPlainBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(32.dp) },
                            effects = {
                                blur(32.dp.toPx())
                                colorControls(brightness = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Flashlight icon
                }
                
                Box(
                    Modifier
                        .size(64.dp)
                        .drawPlainBackdrop(
                            backdrop = backdrop,
                            shape = { RoundedRectangle(32.dp) },
                            effects = {
                                blur(32.dp.toPx())
                                colorControls(brightness = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Camera icon
                }
            }
        }
    }
}
