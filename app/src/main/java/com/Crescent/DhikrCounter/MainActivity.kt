package com.Crescent.DhikrCounter

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.Crescent.DhikrCounter.ui.theme.DhikrTheme
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.ui.AppNavigation
import com.Crescent.DhikrCounter.ui.CounterViewModel
import com.Crescent.DhikrCounter.service.FloatingCounterService
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop
import com.Crescent.DhikrCounter.ui.components.GlassSettings

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsManager: SettingsManager
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val viewModel: CounterViewModel by lazy {
        ViewModelProvider(this)[CounterViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as DhikrApplication
        settingsManager = app.settingsManager
        
        handleIntent(intent)
        
        if (settingsManager.isKeepScreenAwake) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "pref_floating_enabled" || key == "pref_dynamic_colors") {
                updateFloatingService()
            }
        }
        settingsManager.prefs.registerOnSharedPreferenceChangeListener(prefListener)

        val isFirstLaunchOverlay = settingsManager.prefs.getBoolean("is_first_launch_overlay", true)
        if (isFirstLaunchOverlay) {
            settingsManager.prefs.edit().putBoolean("is_first_launch_overlay", false).apply()
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        updateFloatingService()
        
        setContent {
            val prefs = remember { settingsManager.prefs }

            var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
            var isDynamicColors by remember { mutableStateOf(settingsManager.isDynamicColorsEnabled) }
            var isAmoled by remember { mutableStateOf(settingsManager.isAmoledModeEnabled) }
            var wallpaperUri by remember { mutableStateOf(settingsManager.appWallpaperUri) }
            var appFontFamily by remember { mutableStateOf(settingsManager.appFontFamily) }
            var appFontWeight by remember { mutableStateOf(settingsManager.appFontWeight) }

            // LIVE GLASS STATES
            var glassBlur by remember { mutableFloatStateOf(settingsManager.getGlassBlurRadius()) }
            var glassCornerRadius by remember { mutableFloatStateOf(settingsManager.getGlassCornerRadius()) }
            var glassRefractionHeight by remember { mutableFloatStateOf(settingsManager.getGlassRefractionHeight()) }
            var glassRefractionAmount by remember { mutableFloatStateOf(settingsManager.getGlassRefractionAmount()) }
            var glassChromaticAberration by remember { mutableFloatStateOf(settingsManager.getGlassChromaticAberration()) }
            var glassIntensity by remember { mutableFloatStateOf(settingsManager.getGlassBrightness()) }
            var adaptiveLuminance by remember { mutableStateOf(settingsManager.isGlassAdaptiveTextColorEnabled) }
            var adaptiveLuminanceInterval by remember { mutableIntStateOf(settingsManager.getAdaptiveLuminanceInterval()) }
            var hapticIntensity by remember { mutableFloatStateOf(settingsManager.getHapticIntensity()) }
            var hapticEnabled by remember { mutableStateOf(settingsManager.isHapticFeedbackEnabled) }
            
            // Fallback font tints
            var fontTintFallbackMode by remember { mutableIntStateOf(settingsManager.getFontTintFallbackMode()) }
            var fontTintPaletteColor by remember { mutableIntStateOf(settingsManager.getFontTintPaletteColor()) }
            var fontTintCustomColor by remember { mutableIntStateOf(settingsManager.getFontTintCustomColor()) }

            // DOCK GLASS STATES
            var dockBlur by remember { mutableFloatStateOf(settingsManager.getDockBlurRadius()) }
            var dockCornerRadius by remember { mutableFloatStateOf(settingsManager.getDockCornerRadius()) }
            var dockRefractionHeight by remember { mutableFloatStateOf(settingsManager.getDockRefractionHeight()) }
            var dockRefractionAmount by remember { mutableFloatStateOf(settingsManager.getDockRefractionAmount()) }
            var dockChromaticAberration by remember { mutableFloatStateOf(settingsManager.getDockChromaticAberration()) }

            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "pref_theme_mode" -> themeMode = p.getString("pref_theme_mode", SettingsManager.DEFAULT_THEME_MODE) ?: SettingsManager.DEFAULT_THEME_MODE
                        "pref_dynamic_colors" -> isDynamicColors = p.getBoolean("pref_dynamic_colors", SettingsManager.DEFAULT_DYNAMIC_COLORS)
                        "pref_amoled_mode" -> isAmoled = p.getBoolean("pref_amoled_mode", SettingsManager.DEFAULT_AMOLED_MODE)
                        "pref_app_wallpaper_uri" -> wallpaperUri = p.getString("pref_app_wallpaper_uri", "") ?: ""
                        "pref_app_font_family" -> appFontFamily = p.getString("pref_app_font_family", SettingsManager.DEFAULT_APP_FONT_FAMILY) ?: SettingsManager.DEFAULT_APP_FONT_FAMILY
                        "pref_app_font_weight" -> appFontWeight = p.getString("pref_app_font_weight", SettingsManager.DEFAULT_APP_FONT_WEIGHT) ?: SettingsManager.DEFAULT_APP_FONT_WEIGHT
                        
                        // Glass updates
                        "pref_glass_blur_radius" -> glassBlur = p.getFloat("pref_glass_blur_radius", SettingsManager.DEFAULT_GLASS_BLUR_RADIUS)
                        "pref_glass_corner_radius" -> glassCornerRadius = p.getFloat("pref_glass_corner_radius", SettingsManager.DEFAULT_GLASS_CORNER_RADIUS)
                        "pref_glass_refraction_height" -> glassRefractionHeight = p.getFloat("pref_glass_refraction_height", SettingsManager.DEFAULT_GLASS_REFRACTION_HEIGHT)
                        "pref_glass_refraction_amount" -> glassRefractionAmount = p.getFloat("pref_glass_refraction_amount", SettingsManager.DEFAULT_GLASS_REFRACTION_AMOUNT)
                        "pref_glass_chromatic_aberration" -> glassChromaticAberration = p.getFloat("pref_glass_chromatic_aberration", SettingsManager.DEFAULT_GLASS_CHROMATIC_ABERRATION)
                        "pref_glass_brightness" -> glassIntensity = p.getFloat("pref_glass_brightness", SettingsManager.DEFAULT_GLASS_BRIGHTNESS)
                        "pref_glass_adaptive_text_color" -> adaptiveLuminance = p.getBoolean("pref_glass_adaptive_text_color", SettingsManager.DEFAULT_GLASS_ADAPTIVE_TEXT_COLOR)
                        "pref_adaptive_luminance_interval" -> adaptiveLuminanceInterval = p.getInt("pref_adaptive_luminance_interval", SettingsManager.DEFAULT_ADAPTIVE_LUMINANCE_INTERVAL)
                        "pref_haptic_intensity" -> hapticIntensity = p.getFloat("pref_haptic_intensity", SettingsManager.DEFAULT_HAPTIC_INTENSITY)
                        "pref_haptic_feedback" -> hapticEnabled = p.getBoolean("pref_haptic_feedback", SettingsManager.DEFAULT_HAPTIC_FEEDBACK_ENABLED)

                        "pref_font_tint_fallback_mode" -> fontTintFallbackMode = p.getInt("pref_font_tint_fallback_mode", 0)
                        "pref_font_tint_palette_color" -> fontTintPaletteColor = p.getInt("pref_font_tint_palette_color", 0xFF6366F1.toInt())
                        "pref_font_tint_custom_color" -> fontTintCustomColor = p.getInt("pref_font_tint_custom_color", 0xFF6366F1.toInt())

                        // Dock updates
                        "pref_dock_blur_radius" -> dockBlur = p.getFloat("pref_dock_blur_radius", SettingsManager.DEFAULT_DOCK_BLUR_RADIUS)
                        "pref_dock_corner_radius" -> dockCornerRadius = p.getFloat("pref_dock_corner_radius", SettingsManager.DEFAULT_DOCK_CORNER_RADIUS)
                        "pref_dock_refraction_height" -> dockRefractionHeight = p.getFloat("pref_dock_refraction_height", SettingsManager.DEFAULT_DOCK_REFRACTION_HEIGHT)
                        "pref_dock_refraction_amount" -> dockRefractionAmount = p.getFloat("pref_dock_refraction_amount", SettingsManager.DEFAULT_DOCK_REFRACTION_AMOUNT)
                        "pref_dock_chromatic_aberration" -> dockChromaticAberration = p.getFloat("pref_dock_chromatic_aberration", SettingsManager.DEFAULT_DOCK_CHROMATIC_ABERRATION)
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            // ISSUE 6: Trigger glass refresh on theme change
            LaunchedEffect(darkTheme, isDynamicColors, isAmoled, wallpaperUri) {
                // Theme change or wallpaper change requires immediate backdrop refresh
            }
            
            DhikrTheme(
                darkTheme = darkTheme,
                dynamicColor = isDynamicColors,
                amoledMode = isAmoled,
                appFontFamily = appFontFamily,
                appFontWeight = appFontWeight
            ) {
                val backdrop = rememberLayerBackdrop()
                Box(modifier = Modifier.fillMaxSize()) {
                    if (wallpaperUri.isNotEmpty()) {
                        coil.compose.AsyncImage(
                            model = wallpaperUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(backdrop)
                                .background(
                                    Brush.verticalGradient(
                                        colors = if (darkTheme) {
                                            listOf(
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.surfaceContainerHigh,
                                                MaterialTheme.colorScheme.surfaceContainer
                                            )
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                MaterialTheme.colorScheme.surface,
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                                            )
                                        }
                                    )
                                )
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        val glassSettings = remember(glassBlur, glassCornerRadius, glassRefractionHeight, glassRefractionAmount, glassChromaticAberration) {
                            GlassSettings(
                                blurRadius = glassBlur,
                                cornerRadius = glassCornerRadius,
                                refractionHeight = glassRefractionHeight,
                                refractionAmount = glassRefractionAmount,
                                chromaticAberration = glassChromaticAberration
                            )
                        }
                        val dockSettings = remember(dockBlur, dockCornerRadius, dockRefractionHeight, dockRefractionAmount, dockChromaticAberration) {
                            com.Crescent.DhikrCounter.ui.components.DockSettings(
                                blurRadius = dockBlur,
                                cornerRadius = dockCornerRadius,
                                refractionHeight = dockRefractionHeight,
                                refractionAmount = dockRefractionAmount,
                                chromaticAberration = dockChromaticAberration
                            )
                        }
                        CompositionLocalProvider(
                            LocalBackdrop provides backdrop,
                            com.Crescent.DhikrCounter.ui.components.LocalGlassSettings provides glassSettings,
                            com.Crescent.DhikrCounter.ui.components.LocalDockSettings provides dockSettings,
                            com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceEnabled provides adaptiveLuminance,
                            com.Crescent.DhikrCounter.ui.components.LocalAdaptiveLuminanceInterval provides adaptiveLuminanceInterval,
                            com.Crescent.DhikrCounter.ui.components.LocalGlassIntensity provides glassIntensity,
                            com.Crescent.DhikrCounter.ui.components.LocalHapticIntensity provides hapticIntensity,
                            com.Crescent.DhikrCounter.ui.components.LocalHapticEnabled provides hapticEnabled,
                            com.Crescent.DhikrCounter.ui.components.LocalFontTintFallbackMode provides fontTintFallbackMode,
                            com.Crescent.DhikrCounter.ui.components.LocalFontTintPaletteColor provides fontTintPaletteColor,
                            com.Crescent.DhikrCounter.ui.components.LocalFontTintCustomColor provides fontTintCustomColor,
                            com.Crescent.DhikrCounter.ui.components.LocalPrismalSceneVersion provides (fontTintFallbackMode.toLong() + fontTintPaletteColor.toLong() + fontTintCustomColor.toLong())
                        ) {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }

    private fun updateFloatingService() {
        if (settingsManager.isFloatingBubbleEnabled) {
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, FloatingCounterService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            val intent = Intent(this, FloatingCounterService::class.java)
            stopService(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("show_reset_dialog", false) == true) {
            viewModel.showResetConfirmation.value = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (settingsManager.isFloatingBubbleEnabled && !Settings.canDrawOverlays(this)) {
            settingsManager.prefs.edit().putBoolean("pref_floating_enabled", false).apply()
        }
        updateFloatingService()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefListener?.let {
            if (::settingsManager.isInitialized) {
                settingsManager.prefs.unregisterOnSharedPreferenceChangeListener(it)
            }
        }
    }
}
