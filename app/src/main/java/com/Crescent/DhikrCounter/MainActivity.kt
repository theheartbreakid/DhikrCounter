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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.Crescent.DhikrCounter.ui.theme.DhikrTheme
import com.Crescent.DhikrCounter.ui.AppNavigation
import com.Crescent.DhikrCounter.service.FloatingCounterService

class MainActivity : ComponentActivity() {
    
    private lateinit var settingsManager: com.Crescent.DhikrCounter.utils.SettingsManager
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val viewModel: com.Crescent.DhikrCounter.ui.CounterViewModel by lazy {
        ViewModelProvider(this)[com.Crescent.DhikrCounter.ui.CounterViewModel::class.java]
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
            if (key == "pref_floating_enabled") {
                updateFloatingService()
            }
        }
        settingsManager.prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // On first app launch, automatically check for "Display over other apps" permission
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

        // Sync FloatingCounterService state
        updateFloatingService()
        
        setContent {
            val prefs = remember { settingsManager.prefs }
            
            var themeMode by remember { mutableStateOf(settingsManager.themeMode) }
            var isDynamicColors by remember { mutableStateOf(settingsManager.isDynamicColorsEnabled) }
            var isAmoled by remember { mutableStateOf(settingsManager.isAmoledModeEnabled) }

            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
                    when (key) {
                        "pref_theme_mode" -> themeMode = p.getString("pref_theme_mode", "system") ?: "system"
                        "pref_dynamic_colors" -> isDynamicColors = p.getBoolean("pref_dynamic_colors", true)
                        "pref_amoled_mode" -> isAmoled = p.getBoolean("pref_amoled_mode", false)
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
            
            DhikrTheme(
                darkTheme = darkTheme,
                dynamicColor = isDynamicColors,
                amoledMode = isAmoled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
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
            // We don't auto-disable here anymore, to allow the user to grant permission and return
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
        // Re-check permission
        if (settingsManager.isFloatingBubbleEnabled && !Settings.canDrawOverlays(this)) {
            // Permission was denied or revoked, disable the setting
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
