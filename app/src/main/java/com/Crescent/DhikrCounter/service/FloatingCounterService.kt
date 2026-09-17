package com.Crescent.DhikrCounter.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.WindowManager.LayoutParams
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.lerp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.MainActivity
import com.Crescent.DhikrCounter.R
import com.Crescent.DhikrCounter.ui.theme.DhikrTheme
import com.Crescent.DhikrCounter.data.SessionEntity
import com.Crescent.DhikrCounter.data.SessionRepository
import com.Crescent.DhikrCounter.data.HistoryRepository
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.utils.SoundManager
import com.Crescent.DhikrCounter.ui.widgets.updateAllWidgets
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidIconButton
import com.Crescent.DhikrCounter.ui.components.catalog.utils.InteractiveHighlight
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.liquidLens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.Crescent.DhikrCounter.ui.components.AdaptiveLuminanceProvider
import com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor
import com.Crescent.DhikrCounter.ui.components.AdaptiveIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.*

class FloatingCounterService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        private const val CHANNEL_ID = "floating_counter_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var bubbleParams: LayoutParams? = null
    
    private var dismissView: ComposeView? = null
    private var dismissParams: LayoutParams? = null

    private lateinit var repository: SessionRepository
    private lateinit var historyRepository: HistoryRepository
    private lateinit var settingsManager: SettingsManager
    private lateinit var soundManager: SoundManager
    private lateinit var hapticManager: com.Crescent.DhikrCounter.utils.HapticManager
    private lateinit var usageSessionManager: com.Crescent.DhikrCounter.data.UsageSessionManager
    
    private var activeSession by mutableStateOf<SessionEntity?>(null)
    private var isExpanded by mutableStateOf(false)
    private var isDragging by mutableStateOf(false)
    private var isNearDismiss by mutableStateOf(false)
    private var isDismissedViaDrag = false
    private var cumulativeDragDistance = 0f

    private val displaySize = Point()
    
    private var bubbleX by mutableFloatStateOf(0f)
    private var bubbleY by mutableFloatStateOf(0f)
    
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var initialBubbleX = 0f
    private var initialBubbleY = 0f
    private var isDraggingLocal = false

    private val _savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = _savedStateRegistryController.savedStateRegistry

    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
        when (key) {
            "active_session_id" -> observeActiveSession()
        }
    }

    override fun onCreate() {
        super.onCreate()
        _savedStateRegistryController.performRestore(null)

        createNotificationChannel()
        try {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        initializeComponents()
        setupFloatingBubble()
        observeActiveSession()
    }

    private fun initializeComponents() {
        val app = application as DhikrApplication
        repository = app.sessionRepository
        historyRepository = app.historyRepository
        settingsManager = app.settingsManager
        soundManager = app.soundManager
        hapticManager = app.hapticManager
        usageSessionManager = app.usageSessionManager

        settingsManager.prefs.registerOnSharedPreferenceChangeListener(prefListener)
        
        soundManager.playSound(SoundManager.SoundType.FLOATING_POPUP)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        updateDisplaySize()
        
        val xRatio = settingsManager.prefs.getFloat("bubble_last_x_ratio", 1f)
        val yRatio = settingsManager.prefs.getFloat("bubble_last_y_ratio", 0.5f)
        bubbleX = xRatio * displaySize.x
        bubbleY = yRatio * displaySize.y
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val oldXRatio = if (displaySize.x > 0) bubbleX / displaySize.x else 1f
        val oldYRatio = if (displaySize.y > 0) bubbleY / displaySize.y else 0.5f
        
        super.onConfigurationChanged(newConfig)
        updateDisplaySize()
        
        bubbleX = oldXRatio * displaySize.x
        bubbleY = oldYRatio * displaySize.y
        
        snapToEdge()
    }

    private fun updateDisplaySize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            displaySize.set(metrics.bounds.width(), metrics.bounds.height())
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(displaySize)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingBubble() {
        val density = resources.displayMetrics.density
        val baseSizePx = (64 * density).toInt()
        val bubbleScale = settingsManager.bubbleScale
        val bubbleSizePx = (baseSizePx * bubbleScale).toInt()
        val paddingPx = (64 * density).toInt() 
        val windowSizePx = bubbleSizePx + paddingPx * 2

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }

        val lp = LayoutParams(
            windowSizePx,
            windowSizePx,
            layoutFlag,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCH_MODAL or LayoutParams.FLAG_LAYOUT_IN_SCREEN or LayoutParams.FLAG_LAYOUT_NO_LIMITS or LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (bubbleX - windowSizePx / 2f).toInt()
        lp.y = (bubbleY - windowSizePx / 2f).toInt()
        
        bubbleParams = lp

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingCounterService)
            setViewTreeViewModelStoreOwner(this@FloatingCounterService)
            setViewTreeSavedStateRegistryOwner(this@FloatingCounterService)

            setContent {
                val prefs = remember { settingsManager.prefs }
                
                var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
                var amoledMode by remember { mutableStateOf(settingsManager.isAmoledModeEnabled) }

                // Observed Bubble-Specific Settings
                var bubbleDynamicColors by remember { mutableStateOf(settingsManager.isBubbleDynamicColorsEnabled) }
                var bubbleAdaptiveLuminance by remember { mutableStateOf(settingsManager.isBubbleAdaptiveLuminanceEnabled) }
                var adaptiveLuminanceInterval by remember { mutableIntStateOf(settingsManager.getAdaptiveLuminanceInterval()) }
                var bubbleTintStrength by remember { mutableFloatStateOf(settingsManager.getBubbleTintStrength()) }
                
                var hapticIntensity by remember { mutableFloatStateOf(settingsManager.getHapticIntensity()) }
                var hapticEnabled by remember { mutableStateOf(settingsManager.isHapticFeedbackEnabled) }
                
                // Bubble Specific Glass Settings
                var bubbleBlur by remember { mutableFloatStateOf(settingsManager.getBubbleBlurRadius()) }
                var bubbleCornerRadius by remember { mutableFloatStateOf(settingsManager.getBubbleCornerRadius()) }
                var bubbleRefractionHeight by remember { mutableFloatStateOf(settingsManager.getBubbleRefractionHeight()) }
                var bubbleRefractionAmount by remember { mutableFloatStateOf(settingsManager.getBubbleRefractionAmount()) }
                var bubbleChromaticAberration by remember { mutableFloatStateOf(settingsManager.getBubbleChromaticAberration()) }
                var bubbleVibrancy by remember { mutableFloatStateOf(settingsManager.getBubbleVibrancy()) }
                var bubbleAccentColor by remember { mutableIntStateOf(settingsManager.getBubbleAccentColor()) }

                var bubbleScale by remember { mutableFloatStateOf(settingsManager.getBubbleScale()) }
                var appFontFamily by remember { mutableStateOf(settingsManager.appFontFamily) }
                var appFontWeight by remember { mutableStateOf(settingsManager.appFontWeight) }

                DisposableEffect(prefs) {
                    val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                        when (key) {
                            "pref_theme_mode" -> themeMode = p.getString(key, "system") ?: "system"
                            "pref_amoled_mode" -> amoledMode = p.getBoolean(key, false)

                            "pref_bubble_dynamic_colors" -> bubbleDynamicColors = p.getBoolean(key, true)
                            "pref_bubble_adaptive_luminance" -> bubbleAdaptiveLuminance = p.getBoolean(key, true)
                            "pref_adaptive_luminance_interval" -> adaptiveLuminanceInterval = p.getInt(key, 1000)
                            "pref_bubble_tint_strength" -> bubbleTintStrength = p.getFloat(key, 0.75f)
                            
                            "pref_haptic_intensity" -> hapticIntensity = p.getFloat(key, 1.0f)
                            "pref_haptic_feedback" -> hapticEnabled = p.getBoolean(key, true)
                            
                            "pref_bubble_blur_radius" -> bubbleBlur = p.getFloat(key, 2.8f)
                            "pref_bubble_corner_radius" -> bubbleCornerRadius = p.getFloat(key, 28f)
                            "pref_bubble_refraction_height" -> bubbleRefractionHeight = p.getFloat(key, 12f)
                            "pref_bubble_refraction_amount" -> bubbleRefractionAmount = p.getFloat(key, 24f)
                            "pref_bubble_chromatic_aberration" -> bubbleChromaticAberration = p.getFloat(key, 0f)
                            "pref_bubble_vibrancy" -> bubbleVibrancy = p.getFloat(key, 1.0f)
                            "pref_bubble_accent_color" -> bubbleAccentColor = p.getInt(key, 0xFF2196F3.toInt())

                            "pref_bubble_scale" -> bubbleScale = p.getFloat(key, 1.0f)
                            "pref_app_font_family" -> appFontFamily = p.getString(key, "SF Pro Text") ?: "SF Pro Text"
                            "pref_app_font_weight" -> appFontWeight = p.getString(key, "Regular") ?: "Regular"
                        }
                    }
                    prefs.registerOnSharedPreferenceChangeListener(listener)
                    onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
                }

                val bubbleBaseSize = 64.dp
                val bubbleSizeDp = bubbleBaseSize * bubbleScale

                val isDark = when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemInDarkTheme()
                }

                DhikrTheme(
                    darkTheme = isDark,
                    dynamicColor = bubbleDynamicColors,
                    amoledMode = amoledMode,
                    appFontFamily = appFontFamily,
                    appFontWeight = appFontWeight
                ) {
                    val finalAccentColor = if (bubbleDynamicColors) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color(bubbleAccentColor)
                    }

                    LaunchedEffect(bubbleX, bubbleY, bubbleScale, isExpanded) {
                        val currentLp = bubbleParams ?: return@LaunchedEffect
                        
                        // Pass 36: Tighten touch bounds.
                        // When expanded, we need room for action buttons (approx 128dp total).
                        // When collapsed, the window should exactly match the bubble size.
                        val expandedPadding = if (isExpanded) (64 * density).toInt() else 0
                        val newSize = (bubbleBaseSize.value * bubbleScale * density).toInt() + expandedPadding * 2
                        
                        currentLp.width = newSize
                        currentLp.height = newSize
                        currentLp.x = (bubbleX - currentLp.width / 2f).toInt()
                        currentLp.y = (bubbleY - currentLp.height / 2f).toInt()

                        try { 
                            windowManager.updateViewLayout(this@apply, currentLp)
                            this@apply.invalidateOutline()
                        } catch (e: Exception) {}
                    }

                    CompositionLocalProvider(
                        com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceEnabled provides bubbleAdaptiveLuminance,
                        com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceInterval provides adaptiveLuminanceInterval,
                        com.Crescent.DhikrCounter.ui.components.LocalGlassIntensity provides 1.0f,
                        com.Crescent.DhikrCounter.ui.components.LocalHapticIntensity provides hapticIntensity,
                        com.Crescent.DhikrCounter.ui.components.LocalHapticEnabled provides hapticEnabled,
                        com.Crescent.DhikrCounter.ui.components.LocalGlassSettings provides com.Crescent.DhikrCounter.ui.components.GlassSettings(
                            blurRadius = bubbleBlur,
                            cornerRadius = bubbleCornerRadius,
                            refractionHeight = bubbleRefractionHeight,
                            refractionAmount = bubbleRefractionAmount,
                            chromaticAberration = bubbleChromaticAberration
                        )
                    ) {
                        BubbleWindowUI(
                            hapticManager = hapticManager,
                            count = activeSession?.count ?: 0L,
                            bubbleX = bubbleX,
                            bubbleY = bubbleY,
                            screenWidth = displaySize.x.toFloat(),
                            screenHeight = displaySize.y.toFloat(),
                            isExpanded = isExpanded,
                            onExpandedChange = { isExpanded = it },
                            isDragging = isDragging,
                            onDraggingChange = { isDragging = it },
                            isNearDismiss = isNearDismiss,
                            bubbleSize = bubbleSizeDp.value,
                            cornerRadius = bubbleCornerRadius,
                            opacity = 0.8f,
                            blurRadius = bubbleBlur,
                            refractionHeight = bubbleRefractionHeight,
                            refractionAmount = bubbleRefractionAmount,
                            chromaticAberration = bubbleChromaticAberration,
                            vibrancy = bubbleVibrancy,
                            scale = 1.0f,
                            accentColor = finalAccentColor,
                            tintStrength = bubbleTintStrength,
                            isDark = isDark,
                            onIncrement = { incrementCount() },
                            onDecrement = { decrementCount() },
                            onReset = { resetCount() },
                            onOpenApp = { openApp() },
                            onDragStart = { 
                                isDragging = true
                                cumulativeDragDistance = 0f
                                hapticManager.vibrateSubtle()
                                showDismissZone() 
                            },
                            onDrag = { delta ->
                                if (isExpanded) {
                                    cumulativeDragDistance += delta.getDistance()
                                    if (cumulativeDragDistance > 24 * density) {
                                        isExpanded = false
                                    }
                                }
                                bubbleX += delta.x
                                bubbleY += delta.y
                                checkDismissProximity()
                            },
                            onDragEnd = { handleDragEnd() }
                        )
                    }
                }
            }
        }
        bubbleView = view
        
        // Pass 36: Apply Outline to ComposeView for precise touch bounds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val size = view.width
                    if (size <= 0) return
                    val radius = if (isExpanded) 0f else (settingsManager.getBubbleCornerRadius() * density)
                    outline.setRoundRect(0, 0, size, size, radius.coerceAtMost(size / 2f))
                }
            }
            view.clipToOutline = true
        }

        windowManager.addView(view, lp)
    }

    private fun checkDismissProximity() {
        val density = resources.displayMetrics.density
        val bubbleSizePx = (64 * settingsManager.bubbleScale * density).toInt()
        val dismissSizePx = (140 * density).toInt()

        val dismissCenterX = displaySize.x / 2f
        val dismissYOffset = (60 * density) // HUD style
        val dismissCenterY = displaySize.y - dismissYOffset
        
        val dx = bubbleX - dismissCenterX
        val dy = bubbleY - dismissCenterY
        val dist = sqrt((dx * dx + dy * dy).toDouble())
        
        val captureRadius = (120 * density)
        val collisionDist = (60 * density)
        
        val wasNear = isNearDismiss
        isNearDismiss = dist < collisionDist
        
        if (isNearDismiss && !wasNear) {
            performHapticFeedback()
        }
        
        if (dist < captureRadius && isDragging) {
            val strength = ((captureRadius - dist) / (captureRadius)).coerceIn(0.0, 1.0)
            bubbleX = lerp(bubbleX, dismissCenterX, (strength * 0.15f).toFloat())
            bubbleY = lerp(bubbleY, dismissCenterY, (strength * 0.15f).toFloat())
        }
    }

    private fun handleDragEnd() {
        if (isNearDismiss) {
            val density = resources.displayMetrics.density
            val dismissCenterX = displaySize.x / 2f
            val dismissYOffset = (60 * density)
            val dismissCenterY = displaySize.y - dismissYOffset

            val animatorX = ValueAnimator.ofFloat(bubbleX, dismissCenterX)
            val animatorY = ValueAnimator.ofFloat(bubbleY, dismissCenterY)
            animatorX.duration = 300
            animatorY.duration = 300
            animatorX.interpolator = android.view.animation.AnticipateInterpolator()
            animatorY.interpolator = android.view.animation.AnticipateInterpolator()
            
            animatorX.addUpdateListener { bubbleX = it.animatedValue as Float }
            animatorY.addUpdateListener { bubbleY = it.animatedValue as Float }
            
            animatorX.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    performDismiss()
                }
            })
            animatorX.start()
            animatorY.start()
        } else {
            snapToEdge()
            hideDismissZone()
            savePosition()
        }
    }

    private fun performDismiss() {
        isDismissedViaDrag = true
        soundManager.playSound(SoundManager.SoundType.FLOATING_DISMISS)
        settingsManager.prefs.edit().putBoolean("pref_floating_enabled", false).apply()
        
        // Final contract animation for dismiss target is handled in DismissTargetUI visibility state
        lifecycleScope.launch {
            delay(300)
            stopSelf()
        }
    }

    private fun snapToEdge() {
        hapticManager.vibrateSubtle()
        val density = resources.displayMetrics.density
        val bubbleSizePx = (64 * settingsManager.bubbleScale * density).toInt()
        val margin = (1 * density).toInt()
        val targetX = if (bubbleX < displaySize.x / 2f) (bubbleSizePx / 2f + margin) else displaySize.x - (bubbleSizePx / 2f + margin)
        
        val animator = ValueAnimator.ofFloat(bubbleX, targetX)
        animator.duration = 450
        animator.interpolator = android.view.animation.OvershootInterpolator(1.2f)
        animator.addUpdateListener { anim ->
            bubbleX = anim.animatedValue as Float
        }
        animator.start()
    }

    private fun savePosition() {
        val xRatio = if (displaySize.x > 0) bubbleX / displaySize.x else 1f
        val yRatio = if (displaySize.y > 0) bubbleY / displaySize.y else 0.5f
        settingsManager.prefs.edit()
            .putFloat("bubble_last_x_ratio", xRatio)
            .putFloat("bubble_last_y_ratio", yRatio)
            .apply()
    }

    private fun showDismissZone() {
        if (dismissView != null) return
        
        val density = resources.displayMetrics.density
        val widthPx = (240 * density).toInt()
        val heightPx = (200 * density).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            LayoutParams.TYPE_PHONE
        }

        val lp = LayoutParams(
            widthPx,
            heightPx,
            layoutFlag,
            LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        lp.y = 0
        dismissParams = lp

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingCounterService)
            setViewTreeViewModelStoreOwner(this@FloatingCounterService)
            setViewTreeSavedStateRegistryOwner(this@FloatingCounterService)

            setContent {
                val bubbleBlur = settingsManager.getBubbleBlurRadius()
                val bubbleCornerRadius = settingsManager.getBubbleCornerRadius()
                val bubbleRefractionHeight = settingsManager.getBubbleRefractionHeight()
                val bubbleRefractionAmount = settingsManager.getBubbleRefractionAmount()
                val bubbleChromaticAberration = settingsManager.getBubbleChromaticAberration()
                val bubbleVibrancy = settingsManager.getBubbleVibrancy()
                
                val glassSettings = com.Crescent.DhikrCounter.ui.components.GlassSettings(
                    blurRadius = bubbleBlur,
                    cornerRadius = bubbleCornerRadius,
                    refractionHeight = bubbleRefractionHeight,
                    refractionAmount = bubbleRefractionAmount,
                    chromaticAberration = bubbleChromaticAberration
                )
                
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { isVisible = true }
                
                DismissTargetUI(
                    isNear = isNearDismiss,
                    isVisible = isVisible && !isDismissedViaDrag,
                    glassSettings = glassSettings,
                    vibrancy = bubbleVibrancy,
                    isDark = isSystemInDarkTheme()
                )
            }
        }
        dismissView = view
        windowManager.addView(view, lp)
    }

    private fun hideDismissZone() {
        dismissView?.let { 
            // In a real app we might want to wait for animation, but for simplicity:
            try { windowManager.removeView(it) } catch (e: Exception) {}
            dismissView = null
            dismissParams = null
        }
        isNearDismiss = false
    }

    private fun observeActiveSession() {
        lifecycleScope.launch {
            val sessionId = settingsManager.activeSessionId
            repository.getSessionFlow(sessionId).collectLatest { session ->
                if (activeSession == null && session != null) {
                    usageSessionManager.startSession(session.id, session.name, session.count)
                } else if (activeSession != null && session != null && activeSession?.id != session.id) {
                    usageSessionManager.endSession(activeSession?.count ?: 0L)
                    usageSessionManager.startSession(session.id, session.name, session.count)
                }
                activeSession = session
            }
        }
    }

    private fun incrementCount() {
        activeSession?.let { current ->
            lifecycleScope.launch {
                repository.incrementCount(current.id, current.incrementValue)
                // historyRepository.logEvent(current.id, current.name, "COUNT_CHANGED", current.incrementValue) // Removed
                updateAllWidgets(this@FloatingCounterService)
                
                val isGoalJustReached = current.goalCount > 0 && current.count + current.incrementValue >= current.goalCount && current.count < current.goalCount
                if (isGoalJustReached) {
                    usageSessionManager.updateGoalMet(true)
                }

                launch(Dispatchers.Main) {
                    performHapticFeedback()
                    if (isGoalJustReached) {
                        soundManager.playSound(SoundManager.SoundType.GOAL_REACHED)
                    } else {
                        soundManager.playSound(SoundManager.SoundType.INCREMENT)
                    }
                }
            }
        }
    }

    private fun decrementCount() {
        activeSession?.let { current ->
            lifecycleScope.launch {
                repository.decrementCount(current.id, current.incrementValue, settingsManager.isNegativeCountAllowed)
                // historyRepository.logEvent(current.id, current.name, "COUNT_CHANGED", -current.incrementValue) // Removed
                updateAllWidgets(this@FloatingCounterService)
                launch(Dispatchers.Main) {
                    performHapticFeedback()
                    soundManager.playSound(SoundManager.SoundType.DECREMENT)
                }
            }
        }
    }

    private fun resetCount() {
        if (settingsManager.isConfirmBeforeReset) {
            openApp(showResetDialog = true)
            if (isExpanded) isExpanded = false
            return
        }

        activeSession?.let { current ->
            lifecycleScope.launch {
                usageSessionManager.endSession(current.count)
                repository.resetCount(current.id)
                // historyRepository.logEvent(current.id, current.name, "RESET", -current.count) // Removed
                usageSessionManager.startSession(current.id, current.name, 0)
                updateAllWidgets(this@FloatingCounterService)
                launch(Dispatchers.Main) {
                    performHapticFeedback()
                    soundManager.playSound(SoundManager.SoundType.RESET)
                }
            }
        }
    }

    private fun performHapticFeedback() {
        hapticManager.vibrate()
    }

    private fun openApp(showResetDialog: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (showResetDialog) putExtra("show_reset_dialog", true)
        }
        startActivity(intent)
        if (isExpanded) isExpanded = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Floating Counter Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DhikrCounter++ Floating")
            .setContentText("Floating counter is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        activeSession?.let { usageSessionManager.endSession(it.count) }
        super.onDestroy()
        if (!isDismissedViaDrag) {
            soundManager.playSound(SoundManager.SoundType.FLOATING_DISMISS)
        }
        settingsManager.prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        dismissView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        _viewModelStore.clear()
    }
}

