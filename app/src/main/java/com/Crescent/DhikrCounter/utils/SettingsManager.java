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
    public String getWidgetStyle() { return prefs.getString("pref_widget_style", "Material 3"); }
    public float getWidgetCornerRadius() { return prefs.getFloat("pref_widget_corner_radius", getCornerRadius()); }
    public float getWidgetScale() { return prefs.getFloat("pref_widget_scale", 1.0f); }
    public float getWidgetDepth() { return prefs.getFloat("pref_widget_depth", 0.5f); }
    public float getWidgetOpacity() { return prefs.getFloat("pref_widget_opacity", 1.0f); }
    public String getWidgetAnimation() { return prefs.getString("pref_widget_animation", "Smooth"); }

    // Derived Widget Properties (for backward compatibility if needed, or to be removed later)
    public float getWidgetBlurStrength() {
        String style = getWidgetStyle();
        float depth = getWidgetDepth();
        if (style.equals("Glass")) return 15f * depth;
        if (style.equals("Glass+")) return 25f * depth;
        return 0f;
    }

    public float getGlassTransparency() {
        String style = getWidgetStyle();
        if (style.startsWith("Glass")) return 0.4f;
        return 1.0f;
    }

    public float getBackgroundTintIntensity() {
        String style = getWidgetStyle();
        if (style.equals("Minimal")) return 0.05f;
        if (style.equals("AMOLED")) return 0f;
        return 0.2f;
    }

    public float getBorderThickness() {
        String style = getWidgetStyle();
        if (style.startsWith("Glass")) return 1.0f * getWidgetDepth();
        return 0f;
    }

    public float getBorderOpacity() {
        String style = getWidgetStyle();
        if (style.startsWith("Glass")) return 0.5f * getWidgetDepth();
        return 0f;
    }

    public float getShadowIntensity() {
        return getWidgetDepth() * 0.5f;
    }

    public float getWidgetIconSize() { return 24f * getWidgetScale(); }
    public float getWidgetFontScale() { return getWidgetScale(); }
    public float getProgressRingThickness() { return 4.0f * getWidgetScale(); }
    public boolean isWidgetCompactMode() { return false; } // Handled by scale now
    public boolean isAmoledWidgetEnabled() { return getWidgetStyle().equals("AMOLED"); }

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
