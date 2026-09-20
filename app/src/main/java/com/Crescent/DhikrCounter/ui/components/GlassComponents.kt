package com.Crescent.DhikrCounter.ui.components

import android.os.VibrationEffect
import android.os.Build
import android.view.ViewGroup
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.ui.components.catalog.utils.scale
import com.matrix.prismal.PrismalFrameLayout
import com.matrix.prismal.PrismalLiquidGlass
import com.matrix.prismal.DownsampleMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

/**
 * PRISMAL STABILIZATION PASS
 * ARCHITECTURAL SINGLE-SCENE RENDERING & ADAPTIVE LUMINANCE.
 */

val LocalPrismalAdaptiveColor = compositionLocalOf { Color.White }
val LocalPrismalCaptureHost = compositionLocalOf<ViewGroup?> { null }
val LocalPrismalSceneVersion = compositionLocalOf { 0L }
val LocalAdaptiveLuminanceActive = compositionLocalOf { false }
val LocalAdaptiveLuminanceEnabled = compositionLocalOf { true }
val LocalAdaptiveLuminanceInterval = compositionLocalOf { 1000 }
val LocalGlassIntensity = compositionLocalOf { 1.0f }
val LocalHapticIntensity = compositionLocalOf { 1.0f }
val LocalHapticEnabled = compositionLocalOf { true }

val LocalFontTintFallbackMode = compositionLocalOf { 0 }
val LocalFontTintPaletteColor = compositionLocalOf { 0xFF6366F1.toInt() }
val LocalFontTintCustomColor = compositionLocalOf { 0xFF6366F1.toInt() }

/**
 * Perform haptic feedback with global intensity scaling.
 */
@Composable
fun rememberPrismalHaptic(): () -> Unit {
    val context = LocalContext.current
    val hapticManager = remember(context) {
        (context.applicationContext as DhikrApplication).hapticManager
    }
    val intensity = LocalHapticIntensity.current
    val enabled = LocalHapticEnabled.current

    return remember(hapticManager, intensity, enabled) {
        {
            if (enabled) {
                hapticManager.vibrate(50L, intensity)
            }
        }
    }
}

/**
 * ADAPTIVE ICON
 * Swaps between light and dark variants based on backdrop luminance.
 * Reuse the same throttled luminance pipeline from AdaptiveLuminanceProvider.
 */