@Composable
fun BubbleWindowUI(
    hapticManager: com.Crescent.DhikrCounter.utils.HapticManager,
    count: Long,
    bubbleX: Float,
    bubbleY: Float,
    screenWidth: Float,
    screenHeight: Float,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isDragging: Boolean,
    onDraggingChange: (Boolean) -> Unit,
    isNearDismiss: Boolean,
    bubbleSize: Float,
    cornerRadius: Float,
    opacity: Float,
    blurRadius: Float,
    refractionHeight: Float,
    refractionAmount: Float,
    chromaticAberration: Float,
    vibrancy: Float,
    scale: Float,
    accentColor: Color,
    tintStrength: Float,
    isDark: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    onOpenApp: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current
    val bubbleSizePx = with(density) { (bubbleSize * scale).dp.toPx() }
    val animationScope = rememberCoroutineScope()

    val dragScale by animateFloatAsState(if (isDragging) 1.15f else 1f, label = "DragScale")
    val dismissScale by animateFloatAsState(if (isNearDismiss) 0.6f else 1f, label = "DismissScale")
    val totalScale = dragScale * dismissScale
    
    val currentOpacity by animateFloatAsState(if (isNearDismiss) 0.4f else opacity, label = "BubbleOpacity")

    val bubbleShape = remember(cornerRadius, bubbleSizePx) {
        val maxRadius = bubbleSizePx / 2f
        val currentRadiusPx = with(density) { cornerRadius.dp.toPx() }
        RoundedCornerShape(with(density) { currentRadiusPx.coerceAtMost(maxRadius).toDp() })
    }

    // PERFORMANCE: Hoist and remember lambdas to avoid unnecessary modifier updates (Pass 35)
    val effectsBlock: com.kyant.backdrop.BackdropEffectScope.() -> Unit = remember(blurRadius, refractionHeight, refractionAmount, chromaticAberration, vibrancy) {
        {
            vibrancy(vibrancy)
            blur(blurRadius.dp.toPx())
            liquidLens(
                refractionHeight = refractionHeight.dp.toPx(),
                refractionAmount = refractionAmount.dp.toPx(),
                chromaticAberration = chromaticAberration
            )
        }
    }

    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }
    val layer = rememberGraphicsLayer()

    val onDrawBackdropBlock: DrawScope.(drawBackdrop: DrawScope.() -> Unit) -> Unit = remember(accentColor, tintStrength, layer, totalScale, currentOpacity) {
        { drawBackdrop ->
            drawBackdrop()
            if (accentColor.isSpecified) {
                drawRect(accentColor, blendMode = BlendMode.Hue)
                drawRect(accentColor.copy(alpha = tintStrength * 0.2f))
            }
            // PERFORMANCE: Record layer only when visible/rendering (matching Pass 35 guidance)
            layer.record(
                density = this,
                layoutDirection = layoutDirection,
                size = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())
            ) {
                drawBackdrop()
                if (accentColor.isSpecified) {
                    drawRect(accentColor, blendMode = BlendMode.Hue)
                    drawRect(accentColor.copy(alpha = tintStrength * 0.2f))
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                    scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessLow)) +
                   scaleOut(spring(stiffness = Spring.StiffnessLow))
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val offset = bubbleSizePx * 0.75f
                val threshold = offset + with(density) { 32.dp.toPx() }

                val isLeft = bubbleX < threshold
                val isRight = bubbleX > screenWidth - threshold
                val isTop = bubbleY < threshold
                val isBottom = bubbleY > screenHeight - threshold

                val posDecrement: Offset
                val posReset: Offset
                val posOpenApp: Offset

                when {
                    isLeft && isTop -> {
                        posDecrement = Offset(offset, 0f)
                        posReset = Offset(offset, offset)
                        posOpenApp = Offset(0f, offset)
                    }
                    isRight && isTop -> {
                        posDecrement = Offset(-offset, 0f)
                        posReset = Offset(-offset, offset)
                        posOpenApp = Offset(0f, offset)
                    }
                    isLeft && isBottom -> {
                        posDecrement = Offset(offset, 0f)
                        posReset = Offset(offset, -offset)
                        posOpenApp = Offset(0f, -offset)
                    }
                    isRight && isBottom -> {
                        posDecrement = Offset(-offset, 0f)
                        posReset = Offset(-offset, -offset)
                        posOpenApp = Offset(0f, -offset)
                    }
                    isLeft -> {
                        posDecrement = Offset(offset * 0.707f, -offset * 0.707f)
                        posReset = Offset(offset, 0f)
                        posOpenApp = Offset(offset * 0.707f, offset * 0.707f)
                    }
                    isRight -> {
                        posDecrement = Offset(-offset * 0.707f, -offset * 0.707f)
                        posReset = Offset(-offset, 0f)
                        posOpenApp = Offset(-offset * 0.707f, offset * 0.707f)
                    }
                    isTop -> {
                        posDecrement = Offset(-offset * 0.707f, offset * 0.707f)
                        posReset = Offset(offset * 0.707f, offset * 0.707f)
                        posOpenApp = Offset(0f, offset * 1.1f)
                    }
                    isBottom -> {
                        posDecrement = Offset(-offset * 0.707f, -offset * 0.707f)
                        posReset = Offset(offset * 0.707f, -offset * 0.707f)
                        posOpenApp = Offset(0f, -offset * 1.1f)
                    }
                    else -> {
                        val angleStep = PI / 4
                        posDecrement = Offset((-offset * cos(angleStep)).toFloat(), (-offset * sin(angleStep)).toFloat())
                        posReset = Offset((offset * cos(angleStep)).toFloat(), (-offset * sin(angleStep)).toFloat())
                        posOpenApp = Offset(0f, offset)
                    }
                }

                // Decrement
                LiquidIconButton(
                    onClick = onDecrement,
                    backdrop = rememberCanvasBackdrop { },
                    modifier = Modifier.offset {
                        IntOffset(posDecrement.x.toInt(), posDecrement.y.toInt())
                    },
                    iconSize = 36.dp,
                    surfaceColor = if (isDark) Color(0xFF1976D2).copy(0.25f) else Color(0xFF90CAF9).copy(0.25f)
                ) {
                    AdaptiveIcon(Icons.Default.Remove, modifier = Modifier.size(18.dp))
                }

                // Reset
                LiquidIconButton(
                    onClick = onReset,
                    backdrop = rememberCanvasBackdrop { },
                    modifier = Modifier.offset {
                        IntOffset(posReset.x.toInt(), posReset.y.toInt())
                    },
                    iconSize = 36.dp,
                    surfaceColor = Color(0xFFFF5252).copy(alpha = 0.25f)
                ) {
                    AdaptiveIcon(Icons.Default.Replay, modifier = Modifier.size(18.dp))
                }

                // Open App
                LiquidIconButton(
                    onClick = onOpenApp,
                    backdrop = rememberCanvasBackdrop { },
                    modifier = Modifier.offset {
                        IntOffset(posOpenApp.x.toInt(), posOpenApp.y.toInt())
                    },
                    iconSize = 36.dp,
                    surfaceColor = if (isDark) Color(0xFF424242).copy(0.25f) else Color(0xFFF5F5F5).copy(0.25f)
                ) {
                    AdaptiveIcon(Icons.AutoMirrored.Filled.OpenInNew, modifier = Modifier.size(18.dp))
                }
            }
        }

        AdaptiveLuminanceProvider(layer = layer, enabled = true) {
            Box(
                Modifier
                    .size((bubbleSize * scale).dp)
                    .clip(bubbleShape)
                    .drawBackdrop(
                        backdrop = rememberCanvasBackdrop { },
                        shape = { bubbleShape },
                        effects = effectsBlock,
                        layerBlock = {
                            val progress = interactiveHighlight.pressProgress
                            val pressScale = lerp(1f, 1f + 4f.dp.toPx() / size.height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

                            val maxDragScale = 4f.dp.toPx() / size.height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX = totalScale * (pressScale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension))
                            scaleY = totalScale * (pressScale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension))
                            alpha = currentOpacity
                        },
                        onDrawBackdrop = onDrawBackdropBlock,
                        onDrawSurface = {
                            // Tint applied in onDrawBackdrop for true glass material
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { if (isExpanded) { hapticManager.vibrateSubtle(); onExpandedChange(false) } else onIncrement() },
                            onLongPress = { hapticManager.vibrateStrong(); onExpandedChange(true) }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onDraggingChange(true); onDragStart() },
                            onDragEnd = { onDraggingChange(false); onDragEnd() },
                            onDragCancel = { onDraggingChange(false); onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount)
                            }
                        )
                    }
                    .then(interactiveHighlight.modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = LocalPrismalAdaptiveColor.current,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontSize = (22 * (bubbleSize / 64f)).sp
                    )
                )
            }
        }
    }
}

