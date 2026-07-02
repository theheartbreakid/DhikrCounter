package com.Crescent.DhikrCounter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.drawPlainBackdrop
import com.kashif_e.backdrop.effects.blur

@Composable
fun GlassSurface(
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    blurRadius: Dp = 26.dp,
    tonalColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val blurPx = remember(blurRadius, density) { with(density) { blurRadius.toPx() } }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val overlayAlpha = if (isDark) 0.10f else 0.15f

    val shape = remember(cornerRadius) { androidx.compose.foundation.shape.RoundedCornerShape(cornerRadius) }
    val borderColor = Color.White.copy(alpha = 0.06f)
    val highlight = remember {
        Brush.verticalGradient(
            0.0f to Color.White.copy(alpha = 0.05f),
            1.0f to Color.Transparent
        )
    }

    Box(modifier = modifier.clip(shape)) {
        // Layer 1: backdrop-only blur (samples pixels BEHIND this component).
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawPlainBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = { blur(radius = blurPx) }
                )
        )

        // Layer 2: translucent surface tint (NOT blurred).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color = tonalColor.copy(alpha = overlayAlpha), shape = shape)
        )

        // Border (sharp).
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 1.dp, color = borderColor, shape = shape)
        )

        // Top highlight (sharp).
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(highlight, shape = shape)
        )

        // Foreground content (always sharp).
        Box(modifier = Modifier.padding(0.dp)) {
            content()
        }
    }
}

@Composable
fun GlassDialog(
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    blurRadius: Dp = 26.dp,
    tonalColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: Dp = 20.dp,
    properties: DialogProperties = DialogProperties(),
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        GlassSurface(
            backdrop = backdrop,
            modifier = modifier,
            cornerRadius = cornerRadius,
            blurRadius = blurRadius,
            tonalColor = tonalColor
        ) {
            Box(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