@Composable
fun AdaptiveIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    darkVariant: ImageVector = icon,
    contentDescription: String? = null,
    tint: Color = LocalPrismalAdaptiveColor.current
) {
    val isBackdropLight = tint.luminance() < 0.5f
    val targetIcon = if (isBackdropLight) darkVariant else icon

    Icon(
        imageVector = targetIcon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

/**
 * ADAPTIVE LUMINANCE PROVIDER
 * Samples backdrop pixels in real-time to compute optimal text contrast.
 * Matches implementation from AdaptiveLuminanceGlassContent.kt.
 * 
 * PERFORMANCE REFACTOR (Pass 22):
 * - Throttled updates (30Hz while moving, paused when static).
 * - Avoids nested samplers via LocalAdaptiveLuminanceActive.
 */
@Composable
fun AdaptiveLuminanceProvider(
    layer: GraphicsLayer?,
    enabled: Boolean = true, // Kept for API compatibility but ignored in favor of LocalAdaptiveLuminanceEnabled
    content: @Composable () -> Unit
) {
    if (LocalAdaptiveLuminanceActive.current) {
        content()
        return
    }

    val globalEnabled = LocalAdaptiveLuminanceEnabled.current
    val context = LocalContext.current
    val settings = (context.applicationContext as DhikrApplication).settingsManager

    val isLightTheme = !isSystemInDarkTheme()
    
    // Fallback settings observed via CompositionLocal
    val fallbackMode = LocalFontTintFallbackMode.current
    val paletteColor = LocalFontTintPaletteColor.current
    val customColor = LocalFontTintCustomColor.current
    val sceneVersion = LocalPrismalSceneVersion.current

    val contentColorAnimation = remember(isLightTheme, globalEnabled, fallbackMode, paletteColor, customColor) {
        val initialColor = if (globalEnabled) {
            if (isLightTheme) Color(0xFF1A1C1E) else Color.White
        } else {
            when (fallbackMode) {
                0 -> {
                    val auto = settings.getFontTintAutoColor()
                    if (auto != -1) Color(auto) else (if (isLightTheme) Color(0xFF1A1C1E) else Color.White)
                }
                1 -> Color(paletteColor)
                2 -> Color(customColor)
                else -> if (isLightTheme) Color(0xFF1A1C1E) else Color.White
            }
        }
        Animatable(initialColor)
    }

    var lastPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Unspecified) }
    val refreshInterval = LocalAdaptiveLuminanceInterval.current
    
    // PERFORMANCE: Hoist buffer and lifecycle observation to avoid per-run allocation and unnecessary work
    val buffer = remember { IntArray(25) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isAppVisible by remember { mutableStateOf(true) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isAppVisible = event == androidx.lifecycle.Lifecycle.Event.ON_RESUME || event == androidx.lifecycle.Lifecycle.Event.ON_START
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(layer, globalEnabled, fallbackMode, paletteColor, customColor, sceneVersion, lastPosition, isAppVisible) {
        if (globalEnabled && layer != null) {
            if (isAppVisible) {
                // DEBOUNCE: Only recalculate after motion settles (Pass 30)
                delay(refreshInterval.toLong())

                try {
                    val imageBitmap = layer.toImageBitmap()
                    val thumbnail = imageBitmap.scale(5, 5)
                    thumbnail.readPixels(buffer)
                    
                    // PERFORMANCE: Optimized loop for luminance calculation
                    var luminanceSum = 0.0
                    for (argb in buffer) {
                        val r = (argb shr 16 and 0xFF) / 255f
                        val g = (argb shr 8 and 0xFF) / 255f
                        val b = (argb and 0xFF) / 255f
                        luminanceSum += 0.2126 * r + 0.7152 * g + 0.0722 * b
                    }
                    val averageLuminance = (luminanceSum / buffer.size).toFloat()

                    val targetColor = if (averageLuminance > 0.45f) Color(0xFF1A1C1E) else Color.White
                    if (contentColorAnimation.targetValue != targetColor) {
                        contentColorAnimation.animateTo(
                            targetColor,
                            tween(600, easing = LinearOutSlowInEasing)
                        )
                    }
                } catch (e: Exception) {
                    // Layer might not be ready or disposed
                }
            }
        } else {
            // FALLBACK PATH (Adaptive Luminance OFF)
            if (fallbackMode == 0 && layer != null) { // AUTO
                try {
                    delay(500) // Initial capture delay
                    val imageBitmap = layer.toImageBitmap()
                    val thumbnail = imageBitmap.scale(5, 5)
                    // PERFORMANCE: Reuse hoisted buffer
                    thumbnail.readPixels(buffer)
                    
                    var rSum = 0f; var gSum = 0f; var bSum = 0f
                    buffer.forEach { argb ->
                        rSum += (argb shr 16 and 0xFF) / 255f
                        gSum += (argb shr 8 and 0xFF) / 255f
                        bSum += (argb and 0xFF) / 255f
                    }
                    val avgColor = Color(rSum / 25, gSum / 25, bSum / 25)
                    val avgLuminance = 0.2126f * (rSum / 25) + 0.7152f * (gSum / 25) + 0.0722f * (bSum / 25)
                    
                    val result = deriveReadableColor(avgColor, avgLuminance)
                    contentColorAnimation.animateTo(result, tween(400))
                    settings.setFontTintAutoColor(result.toArgb())
                } catch (e: Exception) {}
            } else {
                val baseColor = if (fallbackMode == 1) Color(paletteColor)
                               else Color(customColor)
                
                if (layer != null) {
                    try {
                        val imageBitmap = layer.toImageBitmap()
                        val thumbnail = imageBitmap.scale(1, 1)
                        // PERFORMANCE: Reuse hoisted buffer
                        thumbnail.readPixels(buffer)
                        val argb = buffer[0]
                        val bgLuminance = (0.2126f * (argb shr 16 and 0xFF) + 0.7152f * (argb shr 8 and 0xFF) + 0.0722f * (argb and 0xFF)) / 255f
                        contentColorAnimation.animateTo(ensureContrast(baseColor, bgLuminance), tween(200))
                    } catch (e: Exception) {
                        contentColorAnimation.animateTo(baseColor, tween(200))
                    }
                } else {
                    contentColorAnimation.animateTo(baseColor, tween(200))
                }
            }
        }
    }

    Box(modifier = Modifier.onGloballyPositioned { coords ->
        if (globalEnabled) {
            val currentPos = coords.localToWindow(androidx.compose.ui.geometry.Offset.Zero)
            lastPosition = currentPos
        }
    }) {
        CompositionLocalProvider(
            LocalPrismalAdaptiveColor provides contentColorAnimation.value,
            LocalAdaptiveLuminanceActive provides true
        ) {
            content()
        }
    }
}

private fun deriveReadableColor(averageColor: Color, backgroundLuminance: Float): Color {
    val isDarkBackground = backgroundLuminance < 0.5f
    return if (isDarkBackground) {
        if (averageColor.luminance() < 0.6f) lerp(averageColor, Color.White, 0.7f) else averageColor
    } else {
        if (averageColor.luminance() > 0.4f) lerp(averageColor, Color.Black, 0.7f) else averageColor
    }
}

private fun ensureContrast(color: Color, backgroundLuminance: Float): Color {
    val isDarkBackground = backgroundLuminance < 0.5f
    return if (isDarkBackground) {
        if (color.luminance() < 0.5f) lerp(color, Color.White, 0.7f) else color
    } else {
        if (color.luminance() > 0.5f) lerp(color, Color.Black, 0.7f) else color
    }
}

data class GlassSettings(
    val blurRadius: Float,
    val cornerRadius: Float,
    val refractionHeight: Float,
    val refractionAmount: Float,
    val chromaticAberration: Float
)

data class WavySettings(
    val isEnabled: Boolean,
    val thickness: Float,
    val trackThickness: Float,
    val amplitude: Float,
    val wavelength: Float,
    val gapSize: Float,
    val waveSpeed: Float,
    val waveSpeedAuto: Boolean,
    val color: Int,
    val trackColor: Int
)

val LocalGlassSettings = staticCompositionLocalOf {
    GlassSettings(
        blurRadius = 28f,
        cornerRadius = 28f,
        refractionHeight = 12f,
        refractionAmount = 24f,
        chromaticAberration = 0f
    )
}

val LocalWavySettings = staticCompositionLocalOf {
    WavySettings(
        isEnabled = false,
        thickness = 8f,
        trackThickness = 8f,
        amplitude = 1.0f,
        wavelength = 20f,
        gapSize = 4f,
        waveSpeed = 20f,
        waveSpeedAuto = true,
        color = 0,
        trackColor = 0
    )
}

data class DockSettings(
    val blurRadius: Float,
    val cornerRadius: Float,
    val refractionHeight: Float,
    val refractionAmount: Float,
    val chromaticAberration: Float
)

val LocalDockSettings = staticCompositionLocalOf {
    DockSettings(
        blurRadius = 8f,
        cornerRadius = 32f,
        refractionHeight = 24f,
        refractionAmount = 24f,
        chromaticAberration = 0.01f
    )
}

/**
 * PRISMAL SCENE PROVIDER
 * Ensures all glass components share ONE physical environment.
 * Uses PrismalScene.getOrCreate() for proper shared hierarchical capture.
 */
@Composable
fun PrismalScene(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val localView = LocalView.current
    val host = remember(localView) { localView.rootView as? ViewGroup }

    // RENDER STABILITY: No more frame-by-frame heartbeat loop.
    // Prismal handles invalidation natively based on content change.

    CompositionLocalProvider(
        LocalPrismalCaptureHost provides host,
        LocalPrismalSceneVersion provides 0L // Kept for API compatibility, value doesn't trigger loop
    ) {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
fun PrismalSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    tonalColor: Color? = null,
    thickness: Float? = null,
    blurRadius: Float? = null,
    onClick: (() -> Unit)? = null,
    adaptiveLuminance: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = (context.applicationContext as DhikrApplication).settingsManager
    val density = LocalDensity.current
    val captureHost = LocalPrismalCaptureHost.current
    val isDark = isSystemInDarkTheme()

    // PRISMAL FIX PASS 4: RENDER STABILITY
    // Removed animateFloatAsState overrides for brightness/rim/specular to favor native theme adaptation
    val brightness = if (isDark) 1.08f else 1.12f
    val rim = if (isDark) 0.75f else 0.95f
    val specular = if (isDark) 0.9f else 1.15f

    val cornerRadiusPx = remember(shape, density) {
        if (shape is RoundedCornerShape) {
            shape.topStart.toPx(androidx.compose.ui.geometry.Size(1000f, 1000f), density)
        } else {
            0f
        }
    }

    val isLuminanceRoot = !LocalAdaptiveLuminanceActive.current
    val layer = if (isLuminanceRoot) rememberGraphicsLayer() else null
    val backdrop = com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop.current
    val glassIntensity = LocalGlassIntensity.current

    // PERFORMANCE: Lifecycle awareness to pause rendering when backgrounded
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isAppVisible by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isAppVisible = event == androidx.lifecycle.Lifecycle.Event.ON_RESUME || event == androidx.lifecycle.Lifecycle.Event.ON_START
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AdaptiveLuminanceProvider(layer = layer) {
        Box(
            modifier = modifier.drawBehind {
                if (backdrop != null && isLuminanceRoot && isAppVisible) {
                    layer?.record(
                        density = this,
                        layoutDirection = layoutDirection,
                        size = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
                    ) {
                        with(backdrop) {
                            drawBackdrop(this@drawBehind, null, null)
                        }
                        tonalColor?.let { drawRect(it) }
                    }
                }
            },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.matchParentSize(),
                factory = { ctx ->
                    PrismalFrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setCaptureHost(captureHost)
                        PrismalLiquidGlass.applyBase(this)
                        applyCalibratedSettings(this, settings, density, brightness, rim, specular, glassIntensity)
                        thickness?.let { setThickness(density.run { it.dp.toPx() }) }
                        blurRadius?.let { setBlurRadius(density.run { it.dp.toPx() }) }
                        tonalColor?.let { setGlassColor(it.toArgb()) }
                        setCornerRadius(cornerRadiusPx)
                        if (onClick != null) { setOnClickWithAnimationListener { onClick() } }
                    }
                },
                update = { view ->
                    // FIX: No more forced updateBackground() on every frame.
                    // Let Prismal handle redraws only when captureHost content changes.
                    view.setCaptureHost(captureHost)
                    applyCalibratedSettings(view, settings, density, brightness, rim, specular, glassIntensity)
                    thickness?.let { view.setThickness(density.run { it.dp.toPx() }) }
                    blurRadius?.let { view.setBlurRadius(density.run { it.dp.toPx() }) }
                    tonalColor?.let { view.setGlassColor(it.toArgb()) }
                    view.setCornerRadius(cornerRadiusPx)
                    if (onClick != null) {
                        view.setOnClickWithAnimationListener { onClick() }
                    } else {
                        view.setOnClickListener(null)
                        view.isClickable = false
                    }
                }
            )
            content()
        }
    }
}

@Composable
fun PrismalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonalColor: Color? = null,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    PrismalSurface(
        modifier = modifier,
        shape = shape,
        tonalColor = tonalColor,
        thickness = 5f, // Issue 8: Buttons 5dp
        onClick = onClick
    ) {
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
            content()
        }
    }
}

