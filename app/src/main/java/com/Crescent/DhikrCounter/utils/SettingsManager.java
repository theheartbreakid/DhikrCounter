package com.Crescent.DhikrCounter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class SettingsManager {
    private SharedPreferences prefs;

    public SettingsManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // General
    public String getThemeMode() { return prefs.getString("pref_theme_mode", "system"); }
    public boolean isDynamicColorsEnabled() { return prefs.getBoolean("pref_dynamic_colors", true); }
    public boolean isAmoledModeEnabled() { return prefs.getBoolean("pref_amoled_mode", false); }

    // UI Customization
    public float getCornerRadius() { return prefs.getFloat("pref_corner_radius", 24f); }

    // Widget Appearance
    public float getWidgetCornerRadius() { return prefs.getFloat("pref_widget_corner_radius", getCornerRadius()); }
    public float getWidgetOpacity() { return prefs.getFloat("pref_widget_opacity", 0.9f); }
    public float getWidgetBlurStrength() { return prefs.getFloat("pref_widget_blur", 0f); }
    public float getGlassTransparency() { return prefs.getFloat("pref_glass_transparency", 0.1f); }
    public float getBackgroundTintIntensity() { return prefs.getFloat("pref_tint_intensity", 0.2f); }
    public float getBorderThickness() { return prefs.getFloat("pref_border_thickness", 1.0f); }
    public float getBorderOpacity() { return prefs.getFloat("pref_border_opacity", 0.5f); }
    public float getShadowIntensity() { return prefs.getFloat("pref_shadow_intensity", 0.0f); }
    public float getWidgetIconSize() { return prefs.getFloat("pref_widget_icon_size", 24f); }
    public float getWidgetFontScale() { return prefs.getFloat("pref_widget_font_scale", 1.0f); }
    public float getProgressRingThickness() { return prefs.getFloat("pref_progress_thickness", 4.0f); }
    public boolean isWidgetCompactMode() { return prefs.getBoolean("pref_widget_compact", false); }
    public boolean isAmoledWidgetEnabled() { return prefs.getBoolean("pref_amoled_widget", false); }

    // Counter Settings
    public boolean isCountAnimationEnabled() { return prefs.getBoolean("pref_count_animation", true); }
    public boolean isNegativeCountAllowed() { return prefs.getBoolean("pref_allow_negative", false); }
    public boolean isConfirmBeforeReset() { return prefs.getBoolean("pref_confirm_reset", true); }
    public boolean isKeepScreenAwake() { return prefs.getBoolean("pref_keep_awake", false); }

    // Haptics & Sound
    public boolean isHapticFeedbackEnabled() { return prefs.getBoolean("pref_haptic_feedback", true); }
    public float getHapticIntensity() { return prefs.getFloat("pref_haptic_intensity", 1.0f); }
    public boolean isSoundFeedbackEnabled() { return prefs.getBoolean("pref_sound_feedback", false); }
    public float getSoundVolume() { return prefs.getFloat("pref_sound_volume", 1.0f); }

    // Floating Bubble
    public boolean isFloatingBubbleEnabled() { return prefs.getBoolean("pref_floating_enabled", false); }
    public float getBubbleOpacity() { return prefs.getFloat("pref_bubble_opacity", 0.8f); }
    public int getBubbleSize() { return prefs.getInt("pref_bubble_size", 64); }

    public long getActiveSessionId() {
        return prefs.getLong("active_session_id", -1L);
    }
    
    public void setActiveSessionId(long id) {
        prefs.edit().putLong("active_session_id", id).apply();
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }
}
