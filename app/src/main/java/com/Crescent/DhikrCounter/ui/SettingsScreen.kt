package com.Crescent.DhikrCounter.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.Crescent.DhikrCounter.DhikrApplication
import com.Crescent.DhikrCounter.utils.BackupManagerUtil
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = (context.applicationContext as DhikrApplication).settingsManager
    val prefs = settingsManager.prefs

    val scope = rememberCoroutineScope()
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var snackbarMessage by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val success = BackupManagerUtil.exportBackup(context, uri)
                snackbarMessage = if (success) "Backup exported successfully" else "Failed to export backup"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            restoreUri = uri
            showRestoreDialog = true
        }
    }

    // General
    var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
    var dynamicColors by remember { mutableStateOf(settingsManager.isDynamicColorsEnabled) }
    var amoledMode by remember { mutableStateOf(settingsManager.isAmoledModeEnabled) }
    
    var showThemeDialog by remember { mutableStateOf(false) }
    
    // UI Customization
    var cornerRadius by remember { mutableStateOf(settingsManager.cornerRadius) }
    var wavyProgress by remember { mutableStateOf(settingsManager.isWavyProgressEnabled) }

    // Counter Settings
    var countAnimation by remember { mutableStateOf(settingsManager.isCountAnimationEnabled) }
    var allowNegative by remember { mutableStateOf(settingsManager.isNegativeCountAllowed) }
    var confirmReset by remember { mutableStateOf(settingsManager.isConfirmBeforeReset) }
    var keepAwake by remember { mutableStateOf(settingsManager.isKeepScreenAwake) }

    // Haptics & Sound
    var hapticEnabled by remember { mutableStateOf(settingsManager.isHapticFeedbackEnabled) }
    var hapticIntensity by remember { mutableStateOf(settingsManager.hapticIntensity) }
    var soundEnabled by remember { mutableStateOf(settingsManager.isSoundFeedbackEnabled) }

    // Floating Counter
    var floatingEnabled by remember { mutableStateOf(settingsManager.isFloatingBubbleEnabled) }
    var floatingOpacity by remember { mutableStateOf(settingsManager.bubbleOpacity) }
    var floatingSize by remember { mutableStateOf(settingsManager.bubbleSize.toFloat()) }

    // Widget Appearance
    var widgetStyle by remember { mutableStateOf(settingsManager.getWidgetStyle()) }
    var widgetCornerRadius by remember { mutableStateOf(settingsManager.getWidgetCornerRadius()) }
    var widgetScale by remember { mutableStateOf(settingsManager.getWidgetScale()) }
    var widgetDepth by remember { mutableStateOf(settingsManager.getWidgetDepth()) }
    var widgetOpacity by remember { mutableStateOf(settingsManager.getWidgetOpacity()) }
    var widgetAnimation by remember { mutableStateOf(settingsManager.getWidgetAnimation()) }
    
    var showStyleDialog by remember { mutableStateOf(false) }
    var showAnimationDialog by remember { mutableStateOf(false) }

    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "pref_floating_enabled") {
                floatingEnabled = p.getBoolean("pref_floating_enabled", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackbarMessage)
            snackbarMessage = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { SettingsHeader("General") }
            item {
                Surface(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ListItem(
                        headlineContent = { Text("App Theme", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { 
                            Text(when(themeMode) {
                                "light" -> "Light"
                                "dark" -> "Dark"
                                else -> "System Default"
                            })
                        }
                    )
                }
            }
            item {
                SwitchPreference(
                    title = "Dynamic Colors",
                    subtitle = "Use Android 12+ Material You colors",
                    checked = dynamicColors,
                    onCheckedChange = { 
                        dynamicColors = it
                        prefs.edit().putBoolean("pref_dynamic_colors", it).apply() 
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "AMOLED Dark Mode",
                    subtitle = "Use pitch black instead of dark gray",
                    checked = amoledMode,
                    onCheckedChange = { 
                        amoledMode = it
                        prefs.edit().putBoolean("pref_amoled_mode", it).apply() 
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "Keep Screen Awake",
                    checked = keepAwake,
                    onCheckedChange = { 
                        keepAwake = it
                        prefs.edit().putBoolean("pref_keep_awake", it).apply() 
                    }
                )
            }

            item { SettingsHeader("Appearance") }
            item {
                SliderPreference(
                    title = "Corner Radius",
                    value = cornerRadius,
                    valueRange = 0f..48f,
                    onValueChange = { 
                        cornerRadius = it
                        prefs.edit().putFloat("pref_corner_radius", it).apply() 
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "Wavy Progress Bars",
                    subtitle = "Use Material 3 Expressive wavy progress indicators",
                    checked = wavyProgress,
                    onCheckedChange = { 
                        wavyProgress = it
                        prefs.edit().putBoolean("pref_wavy_progress", it).apply()
                    }
                )
            }

            item { SettingsHeader("Counter") }
            item {
                SwitchPreference(
                    title = "Count Animation",
                    subtitle = "Animate numbers when they change",
                    checked = countAnimation,
                    onCheckedChange = { 
                        countAnimation = it
                        prefs.edit().putBoolean("pref_count_animation", it).apply() 
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "Allow Negative Count",
                    subtitle = "Allow counter to go below zero",
                    checked = allowNegative,
                    onCheckedChange = { 
                        allowNegative = it
                        prefs.edit().putBoolean("pref_allow_negative", it).apply() 
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "Confirm Before Reset",
                    checked = confirmReset,
                    onCheckedChange = { 
                        confirmReset = it
                        prefs.edit().putBoolean("pref_confirm_reset", it).apply() 
                    }
                )
            }

            item { SettingsHeader("Feedback") }
            item {
                SwitchPreference(
                    title = "Haptic Feedback",
                    checked = hapticEnabled,
                    onCheckedChange = { 
                        hapticEnabled = it
                        prefs.edit().putBoolean("pref_haptic_feedback", it).apply() 
                    }
                )
            }
            item {
                SliderPreference(
                    title = "Vibration Intensity",
                    value = hapticIntensity,
                    valueRange = 0.1f..2.0f,
                    onValueChange = { 
                        hapticIntensity = it
                        prefs.edit().putFloat("pref_haptic_intensity", it).apply() 
                    }
                )
            }
            item {
                SwitchPreference(
                    title = "Sound Feedback",
                    checked = soundEnabled,
                    onCheckedChange = { 
                        soundEnabled = it
                        prefs.edit().putBoolean("pref_sound_feedback", it).apply() 
                    }
                )
            }

            item { SettingsHeader("Floating Counter") }
            item {
                SwitchPreference(
                    title = "Enable Floating Counter",
                    subtitle = "Requires 'Display over other apps' permission",
                    checked = floatingEnabled,
                    onCheckedChange = { 
                        if (it && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            floatingEnabled = it
                            prefs.edit().putBoolean("pref_floating_enabled", it).apply()
                        }
                    }
                )
            }
            item {
                SliderPreference(
                    title = "Widget Size",
                    value = floatingSize,
                    valueRange = 32f..128f,
                    onValueChange = { 
                        floatingSize = it
                        prefs.edit().putInt("pref_bubble_size", it.toInt()).apply() 
                    }
                )
            }
            item {
                SliderPreference(
                    title = "Widget Opacity",
                    value = floatingOpacity,
                    valueRange = 0.1f..1.0f,
                    onValueChange = { 
                        floatingOpacity = it
                        prefs.edit().putFloat("pref_bubble_opacity", it).apply() 
                    }
                )
            }

            item { SettingsHeader("Widget Appearance") }
            item {
                WidgetPreview(
                    style = widgetStyle,
                    cornerRadius = widgetCornerRadius,
                    scale = widgetScale,
                    depth = widgetDepth,
                    opacity = widgetOpacity
                )
            }
            item {
                Surface(
                    onClick = { showStyleDialog = true },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ListItem(
                        headlineContent = { Text("Widget Style", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(widgetStyle) }
                    )
                }
            }
            item {
                SliderPreference(
                    title = "Widget Corner Radius",
                    value = widgetCornerRadius,
                    valueRange = 0f..48f,
                    onValueChange = { 
                        widgetCornerRadius = it
                        prefs.edit().putFloat("pref_widget_corner_radius", it).apply() 
                    }
                )
            }
            item {
                SliderPreference(
                    title = "Widget Scale",
                    value = widgetScale,
                    valueRange = 0.5f..1.5f,
                    onValueChange = { 
                        widgetScale = it
                        prefs.edit().putFloat("pref_widget_scale", it).apply() 
                    }
                )
            }
            item {
                SliderPreference(
                    title = "Widget Depth",
                    value = widgetDepth,
                    valueRange = 0.0f..1.0f,
                    onValueChange = { 
                        widgetDepth = it
                        prefs.edit().putFloat("pref_widget_depth", it).apply() 
                    }
                )
            }
            item {
                SliderPreference(
                    title = "Widget Opacity",
                    value = widgetOpacity,
                    valueRange = 0.0f..1.0f,
                    onValueChange = { 
                        widgetOpacity = it
                        prefs.edit().putFloat("pref_widget_opacity", it).apply() 
                    }
                )
            }
            item {
                Surface(
                    onClick = { showAnimationDialog = true },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ListItem(
                        headlineContent = { Text("Widget Animation", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text(widgetAnimation) }
                    )
                }
            }
            item {
                TextButton(
                    onClick = {
                        widgetStyle = "Material 3"
                        widgetCornerRadius = 24f
                        widgetScale = 1.0f
                        widgetDepth = 0.5f
                        widgetOpacity = 1.0f
                        widgetAnimation = "Smooth"
                        prefs.edit()
                            .putString("pref_widget_style", widgetStyle)
                            .putFloat("pref_widget_corner_radius", widgetCornerRadius)
                            .putFloat("pref_widget_scale", widgetScale)
                            .putFloat("pref_widget_depth", widgetDepth)
                            .putFloat("pref_widget_opacity", widgetOpacity)
                            .putString("pref_widget_animation", widgetAnimation)
                            .apply()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("Restore Defaults")
                }
            }

            item { SettingsHeader("Data & Backup") }
            item {
                Surface(
                    onClick = {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        exportLauncher.launch("DhikrCounter_Backup_$timestamp.json")
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ListItem(
                        headlineContent = { Text("Backup Data") },
                        supportingContent = { Text("Save your sessions and history") }
                    )
                }
            }
            item {
                Surface(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json"))
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    ListItem(
                        headlineContent = { Text("Restore Data") },
                        supportingContent = { Text("Restore from a previous backup") }
                    )
                }
            }
        }
    }

    if (showRestoreDialog && restoreUri != null) {
        AlertDialog(
            onDismissRequest = { 
                showRestoreDialog = false 
                restoreUri = null 
            },
            title = { Text("Restore Backup", fontWeight = FontWeight.Bold) },
            text = { Text("How would you like to restore this backup?\n\nMerge: Keeps your existing data and adds the backup data.\n\nReplace: Deletes all your current data and replaces it with the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    val uri = restoreUri!!
                    showRestoreDialog = false
                    restoreUri = null
                    scope.launch {
                        val success = BackupManagerUtil.restoreBackup(context, uri, isReplace = true)
                        snackbarMessage = if (success) "Backup replaced successfully" else "Failed to restore backup"
                    }
                }) {
                    Text("Replace")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val uri = restoreUri!!
                    showRestoreDialog = false
                    restoreUri = null
                    scope.launch {
                        val success = BackupManagerUtil.restoreBackup(context, uri, isReplace = false)
                        snackbarMessage = if (success) "Backup merged successfully" else "Failed to restore backup"
                    }
                }) {
                    Text("Merge")
                }
            },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("system" to "System Default", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                        Surface(
                            onClick = {
                                themeMode = mode
                                prefs.edit().putString("pref_theme_mode", mode).apply()
                                showThemeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == mode,
                                    onClick = null // Handled by Surface
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(label)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
    }

    if (showStyleDialog) {
        AlertDialog(
            onDismissRequest = { showStyleDialog = false },
            title = { Text("Widget Style", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Material 3", "Glass", "Glass+", "AMOLED", "Minimal").forEach { style ->
                        Surface(
                            onClick = {
                                widgetStyle = style
                                prefs.edit().putString("pref_widget_style", style).apply()
                                showStyleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(selected = widgetStyle == style, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(style)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showStyleDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
    }

    if (showAnimationDialog) {
        AlertDialog(
            onDismissRequest = { showAnimationDialog = false },
            title = { Text("Widget Animation", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Off", "Smooth", "Premium").forEach { animation ->
                        Surface(
                            onClick = {
                                widgetAnimation = animation
                                prefs.edit().putString("pref_widget_animation", animation).apply()
                                showAnimationDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(selected = widgetAnimation == animation, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(animation)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAnimationDialog = false }) { Text("Cancel") } },
            shape = RoundedCornerShape(cornerRadius.dp)
        )
    }
}

@Composable
fun WidgetPreview(
    style: String,
    cornerRadius: Float,
    scale: Float,
    depth: Float,
    opacity: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        val backgroundColor = when (style) {
            "AMOLED" -> androidx.compose.ui.graphics.Color.Black
            "Minimal" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            "Glass", "Glass+" -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.primaryContainer
        }
        
        Card(
            modifier = Modifier
                .width(200.dp * scale)
                .height(120.dp * scale),
            shape = RoundedCornerShape(cornerRadius.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor.copy(alpha = backgroundColor.alpha * opacity)),
            elevation = CardDefaults.cardElevation(defaultElevation = (depth * 8).dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp * scale),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Session Name", style = MaterialTheme.typography.labelSmall, fontSize = (12 * scale).sp)
                Text("33", style = MaterialTheme.typography.displayMedium, fontSize = (32 * scale).sp, color = if (style == "AMOLED") androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.primary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Box(Modifier.size(32.dp * scale).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp * scale)), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("-") }
                    Box(Modifier.size(32.dp * scale).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp * scale)), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("+", color = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(24.dp, 24.dp, 16.dp, 8.dp)
    )
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
fun SliderPreference(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp, 8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