@Composable
fun PrismalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonalColor: Color? = null,
    content: @Composable () -> Unit
) {
    PrismalSurface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        tonalColor = tonalColor,
        thickness = 5f,
        blurRadius = 2.5f,
        onClick = onClick
    ) {
        content()
    }
}

@Composable
fun PrismalCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    tonalColor: Color? = null,
    content: @Composable () -> Unit
) {
    PrismalSurface(
        modifier = modifier,
        shape = shape,
        tonalColor = tonalColor,
        thickness = 18f, // Issue 8: Large Cards 18dp
        content = content
    )
}

@Composable
fun PrismalChip(
    modifier: Modifier = Modifier,
    tonalColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    PrismalSurface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalColor = tonalColor,
        thickness = 6f,
        blurRadius = 2.5f,
        onClick = onClick,
        content = content
    )
}

@Composable
fun PrismalDialog(
    onDismissRequest: () -> Unit,
    tonalColor: Color? = null,
    title: String = "",
    message: String = "",
    positiveText: String = "Confirm",
    negativeText: String? = "Cancel",
    onPositive: () -> Unit = {},
    isDestructive: Boolean = false,
    icon: Int? = null,
    iconColor: Color = Color.White,
    iconCompose: @Composable (() -> Unit)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val settings = (context.applicationContext as DhikrApplication).settingsManager
    val captureHost = LocalPrismalCaptureHost.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        var showDialog by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { showDialog = true }

        val scale by animateFloatAsState(
            targetValue = if (showDialog) 1f else 0.96f,
            animationSpec = spring(
                dampingRatio = settings.getGlassSpringDamping(),
                stiffness = settings.getGlassSpringStiffness()
            ),
            label = "DialogScale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (showDialog) 1f else 0f,
            animationSpec = tween(280),
            label = "DialogAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * alpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            val sizeDetails = LocalAppWindowSizeDetails.current
            val dialogWidth = when (sizeDetails.widthClass) {
                AppWindowWidthSizeClass.COMPACT -> sizeDetails.widthDp.dp * 0.92f
                AppWindowWidthSizeClass.MEDIUM -> 340.dp
                AppWindowWidthSizeClass.EXPANDED -> 460.dp
            }

            CompositionLocalProvider(LocalPrismalCaptureHost provides captureHost) {
                PrismalSurface(
                    modifier = Modifier
                        .width(dialogWidth)
                        .padding(horizontal = if (sizeDetails.widthClass == AppWindowWidthSizeClass.COMPACT) 8.dp else 24.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            alpha = alpha
                        )
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(38.dp),
                    tonalColor = tonalColor ?: Color(0x1AFFFFFF),
                    thickness = 0.8f,
                    blurRadius = 18f
                ) {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = LocalPrismalAdaptiveColor.current
                        )

                        if (message.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                ),
                                color = LocalPrismalAdaptiveColor.current.copy(alpha = 0.7f)
                            )
                        }

                        if (content != null) {
                            Spacer(Modifier.height(20.dp))
                            content()
                        }

                        Spacer(Modifier.height(28.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (negativeText != null) {
                                PrismalDialogButton(
                                    text = negativeText,
                                    onClick = onDismissRequest,
                                    modifier = Modifier.weight(1f),
                                    tonalColor = Color.White.copy(alpha = 0.08f),
                                    contentColor = LocalPrismalAdaptiveColor.current
                                )
                            }

                            val posColor = if (isDestructive) Color.Transparent else MaterialTheme.colorScheme.primary
                            val posTextColor = if (isDestructive) Color(0xFFFF5252) else (if (posColor.luminance() > 0.5f) Color.Black else Color.White)

                            PrismalDialogButton(
                                text = positiveText,
                                onClick = {
                                    onPositive()
                                    onDismissRequest()
                                },
                                modifier = Modifier.weight(1f),
                                tonalColor = posColor,
                                contentColor = posTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrismalDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tonalColor: Color? = null,
    contentColor: Color? = null
) {
    PrismalSurface(
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        tonalColor = tonalColor,
        onClick = onClick,
        thickness = 1.0f,
        blurRadius = 6f
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = contentColor ?: LocalPrismalAdaptiveColor.current
            )
        }
    }
}

@Composable
fun ColoredIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}


