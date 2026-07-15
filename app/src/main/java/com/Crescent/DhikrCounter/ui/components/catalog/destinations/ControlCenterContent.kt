package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.BackdropEffectScope
import com.Crescent.DhikrCounter.ui.components.catalog.BackdropDemoScaffold
import com.Crescent.DhikrCounter.ui.components.catalog.FlightIcon
import com.Crescent.DhikrCounter.ui.components.catalog.utils.ProgressConverter
import com.Crescent.DhikrCounter.ui.components.catalog.utils.rememberUISensor
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.shapes.Capsule

@Composable
fun ControlCenterContent() {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val containerColor =
        if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
        else Color(0xFF121212).copy(0.4f)

    val itemShape = MaterialTheme.shapes.extraLarge
    val innerItemShape = Capsule()

    val sensor = rememberUISensor()

    BackdropDemoScaffold { backdrop ->
        Column(
            Modifier
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { itemShape },
                            effects = {
                                vibrancy()
                                liquidLens(
                                    24f.dp.toPx(),
                                    24f.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                            }
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(Color(0xFF0088FF), innerItemShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(FlightIcon, null, tint = Color.White)
                        }
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(containerColor, innerItemShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.WifiOff, null, tint = contentColor)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(Color(0xFF0088FF), innerItemShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Wifi, null, tint = Color.White)
                        }
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(Color(0xFF0088FF), innerItemShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Bluetooth, null, tint = Color.White)
                        }
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { itemShape },
                            effects = {
                                vibrancy()
                                liquidLens(
                                    24f.dp.toPx(),
                                    24f.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                            }
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .background(Color(0xFFFFCC00), innerItemShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.MusicNote, null, tint = Color.White)
                        }
                        Column {
                            Text(
                                "Music",
                                color = contentColor,
                                fontSize = 16f.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Not Playing",
                                color = contentColor.copy(0.6f),
                                fontSize = 12f.sp
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.height(160.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var brightness by rememberSaveable { mutableFloatStateOf(0.5f) }
                var volume by rememberSaveable { mutableFloatStateOf(0.5f) }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { itemShape },
                            effects = {
                                vibrancy()
                                liquidLens(
                                    24f.dp.toPx(),
                                    24f.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                                drawRect(
                                    Color.White.copy(0.3f),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * (1f - brightness)),
                                    size = androidx.compose.ui.geometry.Size(size.width, size.height * brightness)
                                )
                            }
                        )
                ) {
                    Icon(
                        Icons.Outlined.LightMode,
                        null,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        tint = contentColor
                    )
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { itemShape },
                            effects = {
                                vibrancy()
                                liquidLens(
                                    24f.dp.toPx(),
                                    24f.dp.toPx(),
                                    depthEffect = true
                                )
                            },
                            onDrawSurface = {
                                drawRect(containerColor)
                                drawRect(
                                    Color.White.copy(0.3f),
                                    topLeft = androidx.compose.ui.geometry.Offset(0f, size.height * (1f - volume)),
                                    size = androidx.compose.ui.geometry.Size(size.width, size.height * volume)
                                )
                            }
                        )
                ) {
                    Icon(
                        Icons.Outlined.VolumeUp,
                        null,
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        tint = contentColor
                    )
                }
            }
        }
    }
}
