package com.Crescent.DhikrCounter.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.utils.SettingsManager
import com.Crescent.DhikrCounter.ui.components.*
import com.Crescent.DhikrCounter.ui.components.AdaptiveIcon
import com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidSlider
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidToggle
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidDialog
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidCard
import com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop
import com.kyant.backdrop.Backdrop
import java.util.Locale
import com.Crescent.DhikrCounter.ui.components.AppleFonts.AppleFontStyle
import com.Crescent.DhikrCounter.ui.components.AppleFonts.AppleFontWeight
import com.Crescent.DhikrCounter.ui.widgets.updateAllWidgets
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: CounterViewModel = viewModel()) {
    val context = LocalContext.current
    val settingsManager = (context.applicationContext as DhikrApplication).settingsManager
    val prefs = settingsManager.prefs
    val scope = rememberCoroutineScope()
    
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    val listState = rememberLazyListState()
    val backdrop = LocalBackdrop.current ?: com.kyant.backdrop.backdrops.rememberLayerBackdrop()

    // States
    var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
    var dynamicColors by remember { mutableStateOf(settingsManager.isDynamicColorsEnabled) }
    var adaptiveLuminance by remember { mutableStateOf(settingsManager.isGlassAdaptiveTextColorEnabled) }
    var luminanceInterval by remember { mutableIntStateOf(settingsManager.getAdaptiveLuminanceInterval()) }
    var glassIntensity by remember { mutableFloatStateOf(settingsManager.getGlassBrightness() * 100f) }

    // Optics
    var glassBlur by remember { mutableFloatStateOf(settingsManager.getGlassBlurRadius()) }
    var glassCornerRadius by remember { mutableFloatStateOf(settingsManager.getGlassCornerRadius()) }
    var glassRefractionHeight by remember { mutableFloatStateOf(settingsManager.getGlassRefractionHeight()) }
    var glassRefractionAmount by remember { mutableFloatStateOf(settingsManager.getGlassRefractionAmount()) }
    var glassChromaticAberration by remember { mutableFloatStateOf(settingsManager.getGlassChromaticAberration()) }

    // Preferences
    var allowNegative by remember { mutableStateOf(settingsManager.isNegativeCountAllowed()) }
    var confirmReset by remember { mutableStateOf(settingsManager.isConfirmBeforeReset()) }
    var wavyProgress by remember { mutableStateOf(settingsManager.isWavyProgressEnabled()) }
    var wavyThickness by remember { mutableFloatStateOf(settingsManager.getWavyThickness()) }
    var wavyTrackThickness by remember { mutableFloatStateOf(settingsManager.getWavyTrackThickness()) }
    var wavyAmplitude by remember { mutableFloatStateOf(settingsManager.getWavyAmplitude()) }
    var wavyWavelength by remember { mutableFloatStateOf(settingsManager.getWavyWavelength()) }
    var wavyGapSize by remember { mutableFloatStateOf(settingsManager.getWavyGapSize()) }
    var wavyWaveSpeed by remember { mutableFloatStateOf(settingsManager.getWavyWaveSpeed()) }
    var wavyWaveSpeedAuto by remember { mutableStateOf(settingsManager.isWavyWaveSpeedAuto()) }
    var wavyColor by remember { mutableIntStateOf(settingsManager.getWavyColor()) }
    var wavyTrackColor by remember { mutableIntStateOf(settingsManager.getWavyTrackColor()) }
    var wallpaperUri by remember { mutableStateOf(settingsManager.appWallpaperUri) }

    // Floating Counter
    var floatingEnabled by remember { mutableStateOf(settingsManager.isFloatingBubbleEnabled()) }
    var bubbleScale by remember { mutableFloatStateOf(settingsManager.getBubbleScale()) }
    var showQuickActions by remember { mutableStateOf(settingsManager.isBubbleSnapEnabled()) }

    // Floating Bubble Appearance
    var bubbleCornerRadius by remember { mutableFloatStateOf(settingsManager.getBubbleCornerRadius()) }
    var bubbleBlur by remember { mutableFloatStateOf(settingsManager.getBubbleBlurRadius()) }
    var bubbleRefractionHeight by remember { mutableFloatStateOf(settingsManager.getBubbleRefractionHeight()) }
    var bubbleRefractionAmount by remember { mutableFloatStateOf(settingsManager.getBubbleRefractionAmount()) }
    var bubbleChromaticAberration by remember { mutableFloatStateOf(settingsManager.getBubbleChromaticAberration()) }
    var bubbleVibrancy by remember { mutableFloatStateOf(settingsManager.getBubbleVibrancy() * 100f) }
    var bubbleTintStrength by remember { mutableFloatStateOf(settingsManager.getBubbleTintStrength() * 100f) }
    var bubbleAdaptiveLuminance by remember { mutableStateOf(settingsManager.isBubbleAdaptiveLuminanceEnabled()) }
    var bubbleDynamicColors by remember { mutableStateOf(settingsManager.isBubbleDynamicColorsEnabled()) }
    var bubbleAccentColor by remember { mutableIntStateOf(settingsManager.getBubbleAccentColor()) }

    var fontTintFallbackMode by remember { mutableIntStateOf(settingsManager.getFontTintFallbackMode()) }
    var fontTintPaletteColor by remember { mutableIntStateOf(settingsManager.getFontTintPaletteColor()) }
    var fontTintCustomColor by remember { mutableIntStateOf(settingsManager.getFontTintCustomColor()) }

    var appFontFamily by remember { mutableStateOf(settingsManager.appFontFamily) }
    var appFontWeight by remember { mutableStateOf(settingsManager.appFontWeight) }

    var widgetFontScale by remember { mutableFloatStateOf(settingsManager.getWidgetFontScale() * 100f) }
    var widgetAutoFit by remember { mutableStateOf(settingsManager.isWidgetAutoFitEnabled()) }

    val isBackingUp by viewModel.isBackingUp.observeAsState(false)
    val isRestoring by viewModel.isRestoring.observeAsState(false)
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.observeAsState(0L)

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var isErrorStatus by remember { mutableStateOf(false) }

    var isBackupExpanded by rememberSaveable { mutableStateOf(false) }

    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val uriString = it.toString()
                wallpaperUri = uriString
                settingsManager.setAppWallpaperUri(uriString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportBackup(context, it) { success ->
                isErrorStatus = !success
                backupStatusMessage = if (success) "Backup exported successfully!" else "Failed to export backup."
                showStatusDialog = true
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { uriResult ->
            viewModel.validateBackup(context, uriResult) { success, error ->
                if (success) {
                    pendingRestoreUri = uriResult
                    showRestoreConfirmDialog = true
                } else {
                    isErrorStatus = true
                    backupStatusMessage = error ?: "Invalid backup file."
                    showStatusDialog = true
                }
            }
        }
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showHapticsDialog by remember { mutableStateOf(false) }
    var showFontTintDialog by remember { mutableStateOf(false) }
    var showFontFamilyDialog by remember { mutableStateOf(false) }
    var showFontWeightDialog by remember { mutableStateOf(false) }

    val sizeDetails = com.Crescent.DhikrCounter.ui.components.LocalAppWindowSizeDetails.current
    val isWide = sizeDetails.widthClass == com.Crescent.DhikrCounter.ui.components.AppWindowWidthSizeClass.EXPANDED

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 700.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = if (isWide) 48.dp else 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        // APPEARANCE SECTION
        item(key = "header_appearance") { SettingsSectionHeader("Appearance") }
        item(key = "card_appearance") {
            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    SettingsNavigationItem("Theme Mode", themeMode.replaceFirstChar { it.uppercase() }, Icons.Outlined.Palette) { showThemeDialog = true }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsToggleItem("Dynamic Colors", Icons.Outlined.ColorLens, dynamicColors, backdrop) { dynamicColors = it; prefs.edit().putBoolean("pref_dynamic_colors", it).apply() }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsToggleItem("Adaptive Luminance", Icons.Outlined.BrightnessMedium, adaptiveLuminance, backdrop) { adaptiveLuminance = it; prefs.edit().putBoolean("pref_glass_adaptive_text_color", it).apply() }
                    
                    AnimatedVisibility(
                        visible = adaptiveLuminance,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider(
                                "Luminance Refresh",
                                luminanceInterval.toFloat(),
                                100f..3000f,
                                "ms",
                                backdrop,
                                SettingsManager.DEFAULT_ADAPTIVE_LUMINANCE_INTERVAL.toFloat()
                            ) {
                                luminanceInterval = it.toInt()
                                settingsManager.setAdaptiveLuminanceInterval(it.toInt())
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !adaptiveLuminance,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            SettingsNavigationItem(
                                "Font Tint (Fallback)",
                                when (fontTintFallbackMode) {
                                    0 -> "Auto from background"
                                    1 -> "Predefined palette"
                                    else -> "Custom color picker"
                                },
                                Icons.Outlined.InvertColors
                            ) { showFontTintDialog = true }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Glass Intensity", glassIntensity, 0f..100f, "%", backdrop, SettingsManager.DEFAULT_GLASS_BRIGHTNESS * 100f) {
                        glassIntensity = it
                        settingsManager.setGlassBrightness(it / 100f)
                    }
                }
            }
        }

        // TYPOGRAPHY SECTION
        item(key = "header_typography") { SettingsSectionHeader("Typography") }
        item(key = "card_typography") {
            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    SettingsNavigationItem("Font Family", appFontFamily, Icons.Outlined.FontDownload) { showFontFamilyDialog = true }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsNavigationItem("Font Weight", appFontWeight, Icons.Outlined.FormatBold) { showFontWeightDialog = true }
                }
            }
        }

        // UNIVERSAL GLASS SETTINGS
        item(key = "header_universal_glass") { SettingsSectionHeader("Universal Glass Settings") }
        item(key = "card_universal_glass") {
            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    GlassEffectSlider("Corner Radius", glassCornerRadius, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_GLASS_CORNER_RADIUS) {
                        glassCornerRadius = it
                        settingsManager.setGlassCornerRadius(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Blur Radius", glassBlur, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_GLASS_BLUR_RADIUS) {
                        glassBlur = it
                        settingsManager.setGlassBlurRadius(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Refraction Height", glassRefractionHeight, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_GLASS_REFRACTION_HEIGHT) {
                        glassRefractionHeight = it
                        settingsManager.setGlassRefractionHeight(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Refraction Amount", glassRefractionAmount, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_GLASS_REFRACTION_AMOUNT) {
                        glassRefractionAmount = it
                        settingsManager.setGlassRefractionAmount(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Chromatic Aberration", glassChromaticAberration, 0f..100f, "px", backdrop, SettingsManager.DEFAULT_GLASS_CHROMATIC_ABERRATION) {
                        glassChromaticAberration = it
                        settingsManager.setGlassChromaticAberration(it)
                    }
                }
            }
        }

        // PREFERENCES SECTION
        item(key = "header_preferences") { SettingsSectionHeader("Preferences") }
        item(key = "card_preferences") {
            var soundEnabled by remember { mutableStateOf(settingsManager.isSoundFeedbackEnabled) }
            var hapticEnabled by remember { mutableStateOf(settingsManager.isHapticFeedbackEnabled) }
            var hapticIntensity by remember { mutableFloatStateOf(settingsManager.getHapticIntensity() * 100f) }

            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    SettingsToggleItem("Sound Feedback", Icons.AutoMirrored.Filled.VolumeUp, soundEnabled, backdrop) {
                        soundEnabled = it
                        prefs.edit().putBoolean("pref_sound_feedback", it).apply()
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsToggleItem("Haptic Feedback", Icons.Outlined.Vibration, hapticEnabled, backdrop) {
                        hapticEnabled = it
                        prefs.edit().putBoolean("pref_haptic_feedback", it).apply()
                    }
                    GlassEffectSlider("Haptic Intensity", hapticIntensity, 0f..100f, "%", backdrop, SettingsManager.DEFAULT_HAPTIC_INTENSITY * 100f) {
                        hapticIntensity = it
                        prefs.edit().putFloat("pref_haptic_intensity", it / 100f).apply()
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsToggleItem("Allow Negative Count", Icons.Outlined.RemoveCircleOutline, allowNegative, backdrop) { allowNegative = it; prefs.edit().putBoolean("pref_allow_negative", it).apply() }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsToggleItem("Confirm Before Reset", Icons.Outlined.Info, confirmReset, backdrop) { confirmReset = it; prefs.edit().putBoolean("pref_confirm_reset", it).apply() }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsToggleItem("Wavy Progress Bar", Icons.Outlined.Waves, wavyProgress, backdrop) { wavyProgress = it; prefs.edit().putBoolean("pref_wavy_progress", it).apply() }
                    
                    AnimatedVisibility(visible = wavyProgress) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            
                            WavyColorPicker(
                                title = "Color",
                                selectedColor = wavyColor,
                                onColorSelected = { 
                                    wavyColor = it
                                    settingsManager.setWavyColor(it)
                                },
                                adaptiveColor = adaptiveColor
                            )
                            
                            WavyColorPicker(
                                title = "Track Color",
                                selectedColor = wavyTrackColor,
                                onColorSelected = { 
                                    wavyTrackColor = it
                                    settingsManager.setWavyTrackColor(it)
                                },
                                adaptiveColor = adaptiveColor
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            
                            GlassEffectSlider("Stroke Width", wavyThickness, 1f..32f, "dp", backdrop, SettingsManager.DEFAULT_WAVY_THICKNESS) {
                                wavyThickness = it
                                settingsManager.setWavyThickness(it)
                            }
                            
                            GlassEffectSlider("Track Stroke Width", wavyTrackThickness, 1f..32f, "dp", backdrop, SettingsManager.DEFAULT_WAVY_TRACK_THICKNESS) {
                                wavyTrackThickness = it
                                settingsManager.setWavyTrackThickness(it)
                            }
                            
                            GlassEffectSlider("Gap Size", wavyGapSize, 0f..20f, "dp", backdrop, SettingsManager.DEFAULT_WAVY_GAP_SIZE) {
                                wavyGapSize = it
                                settingsManager.setWavyGapSize(it)
                            }
                            
                            GlassEffectSlider("Amplitude", wavyAmplitude * 100f, 0f..100f, "%", backdrop, SettingsManager.DEFAULT_WAVY_AMPLITUDE * 100f) {
                                wavyAmplitude = it / 100f
                                settingsManager.setWavyAmplitude(it / 100f)
                            }
                            
                            GlassEffectSlider("Wavelength", wavyWavelength, 5f..100f, "dp", backdrop, SettingsManager.DEFAULT_WAVY_WAVELENGTH) {
                                wavyWavelength = it
                                settingsManager.setWavyWavelength(it)
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            
                            SettingsToggleItem("Auto Wave Speed", Icons.Outlined.Speed, wavyWaveSpeedAuto, backdrop) {
                                wavyWaveSpeedAuto = it
                                settingsManager.setWavyWaveSpeedAuto(it)
                            }
                            
                            AnimatedVisibility(visible = !wavyWaveSpeedAuto) {
                                GlassEffectSlider("Wave Speed", wavyWaveSpeed, 1f..200f, "dp/s", backdrop, SettingsManager.DEFAULT_WAVY_WAVE_SPEED) {
                                    wavyWaveSpeed = it
                                    settingsManager.setWavyWaveSpeed(it)
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                LiquidButton(
                                    onClick = {
                                        settingsManager.resetWavyAppearance()
                                        wavyThickness = settingsManager.getWavyThickness()
                                        wavyTrackThickness = settingsManager.getWavyTrackThickness()
                                        wavyAmplitude = settingsManager.getWavyAmplitude()
                                        wavyWavelength = settingsManager.getWavyWavelength()
                                        wavyGapSize = settingsManager.getWavyGapSize()
                                        wavyWaveSpeed = settingsManager.getWavyWaveSpeed()
                                        wavyWaveSpeedAuto = settingsManager.isWavyWaveSpeedAuto()
                                        wavyColor = settingsManager.getWavyColor()
                                        wavyTrackColor = settingsManager.getWavyTrackColor()
                                    },
                                    modifier = Modifier.fillMaxWidth(0.6f),
                                    surfaceColor = Color.Red.copy(alpha = 0.1f),
                                    tint = Color.Red.copy(alpha = 0.7f),
                                    backdrop = backdrop
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.RestartAlt, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Reset Wavy Progress", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // HOME SCREEN WIDGET SECTION
        item(key = "header_widgets") { SettingsSectionHeader("Home Screen Widgets") }
        item(key = "card_widgets") {
            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    SettingsToggleItem("Auto-fit Font to Size", Icons.Outlined.AutoFixHigh, widgetAutoFit, backdrop) {
                        widgetAutoFit = it
                        settingsManager.setWidgetAutoFitEnabled(it)
                        updateAllWidgets(context)
                    }

                    AnimatedVisibility(visible = !widgetAutoFit) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider(
                                "Manual Font Scale",
                                widgetFontScale,
                                80f..150f,
                                "%",
                                backdrop,
                                100f
                            ) {
                                widgetFontScale = it
                                settingsManager.setWidgetFontScale(it / 100f)
                                updateAllWidgets(context)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, null, tint = adaptiveColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Widgets update automatically on count changes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = adaptiveColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // FLOATING COUNTER SECTION
        item(key = "header_floating_counter") { SettingsSectionHeader("Floating Counter") }
        item(key = "card_floating_counter") {
            var isAppearanceExpanded by rememberSaveable { mutableStateOf(false) }

            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    SettingsToggleItem("Enable Floating", Icons.Outlined.Layers, floatingEnabled, backdrop) {
                        floatingEnabled = it
                        prefs.edit().putBoolean("pref_floating_enabled", it).apply()
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))

                    // Expandable Bubble Appearance Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAppearanceExpanded = !isAppearanceExpanded }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdaptiveIcon(Icons.Outlined.AutoAwesome, modifier = Modifier.size(24.dp), tint = adaptiveColor.copy(alpha = 0.7f))
                            Spacer(Modifier.width(16.dp))
                            Text("Bubble Appearance", style = MaterialTheme.typography.bodyLarge, color = adaptiveColor, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(
                            if (isAppearanceExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            null,
                            tint = adaptiveColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isAppearanceExpanded) {
                        Column {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))

                            GlassEffectSlider("Bubble Scale", bubbleScale, 1f..4f, "×", backdrop, SettingsManager.DEFAULT_BUBBLE_SCALE) {
                                bubbleScale = it
                                settingsManager.setBubbleScale(it)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider("Corner Radius", bubbleCornerRadius, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_BUBBLE_CORNER_RADIUS) {
                                bubbleCornerRadius = it
                                settingsManager.setBubbleCornerRadius(it)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider("Blur Radius", bubbleBlur, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_BUBBLE_BLUR_RADIUS) {
                                bubbleBlur = it
                                settingsManager.setBubbleBlurRadius(it)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider("Refraction Height", bubbleRefractionHeight, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_BUBBLE_REFRACTION_HEIGHT) {
                                bubbleRefractionHeight = it
                                settingsManager.setBubbleRefractionHeight(it)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider("Refraction Amount", bubbleRefractionAmount, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_BUBBLE_REFRACTION_AMOUNT) {
                                bubbleRefractionAmount = it
                                settingsManager.setBubbleRefractionAmount(it)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider("Chromatic Aberration", bubbleChromaticAberration, 0f..100f, "px", backdrop, SettingsManager.DEFAULT_BUBBLE_CHROMATIC_ABERRATION) {
                                bubbleChromaticAberration = it
                                settingsManager.setBubbleChromaticAberration(it)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider("Vibrancy", bubbleVibrancy, 0f..200f, "%", backdrop, SettingsManager.DEFAULT_BUBBLE_VIBRANCY * 100f) {
                                bubbleVibrancy = it
                                settingsManager.setBubbleVibrancy(it / 100f)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            GlassEffectSlider("Tint Strength", bubbleTintStrength, 0f..100f, "%", backdrop, SettingsManager.DEFAULT_BUBBLE_TINT_STRENGTH * 100f) {
                                bubbleTintStrength = it
                                settingsManager.setBubbleTintStrength(it / 100f)
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            SettingsToggleItem("Dynamic Colors", Icons.Outlined.ColorLens, bubbleDynamicColors, backdrop) {
                                bubbleDynamicColors = it
                                settingsManager.setBubbleDynamicColorsEnabled(it)
                            }

                            AnimatedContent(
                                targetState = bubbleDynamicColors,
                                label = "BubbleColorPickerVisibility"
                            ) { isDynamic ->
                                if (isDynamic) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Info, null, tint = adaptiveColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text("Using Dynamic Colors", style = MaterialTheme.typography.bodyMedium, color = adaptiveColor.copy(alpha = 0.5f))
                                    }
                                } else {
                                    BubbleColorPicker(
                                        selectedColor = Color(bubbleAccentColor),
                                        onColorSelected = {
                                            bubbleAccentColor = it.toArgb()
                                            settingsManager.setBubbleAccentColor(it.toArgb())
                                        },
                                        adaptiveColor = adaptiveColor
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            SettingsToggleItem("Adaptive Luminance", Icons.Outlined.BrightnessMedium, bubbleAdaptiveLuminance, backdrop) {
                                bubbleAdaptiveLuminance = it
                                settingsManager.setBubbleAdaptiveLuminanceEnabled(it)
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            LiquidButton(
                                onClick = {
                                    settingsManager.resetBubbleAppearance()
                                    // Refresh local states
                                    bubbleCornerRadius = settingsManager.getBubbleCornerRadius()
                                    bubbleBlur = settingsManager.getBubbleBlurRadius()
                                    bubbleRefractionHeight = settingsManager.getBubbleRefractionHeight()
                                    bubbleRefractionAmount = settingsManager.getBubbleRefractionAmount()
                                    bubbleChromaticAberration = settingsManager.getBubbleChromaticAberration()
                                    bubbleVibrancy = settingsManager.getBubbleVibrancy() * 100f
                                    bubbleTintStrength = settingsManager.getBubbleTintStrength() * 100f
                                    bubbleAdaptiveLuminance = settingsManager.isBubbleAdaptiveLuminanceEnabled()
                                    bubbleDynamicColors = settingsManager.isBubbleDynamicColorsEnabled()
                                    bubbleAccentColor = settingsManager.getBubbleAccentColor()
                                    bubbleScale = settingsManager.getBubbleScale()
                                },
                                backdrop = backdrop,
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                tint = adaptiveColor.copy(alpha = 0.05f)
                            ) {
                                Icon(Icons.Outlined.RestartAlt, null, tint = adaptiveColor)
                                Spacer(Modifier.width(8.dp))
                                Text("Reset Bubble Appearance", color = adaptiveColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    SettingsToggleItem("Show Quick Actions", Icons.Outlined.OfflineBolt, showQuickActions, backdrop) { showQuickActions = it; prefs.edit().putBoolean("pref_bubble_snap", it).apply() }
                }
            }
        }

        // DOCK SETTINGS SECTION
        item(key = "header_dock_settings") { SettingsSectionHeader("Dock Settings") }
        item(key = "card_dock_settings") {
            var dockCornerRadius by remember { mutableFloatStateOf(settingsManager.getDockCornerRadius()) }
            var dockBlur by remember { mutableFloatStateOf(settingsManager.getDockBlurRadius()) }
            var dockRefractionHeight by remember { mutableFloatStateOf(settingsManager.getDockRefractionHeight()) }
            var dockRefractionAmount by remember { mutableFloatStateOf(settingsManager.getDockRefractionAmount()) }
            var dockChromaticAberration by remember { mutableFloatStateOf(settingsManager.getDockChromaticAberration()) }

            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    GlassEffectSlider("Corner Radius", dockCornerRadius, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_DOCK_CORNER_RADIUS) {
                        dockCornerRadius = it
                        settingsManager.setDockCornerRadius(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Blur Radius", dockBlur, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_DOCK_BLUR_RADIUS) {
                        dockBlur = it
                        settingsManager.setDockBlurRadius(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Refraction Height", dockRefractionHeight, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_DOCK_REFRACTION_HEIGHT) {
                        dockRefractionHeight = it
                        settingsManager.setDockRefractionHeight(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Refraction Amount", dockRefractionAmount, 0f..100f, "dp", backdrop, SettingsManager.DEFAULT_DOCK_REFRACTION_AMOUNT) {
                        dockRefractionAmount = it
                        settingsManager.setDockRefractionAmount(it)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                    GlassEffectSlider("Chromatic Aberration", dockChromaticAberration, 0f..100f, "px", backdrop, SettingsManager.DEFAULT_DOCK_CHROMATIC_ABERRATION) {
                        dockChromaticAberration = it
                        settingsManager.setDockChromaticAberration(it)
                    }
                }
            }
        }

        // BACKUP & RESTORE SECTION
        item(key = "header_backup_restore") { SettingsSectionHeader("Backup & Restore") }
        item(key = "card_backup_restore") {
            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isBackupExpanded = !isBackupExpanded }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdaptiveIcon(Icons.Outlined.Backup, modifier = Modifier.size(24.dp), tint = adaptiveColor.copy(alpha = 0.7f))
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Backup & Restore", style = MaterialTheme.typography.bodyLarge, color = adaptiveColor, fontWeight = FontWeight.SemiBold)
                                if (lastBackupTimestamp > 0L) {
                                    val dateStr = remember(lastBackupTimestamp) {
                                        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(lastBackupTimestamp))
                                    }
                                    Text("Last backup: $dateStr", style = MaterialTheme.typography.bodySmall, color = adaptiveColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                        Icon(
                            if (isBackupExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            null,
                            tint = adaptiveColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isBackupExpanded) {
                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                            
                            if (isBackingUp || isRestoring) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LiquidButton(
                                        onClick = { exportLauncher.launch("DhikrCounter_Backup_${System.currentTimeMillis()}.json") },
                                        backdrop = backdrop,
                                        modifier = Modifier.fillMaxWidth(),
                                        surfaceColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(Icons.Outlined.CloudUpload, null, tint = if (MaterialTheme.colorScheme.primary.luminance() > 0.5f) Color.Black else Color.White)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Export Backup", color = if (MaterialTheme.colorScheme.primary.luminance() > 0.5f) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                    }

                                    LiquidButton(
                                        onClick = { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream")) },
                                        backdrop = backdrop,
                                        modifier = Modifier.fillMaxWidth(),
                                        surfaceColor = Color.Transparent,
                                        tint = adaptiveColor.copy(alpha = 0.05f)
                                    ) {
                                        Icon(Icons.Outlined.CloudDownload, null, tint = adaptiveColor)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Restore from Backup", color = adaptiveColor, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // BACKGROUND SECTION
        item(key = "header_background") { SettingsSectionHeader("Background") }
        item(key = "card_background") {
            LiquidCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), backdrop = backdrop) {
                Column {
                    SettingsNavigationItem(
                        title = "Change Background Image",
                        icon = Icons.Outlined.Image,
                        onClick = {
                            backgroundLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    if (wallpaperUri.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = adaptiveColor.copy(alpha = 0.05f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    wallpaperUri = ""
                                    settingsManager.setAppWallpaperUri("")
                                }
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.RestartAlt, null, tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(16.dp))
                            Text("Reset to Default", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) }
    }

    if (showThemeDialog) {
        LiquidDialog(
            onDismissRequest = { showThemeDialog = false },
            backdrop = backdrop,
            title = "Theme Mode",
            positiveText = "Done",
            negativeText = null,
            onPositive = { showThemeDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("light", "dark", "system").forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                themeMode = mode
                                prefs.edit().putString("pref_theme_mode", mode).apply()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            mode.replaceFirstChar { it.uppercase() },
                            color = LocalPrismalAdaptiveColor.current,
                            fontWeight = if (themeMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                        if (themeMode == mode) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    if (showHapticsDialog) {
        var soundEnabled by remember { mutableStateOf(settingsManager.isSoundFeedbackEnabled) }
        var hapticEnabled by remember { mutableStateOf(settingsManager.isHapticFeedbackEnabled) }
        var hapticIntensity by remember { mutableFloatStateOf(settingsManager.getHapticIntensity() * 100f) }

        LiquidDialog(
            onDismissRequest = { showHapticsDialog = false },
            backdrop = backdrop,
            title = "Sound & Haptics",
            positiveText = "Done",
            negativeText = null,
            onPositive = { showHapticsDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsToggleItem("Sound Feedback", Icons.AutoMirrored.Filled.VolumeUp, soundEnabled, backdrop) {
                    soundEnabled = it
                    prefs.edit().putBoolean("pref_sound_feedback", it).apply()
                }

                HorizontalDivider(color = LocalPrismalAdaptiveColor.current.copy(alpha = 0.05f))

                SettingsToggleItem("Haptic Feedback", Icons.Outlined.Vibration, hapticEnabled, backdrop) {
                    hapticEnabled = it
                    prefs.edit().putBoolean("pref_haptic_feedback", it).apply()
                }

                GlassEffectSlider("Haptic Intensity", hapticIntensity, 0f..100f, "%", backdrop, SettingsManager.DEFAULT_HAPTIC_INTENSITY * 100f) {
                    hapticIntensity = it
                    prefs.edit().putFloat("pref_haptic_intensity", it / 100f).apply()
                }
            }
        }
    }

    if (showFontTintDialog) {
        LiquidDialog(
            onDismissRequest = { showFontTintDialog = false },
            backdrop = backdrop,
            title = "Font Tint (Fallback)",
            positiveText = "Done",
            onPositive = { showFontTintDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Mode Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Auto", "Palette", "Custom").forEachIndexed { index, label ->
                        LiquidButton(
                            onClick = { 
                                fontTintFallbackMode = index
                                settingsManager.setFontTintFallbackMode(index)
                            },
                            modifier = Modifier.weight(1f),
                            surfaceColor = if (fontTintFallbackMode == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                            backdrop = backdrop
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black, color = LocalPrismalAdaptiveColor.current)
                        }
                    }
                }

                HorizontalDivider(color = LocalPrismalAdaptiveColor.current.copy(alpha = 0.05f))

                when (fontTintFallbackMode) {
                    0 -> { // Auto
                        Text(
                            "Automatically derives a readable tint from your background wallpaper. Samples once per background change for efficiency.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalPrismalAdaptiveColor.current.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                    1 -> { // Palette
                        FontTintPalettePicker(
                            selectedColor = Color(fontTintPaletteColor),
                            onColorSelected = {
                                fontTintPaletteColor = it.toArgb()
                                settingsManager.setFontTintPaletteColor(it.toArgb())
                            },
                            adaptiveColor = LocalPrismalAdaptiveColor.current
                        )
                    }
                    2 -> { // Custom
                        FontTintPalettePicker(
                            selectedColor = Color(fontTintCustomColor),
                            onColorSelected = {
                                fontTintCustomColor = it.toArgb()
                                settingsManager.setFontTintCustomColor(it.toArgb())
                            },
                            adaptiveColor = LocalPrismalAdaptiveColor.current,
                            isCustom = true
                        )
                    }
                }
            }
        }
    }

    if (showFontFamilyDialog) {
        LiquidDialog(
            onDismissRequest = { showFontFamilyDialog = false },
            backdrop = backdrop,
            title = "Font Family",
            positiveText = "Done",
            onPositive = { showFontFamilyDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppleFontStyle.entries.forEach { style ->
                    val isSelected = appFontFamily == style.label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LocalPrismalAdaptiveColor.current.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                appFontFamily = style.label
                                settingsManager.setAppFontFamily(style.label)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                style.label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = style.fontFamily,
                                    fontWeight = AppleFontWeight.fromLabel(appFontWeight).weight
                                ),
                                color = LocalPrismalAdaptiveColor.current
                            )
                            Text(
                                "The quick brown fox jumps over the lazy dog",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = style.fontFamily,
                                    fontWeight = AppleFontWeight.fromLabel(appFontWeight).weight
                                ),
                                color = LocalPrismalAdaptiveColor.current.copy(alpha = 0.6f)
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showFontWeightDialog) {
        LiquidDialog(
            onDismissRequest = { showFontWeightDialog = false },
            backdrop = backdrop,
            title = "Font Weight",
            positiveText = "Done",
            onPositive = { showFontWeightDialog = false }
        ) {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppleFontWeight.entries.size) { index ->
                    val weight = AppleFontWeight.entries[index]
                    val isSelected = appFontWeight == weight.label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LocalPrismalAdaptiveColor.current.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                appFontWeight = weight.label
                                settingsManager.setAppFontWeight(weight.label)
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                weight.label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = AppleFontStyle.fromLabel(appFontFamily).fontFamily,
                                    fontWeight = weight.weight
                                ),
                                color = LocalPrismalAdaptiveColor.current
                            )
                            Text(
                                "The quick brown fox jumps over the lazy dog",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = AppleFontStyle.fromLabel(appFontFamily).fontFamily,
                                    fontWeight = weight.weight
                                ),
                                color = LocalPrismalAdaptiveColor.current.copy(alpha = 0.6f)
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }


    if (showRestoreConfirmDialog) {
        LiquidDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            backdrop = backdrop,
            title = "Restore Backup",
            message = "This will overwrite all current data. This action cannot be undone.",
            positiveText = "Restore",
            negativeText = "Cancel",
            onPositive = {
                pendingRestoreUri?.let { uri ->
                    viewModel.restoreBackup(context, uri) { success ->
                        isErrorStatus = !success
                        backupStatusMessage = if (success) "Data restored successfully!" else "Failed to restore data."
                        showStatusDialog = true
                        if (success) {
                            try {
                                // Sync local UI states with restored settings
                                themeMode = settingsManager.themeMode
                                dynamicColors = settingsManager.isDynamicColorsEnabled
                                adaptiveLuminance = settingsManager.isGlassAdaptiveTextColorEnabled
                                luminanceInterval = settingsManager.getAdaptiveLuminanceInterval()
                                glassIntensity = settingsManager.getGlassBrightness() * 100f
                                glassBlur = settingsManager.getGlassBlurRadius()
                                glassCornerRadius = settingsManager.getGlassCornerRadius()
                                glassRefractionHeight = settingsManager.getGlassRefractionHeight()
                                glassRefractionAmount = settingsManager.getGlassRefractionAmount()
                                glassChromaticAberration = settingsManager.getGlassChromaticAberration()
                                allowNegative = settingsManager.isNegativeCountAllowed()
                                confirmReset = settingsManager.isConfirmBeforeReset()
                                wavyProgress = settingsManager.isWavyProgressEnabled()
                                wallpaperUri = settingsManager.appWallpaperUri
                                floatingEnabled = settingsManager.isFloatingBubbleEnabled()
                                bubbleScale = settingsManager.getBubbleScale()
                                showQuickActions = settingsManager.isBubbleSnapEnabled()
                                bubbleCornerRadius = settingsManager.getBubbleCornerRadius()
                                bubbleBlur = settingsManager.getBubbleBlurRadius()
                                bubbleRefractionHeight = settingsManager.getBubbleRefractionHeight()
                                bubbleRefractionAmount = settingsManager.getBubbleRefractionAmount()
                                bubbleChromaticAberration = settingsManager.getBubbleChromaticAberration()
                                bubbleVibrancy = settingsManager.getBubbleVibrancy() * 100f
                                bubbleTintStrength = settingsManager.getBubbleTintStrength() * 100f
                                bubbleAdaptiveLuminance = settingsManager.isBubbleAdaptiveLuminanceEnabled()
                                bubbleDynamicColors = settingsManager.isBubbleDynamicColorsEnabled()
                                bubbleAccentColor = settingsManager.getBubbleAccentColor()
                                fontTintFallbackMode = settingsManager.getFontTintFallbackMode()
                                fontTintPaletteColor = settingsManager.getFontTintPaletteColor()
                                fontTintCustomColor = settingsManager.getFontTintCustomColor()
                                appFontFamily = settingsManager.appFontFamily
                                appFontWeight = settingsManager.appFontWeight
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        )
    }

    if (showStatusDialog) {
        LiquidDialog(
            onDismissRequest = { showStatusDialog = false },
            backdrop = backdrop,
            title = if (isErrorStatus) "Error" else "Success",
            message = backupStatusMessage ?: "",
            positiveText = "OK",
            negativeText = null,
            onPositive = { showStatusDialog = false },
            icon = if (isErrorStatus) Icons.Outlined.ErrorOutline else Icons.Outlined.CheckCircle,
            iconTint = if (isErrorStatus) Color.Red else Color.Green
        )
    }
}
}

private val BubblePresets = listOf(
    "Crystal White" to Color(0xFFFFFFFF),
    "Ice Blue" to Color(0xFFB3E5FC),
    "Ocean Cyan" to Color(0xFF00BCD4),
    "Emerald" to Color(0xFF4CAF50),
    "Mint" to Color(0xFFB9F6CA),
    "Violet" to Color(0xFF9C27B0),
    "Amber" to Color(0xFFFFC107),
    "Rose" to Color(0xFFE91E63),
    "Graphite" to Color(0xFF424242),
    "Ruby" to Color(0xFFD32F2F)
)

@Composable
fun BubbleColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    adaptiveColor: Color,
    title: String = "Bubble Accent Color"
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = adaptiveColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BubblePresets.size) { index ->
                    val (_, color) = BubblePresets[index]
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = 2.dp,
                                color = if (selectedColor == color) adaptiveColor else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

@Composable
fun WavyColorPicker(
    title: String,
    selectedColor: Int, // 0 for Auto
    onColorSelected: (Int) -> Unit,
    adaptiveColor: Color
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = adaptiveColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(adaptiveColor.copy(alpha = 0.1f))
                        .border(
                            width = 2.dp,
                            color = if (selectedColor == 0) adaptiveColor else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onColorSelected(0) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "Auto",
                        tint = adaptiveColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            items(PalettePresets.size) { index ->
                val color = PalettePresets[index]
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color)
                        .border(
                            width = 2.dp,
                            color = if (selectedColor == color.toArgb()) adaptiveColor else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onColorSelected(color.toArgb()) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == color.toArgb()) {
                        Icon(
                            Icons.Default.Check,
                            null,
                            tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsToggleItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, backdrop: Backdrop, onCheckedChange: (Boolean) -> Unit) {
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AdaptiveIcon(icon, modifier = Modifier.size(24.dp), tint = adaptiveColor.copy(alpha = 0.7f))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = adaptiveColor, fontWeight = FontWeight.SemiBold)
        }
        LiquidToggle(selected = { checked }, onSelect = onCheckedChange, backdrop = backdrop)
    }
}

@Composable
fun SettingsNavigationItem(title: String, value: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AdaptiveIcon(icon, modifier = Modifier.size(24.dp), tint = adaptiveColor.copy(alpha = 0.7f))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = adaptiveColor, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = adaptiveColor.copy(alpha = 0.6f))
                Spacer(Modifier.width(8.dp))
            }
            AdaptiveIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, modifier = Modifier.size(24.dp), tint = adaptiveColor.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun GlassEffectSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, unit: String, backdrop: Backdrop, defaultValue: Float? = null, onValueChange: (Float) -> Unit) {
    val adaptiveColor = LocalPrismalAdaptiveColor.current
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = adaptiveColor, fontWeight = FontWeight.Bold)
                if (defaultValue != null && value != defaultValue) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Outlined.RestartAlt,
                        contentDescription = "Reset",
                        modifier = Modifier.size(16.dp).clickable { onValueChange(defaultValue) },
                        tint = adaptiveColor.copy(alpha = 0.5f)
                    )
                }
            }
            val displayValue = when (unit) {
                "%" -> "${value.toInt()}%"
                "dp" -> String.format(Locale.ROOT, "%d dp", value.toInt())
                else -> String.format(Locale.ROOT, "%d", value.toInt())
            }
            Text(displayValue, style = MaterialTheme.typography.bodySmall, color = adaptiveColor.copy(alpha = 0.6f))
        }
        LiquidSlider(
            value = { value },
            onValueChange = onValueChange,
            valueRange = range,
            visibilityThreshold = 0.01f,
            backdrop = backdrop
        )
    }
}

private val CustomPresets = listOf(
    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF3B82F6),
    Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4),
    Color(0xFF78716C), Color(0xFF451A03), Color(0xFF064E3B), Color(0xFF1E3A8A),
    Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF94A3B8), Color(0xFF334155)
)

private val PalettePresets = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFFEC4899), // Pink
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6)  // Violet
)

@Composable
fun FontTintPalettePicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    adaptiveColor: Color,
    isCustom: Boolean = false
) {
    val presets = if (isCustom) CustomPresets else PalettePresets

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        presets.chunked(4).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color)
                            .border(
                                width = 2.dp,
                                color = if (selectedColor == color) adaptiveColor else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onColorSelected(color) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor == color) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