@Composable
fun DismissTargetUI(
    isNear: Boolean,
    isVisible: Boolean,
    glassSettings: com.Crescent.DhikrCounter.ui.components.GlassSettings,
    vibrancy: Float,
    isDark: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isNear) 1.25f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "DismissScale"
    )
    
    val riseProgress by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "RiseProgress"
    )

    val refractionMultiplier by animateFloatAsState(if (isNear) 1.5f else 1.0f, label = "RefractionMultiplier")
    
    val xAlpha by animateFloatAsState(
        targetValue = if (riseProgress > 0.8f) 1f else 0f,
        animationSpec = tween(300),
        label = "XAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = (1 - riseProgress) * 120.dp.toPx()
                scaleX = scale * riseProgress
                scaleY = scale * riseProgress
                alpha = riseProgress
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .drawBackdrop(
                    backdrop = rememberCanvasBackdrop { },
                    shape = { Capsule() },
                    effects = {
                        vibrancy(vibrancy)
                        blur(glassSettings.blurRadius.dp.toPx())
                        liquidLens(
                            refractionHeight = (glassSettings.refractionHeight * refractionMultiplier).dp.toPx(),
                            refractionAmount = (glassSettings.refractionAmount * refractionMultiplier).dp.toPx(),
                            chromaticAberration = glassSettings.chromaticAberration
                        )
                    },
                    onDrawBackdrop = { drawBackdrop ->
                        drawBackdrop()
                        val color = if (isNear) Color.Red.copy(alpha = 0.6f) else Color.Red.copy(alpha = 0.3f)
                        drawRect(color)
                    },
                    onDrawSurface = {
                        // Tint applied in onDrawBackdrop
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AdaptiveIcon(
                Icons.Default.Close, 
                modifier = Modifier.size(32.dp).graphicsLayer { alpha = xAlpha }, 
                tint = Color.White
            )
        }
    }
}
