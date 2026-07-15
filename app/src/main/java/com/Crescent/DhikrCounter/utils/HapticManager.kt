package com.Crescent.DhikrCounter.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.Crescent.DhikrCounter.DhikrApplication

/**
 * SHARED HAPTIC MANAGER (Pass 36)
 * Centralizes all haptic feedback logic to respect global intensity settings.
 */
class HapticManager(private val context: Context) {
    private val settingsManager: SettingsManager by lazy {
        (context.applicationContext as DhikrApplication).settingsManager
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Performs haptic feedback based on the current intensity setting.
     * @param baseDuration The base duration of the vibration in ms.
     * @param intensityOverride Optional override for the intensity multiplier.
     */
    fun vibrate(baseDuration: Long = 50L, intensityOverride: Float? = null) {
        val enabled = settingsManager.isHapticFeedbackEnabled()
        val intensity = intensityOverride ?: settingsManager.getHapticIntensity()

        if (!enabled || intensity <= 0f || vibrator == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Scale amplitude if supported. We use 255 as the reference maximum.
            val amplitude = (255 * intensity).toInt().coerceIn(1, 255)
            try {
                vibrator?.vibrate(VibrationEffect.createOneShot(baseDuration, amplitude))
            } catch (e: Exception) {
                // Fallback for devices that don't support amplitude control well
                @Suppress("DEPRECATION")
                vibrator?.vibrate((baseDuration * intensity).toLong().coerceAtLeast(1L))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate((baseDuration * intensity).toLong().coerceAtLeast(1L))
        }
    }

    /**
     * Specialized vibration for subtle interactions (detents, small clicks).
     */
    fun vibrateSubtle() {
        vibrate(30L, settingsManager.getHapticIntensity() * 0.5f)
    }

    /**
     * Specialized vibration for strong events (goal reached, long press).
     */
    fun vibrateStrong() {
        vibrate(80L, settingsManager.getHapticIntensity() * 1.5f)
    }
}