private fun applyCalibratedSettings(
    view: PrismalFrameLayout,
    settings: com.Crescent.DhikrCounter.utils.SettingsManager,
    density: androidx.compose.ui.unit.Density,
    brightness: Float,
    rim: Float,
    specular: Float,
    intensityScale: Float = 1.0f
) {
    // ISSUE 8 - PRISMAL CALIBRATED BASELINE (Matched to Reference Image)
    val baseTint = settings.getGlassTintColor()
    val tintAlpha = (baseTint shr 24 and 0xFF) * intensityScale
    val tintWithScaledAlpha = (tintAlpha.toInt() shl 24) or (baseTint and 0x00FFFFFF)
    view.setGlassColor(tintWithScaledAlpha)
    
    view.setIOR(settings.getGlassIOR())
    
    // Reference Image: Blur Radius 3.0 dp, Corner Radius 24 dp
    val blurRadius = settings.getGlassBlurRadius() * intensityScale
    view.setBlurRadius(density.run { blurRadius.dp.toPx() })
    
    view.setNormalStrength(settings.getGlassNormalStrength())
    view.setThickness(density.run { settings.getGlassThickness().dp.toPx() })
    view.setDisplacementScale(settings.getGlassDisplacementScale())
    
    // ISSUE 6 - ADAPTIVE THEME DERIVATION (ANIMATED)
    // Dark mode: reduce base brightness/specular slightly per Prismal guidance
    // Brightness is stored as 0.0-1.0 (representing 0-100% on slider)
    view.setBrightness(settings.getGlassBrightness() * 1.5f * intensityScale)
    view.setRimStrength(rim * intensityScale)
    view.setSpecular(specular * intensityScale, settings.getGlassShininess())
    
    view.setHighlightWidth(settings.getGlassHighlightWidth())
    view.setCausticIntensity(settings.getGlassCausticIntensity())
    view.setLiquidDomeStrength(settings.getGlassLiquidDome())
    view.setFresnelReflectStrength(1.0f) // Issue 8: Fresnel Reflection 1.0
    
    view.setLensRefractionScale(settings.getGlassRefractionAmount() / 24f) // Normalize to default 24
    view.setLightDirection(settings.getGlassLightDirX(), settings.getGlassLightDirY())
    view.setTransmittance(settings.getGlassTransmittance())
    view.setMinSmoothing(settings.getGlassMinSmoothing())
    
    // ISSUE 7/12: Eliminate Native Rendering.
    view.setChromaticAberration(settings.getGlassChromaticAberration() * intensityScale)
    
    val shadowColor = settings.getGlassShadowColor()
    val alpha = (settings.getGlassShadowIntensity() * 255).toInt().coerceIn(0, 255)
    val colorWithAlpha = (alpha shl 24) or (shadowColor and 0x00FFFFFF)
    view.setShadowProperties(colorWithAlpha, settings.getGlassShadowSoftness())
    
    val mode = when (settings.getGlassCaptureDownsample()?.lowercase()) {
        "off" -> DownsampleMode.OFF
        "subtle" -> DownsampleMode.SUBTLE
        "balanced" -> DownsampleMode.BALANCED
        "aggressive" -> DownsampleMode.AGGRESSIVE
        else -> DownsampleMode.BALANCED
    }
    view.setCaptureDownsample(mode)
    view.setClickAnimationPressScale(0.97f)
}

enum class AppWindowWidthSizeClass { COMPACT, MEDIUM, EXPANDED }
enum class AppWindowHeightSizeClass { COMPACT, MEDIUM, EXPANDED }

data class AppWindowSizeDetails(
    val widthClass: AppWindowWidthSizeClass,
    val heightClass: AppWindowHeightSizeClass,
    val isLandscape: Boolean,
    val widthDp: Int,
    val heightDp: Int
)

val LocalAppWindowSizeDetails = staticCompositionLocalOf {
    AppWindowSizeDetails(
        widthClass = AppWindowWidthSizeClass.MEDIUM,
        heightClass = AppWindowHeightSizeClass.MEDIUM,
        isLandscape = false,
        widthDp = 360,
        heightDp = 640
    )
}

