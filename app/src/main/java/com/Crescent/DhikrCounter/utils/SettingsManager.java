package com.Crescent.DhikrCounter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

public class SettingsManager {
    private SharedPreferences prefs;

    // --- GLASS CALIBRATED STABLE DEFAULTS ---
    public static final String DEFAULT_THEME_MODE = "system";
    public static final boolean DEFAULT_DYNAMIC_COLORS = true;
    public static final boolean DEFAULT_AMOLED_MODE = false;

    public static final int DEFAULT_GLASS_TINT_COLOR = 0x0AFFFFFF;
    public static final float DEFAULT_GLASS_BLUR_RADIUS = 2.8f;
    public static final float DEFAULT_GLASS_IOR = 1.52f;
    public static final float DEFAULT_GLASS_CORNER_RADIUS = 28f;
    public static final float DEFAULT_GLASS_REFRACTION_HEIGHT = 12f;
    public static final float DEFAULT_GLASS_REFRACTION_AMOUNT = 24f;
    public static final float DEFAULT_GLASS_THICKNESS = 18f;
    public static final String DEFAULT_GLASS_CAPTURE_DOWNSAMPLE = "balanced";
    public static final float DEFAULT_GLASS_NORMAL_STRENGTH = 1.1f;
    public static final float DEFAULT_GLASS_BRIGHTNESS = 1.0f;
    public static final float DEFAULT_GLASS_RIM_INTENSITY = 0.85f;
    public static final float DEFAULT_GLASS_SPECULAR_INTENSITY = 1.0f;
    public static final float DEFAULT_GLASS_SHININESS = 56f;
    public static final float DEFAULT_GLASS_CHROMATIC_ABERRATION = 0.0f;
    public static final float DEFAULT_GLASS_DISPLACEMENT_SCALE = 0.9f;
    public static final float DEFAULT_GLASS_MIN_SMOOTHING = 1.8f;
    public static final float DEFAULT_GLASS_HIGHLIGHT_WIDTH = 3.5f;
    public static final float DEFAULT_GLASS_CAUSTIC_INTENSITY = 0.1f;
    public static final float DEFAULT_GLASS_LIQUID_DOME = 0.7f;
    public static final float DEFAULT_GLASS_FRESNEL_REFLECTION = 1.0f;
    public static final float DEFAULT_GLASS_LENS_REFRACTION_SCALE = 1.0f;
    public static final float DEFAULT_GLASS_LIGHT_DIR_X = -0.5f;
    public static final float DEFAULT_GLASS_LIGHT_DIR_Y = -0.8f;
    public static final float DEFAULT_GLASS_SHADOW_RADIUS = 12f;
    public static final int DEFAULT_GLASS_SHADOW_COLOR = 0x00000000;
    public static final float DEFAULT_GLASS_SHADOW_INTENSITY = 0.18f;
    public static final float DEFAULT_GLASS_SHADOW_SOFTNESS = 0.2f;
    public static final float DEFAULT_GLASS_TRANSMITTANCE = 1.0f;
    public static final boolean DEFAULT_GLASS_ADAPTIVE_TEXT_COLOR = true;
    public static final float DEFAULT_GLASS_SPRING_STIFFNESS = 380f;
    public static final float DEFAULT_GLASS_SPRING_DAMPING = 0.55f;

    public static final float DEFAULT_BUBBLE_CORNER_RADIUS = 28f;
    public static final float DEFAULT_BUBBLE_BLUR_RADIUS = 2.8f;
    public static final float DEFAULT_BUBBLE_REFRACTION_HEIGHT = 12f;
    public static final float DEFAULT_BUBBLE_REFRACTION_AMOUNT = 24f;
    public static final float DEFAULT_BUBBLE_CHROMATIC_ABERRATION = 0.0f;
    public static final float DEFAULT_BUBBLE_VIBRANCY = 1.0f;
    public static final float DEFAULT_BUBBLE_TINT_STRENGTH = 0.75f;
    public static final float DEFAULT_BUBBLE_SCALE = 1.0f;
    public static final boolean DEFAULT_BUBBLE_ADAPTIVE_LUMINANCE = true;
    public static final boolean DEFAULT_BUBBLE_DYNAMIC_COLORS = true;
    public static final int DEFAULT_BUBBLE_ACCENT_COLOR = 0xFF2196F3;

    public static final float DEFAULT_DOCK_CORNER_RADIUS = 32f;
    public static final float DEFAULT_DOCK_BLUR_RADIUS = 8f;
    public static final float DEFAULT_DOCK_REFRACTION_HEIGHT = 24f;
    public static final float DEFAULT_DOCK_REFRACTION_AMOUNT = 24f;
    public static final float DEFAULT_DOCK_CHROMATIC_ABERRATION = 0.01f;

    public static final int DEFAULT_ADAPTIVE_LUMINANCE_INTERVAL = 1000;
    public static final String DEFAULT_APP_FONT_FAMILY = "SF Pro Text";
    public static final String DEFAULT_APP_FONT_WEIGHT = "Regular";
    public static final int DEFAULT_FONT_TINT_FALLBACK_MODE = 0;
    public static final int DEFAULT_FONT_TINT_PALETTE_COLOR = 0xFF6366F1;
    public static final int DEFAULT_FONT_TINT_CUSTOM_COLOR = 0xFF6366F1;

    public static final boolean DEFAULT_HAPTIC_FEEDBACK_ENABLED = true;
    public static final float DEFAULT_HAPTIC_INTENSITY = 1.0f;
    public static final boolean DEFAULT_SOUND_FEEDBACK_ENABLED = false;
    public static final float DEFAULT_SOUND_VOLUME = 1.0f;

    public static final boolean DEFAULT_WAVY_PROGRESS_ENABLED = false;
    public static final float DEFAULT_WAVY_THICKNESS = 8f;
    public static final float DEFAULT_WAVY_TRACK_THICKNESS = 8f;
    public static final float DEFAULT_WAVY_AMPLITUDE = 1.0f;
    public static final float DEFAULT_WAVY_WAVELENGTH = 20f;
    public static final float DEFAULT_WAVY_GAP_SIZE = 4f;
    public static final float DEFAULT_WAVY_WAVE_SPEED = 20f;
    public static final boolean DEFAULT_WAVY_WAVE_SPEED_AUTO = true;
    public static final int DEFAULT_WAVY_COLOR = 0;
    public static final int DEFAULT_WAVY_TRACK_COLOR = 0;

    public static final boolean DEFAULT_COUNT_ANIMATION = true;
    public static final boolean DEFAULT_ALLOW_NEGATIVE = false;
    public static final boolean DEFAULT_CONFIRM_RESET = true;
    public static final boolean DEFAULT_KEEP_AWAKE = false;
    public static final float DEFAULT_CORNER_RADIUS = 24f;

    public static final boolean DEFAULT_WIDGET_THEME_SYNC = true;
    public static final int DEFAULT_WIDGET_LUMINANCE_OVERRIDE = 0;
    public static final int DEFAULT_WIDGET_ACCENT_OVERRIDE = 0xFF6366F1;
    public static final boolean DEFAULT_WIDGET_SHOW_COUNT = true;
    public static final boolean DEFAULT_WIDGET_SHOW_GOAL = true;
    public static final boolean DEFAULT_WIDGET_SHOW_STREAK = true;
    public static final boolean DEFAULT_WIDGET_SHOW_TIMER = false;
    public static final String DEFAULT_WIDGET_TAP_ACTION = "open_app";
    public static final float DEFAULT_WIDGET_BLUR_RADIUS = 8f;
    public static final int DEFAULT_WIDGET_TINT_COLOR = 0xFFFFFFFF;
    public static final float DEFAULT_WIDGET_TINT_INTENSITY = 0.15f;
    public static final float DEFAULT_WIDGET_FONT_SCALE = 1.0f;
    public static final boolean DEFAULT_WIDGET_AUTO_FIT = true;
    public static final String DEFAULT_WIDGET_B_CATEGORY = "all";

    public SettingsManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // General
    public String getThemeMode() { return prefs.getString("pref_theme_mode", DEFAULT_THEME_MODE); }
    public boolean isDynamicColorsEnabled() { return prefs.getBoolean("pref_dynamic_colors", DEFAULT_DYNAMIC_COLORS); }
    public boolean isAmoledModeEnabled() { return prefs.getBoolean("pref_amoled_mode", DEFAULT_AMOLED_MODE); }

    // UI Customization
    public float getCornerRadius() { return getSafeFloat("pref_corner_radius", DEFAULT_CORNER_RADIUS); }
    public boolean isWavyProgressEnabled() { return prefs.getBoolean("pref_wavy_progress", DEFAULT_WAVY_PROGRESS_ENABLED); }
    public float getWavyThickness() { return getSafeFloat("pref_wavy_thickness", DEFAULT_WAVY_THICKNESS); }
    public float getWavyTrackThickness() { return getSafeFloat("pref_wavy_track_thickness", DEFAULT_WAVY_TRACK_THICKNESS); }
    public float getWavyAmplitude() { return getSafeFloat("pref_wavy_amplitude", DEFAULT_WAVY_AMPLITUDE); }
    public float getWavyWavelength() { return getSafeFloat("pref_wavy_wavelength", DEFAULT_WAVY_WAVELENGTH); }
    public float getWavyGapSize() { return getSafeFloat("pref_wavy_gap_size", DEFAULT_WAVY_GAP_SIZE); }
    public float getWavyWaveSpeed() { return getSafeFloat("pref_wavy_wave_speed", DEFAULT_WAVY_WAVE_SPEED); }
    public boolean isWavyWaveSpeedAuto() { return prefs.getBoolean("pref_wavy_wave_speed_auto", DEFAULT_WAVY_WAVE_SPEED_AUTO); }
    public int getWavyColor() { return getSafeInt("pref_wavy_color", DEFAULT_WAVY_COLOR); }
    public int getWavyTrackColor() { return getSafeInt("pref_wavy_track_color", DEFAULT_WAVY_TRACK_COLOR); }

    public void setWavyThickness(float value) { prefs.edit().putFloat("pref_wavy_thickness", value).apply(); }
    public void setWavyTrackThickness(float value) { prefs.edit().putFloat("pref_wavy_track_thickness", value).apply(); }
    public void setWavyAmplitude(float value) { prefs.edit().putFloat("pref_wavy_amplitude", value).apply(); }
    public void setWavyWavelength(float value) { prefs.edit().putFloat("pref_wavy_wavelength", value).apply(); }
    public void setWavyGapSize(float value) { prefs.edit().putFloat("pref_wavy_gap_size", value).apply(); }
    public void setWavyWaveSpeed(float value) { prefs.edit().putFloat("pref_wavy_wave_speed", value).apply(); }
    public void setWavyWaveSpeedAuto(boolean value) { prefs.edit().putBoolean("pref_wavy_wave_speed_auto", value).apply(); }
    public void setWavyColor(int color) { prefs.edit().putInt("pref_wavy_color", color).apply(); }
    public void setWavyTrackColor(int color) { prefs.edit().putInt("pref_wavy_track_color", color).apply(); }

    public void resetWavyAppearance() {
        prefs.edit()
            .remove("pref_wavy_thickness")
            .remove("pref_wavy_track_thickness")
            .remove("pref_wavy_amplitude")
            .remove("pref_wavy_wavelength")
            .remove("pref_wavy_gap_size")
            .remove("pref_wavy_wave_speed")
            .remove("pref_wavy_wave_speed_auto")
            .remove("pref_wavy_color")
            .remove("pref_wavy_track_color")
            .apply();
    }

    // GETTERS
    public int getGlassTintColor() { return getSafeInt("pref_glass_tint_color", DEFAULT_GLASS_TINT_COLOR); }
    public float getGlassBlurRadius() { return getSafeFloat("pref_glass_blur_radius", DEFAULT_GLASS_BLUR_RADIUS); }
    public float getGlassIOR() { return getSafeFloat("pref_glass_ior", DEFAULT_GLASS_IOR); }
    public float getGlassCornerRadius() { return getSafeFloat("pref_glass_corner_radius", DEFAULT_GLASS_CORNER_RADIUS); }
    public float getGlassRefractionHeight() { return getSafeFloat("pref_glass_refraction_height", DEFAULT_GLASS_REFRACTION_HEIGHT); }
    public float getGlassRefractionAmount() { return getSafeFloat("pref_glass_refraction_amount", DEFAULT_GLASS_REFRACTION_AMOUNT); }
    public float getGlassThickness() { return getSafeFloat("pref_glass_thickness", DEFAULT_GLASS_THICKNESS); }
    public float getGlassNormalStrength() { return getSafeFloat("pref_glass_normal_strength", DEFAULT_GLASS_NORMAL_STRENGTH); }
    public float getGlassBrightness() { return getSafeFloat("pref_glass_brightness", DEFAULT_GLASS_BRIGHTNESS); }
    public float getGlassRimIntensity() { return getSafeFloat("pref_glass_rim_intensity", DEFAULT_GLASS_RIM_INTENSITY); }
    public float getGlassSpecularIntensity() { return getSafeFloat("pref_glass_specular_intensity", DEFAULT_GLASS_SPECULAR_INTENSITY); }
    public float getGlassShininess() { return getSafeFloat("pref_glass_shininess", DEFAULT_GLASS_SHININESS); }
    public float getGlassChromaticAberration() {
        float val = getSafeFloat("pref_glass_chromatic_aberration", DEFAULT_GLASS_CHROMATIC_ABERRATION);
        if (val > 1.0f) {
            val = val / 100f;
            prefs.edit().putFloat("pref_glass_chromatic_aberration", val).apply();
        }
        return val;
    }
    public float getGlassDisplacementScale() { return getSafeFloat("pref_glass_displacement_scale", DEFAULT_GLASS_DISPLACEMENT_SCALE); }
    public float getGlassMinSmoothing() { return getSafeFloat("pref_glass_min_smoothing", DEFAULT_GLASS_MIN_SMOOTHING); }
    public float getGlassHighlightWidth() { return getSafeFloat("pref_glass_highlight_width", DEFAULT_GLASS_HIGHLIGHT_WIDTH); }
    public float getGlassCausticIntensity() { return getSafeFloat("pref_glass_caustic_intensity", DEFAULT_GLASS_CAUSTIC_INTENSITY); }
    public float getGlassLiquidDome() { return getSafeFloat("pref_glass_liquid_dome", DEFAULT_GLASS_LIQUID_DOME); }
    public float getGlassFresnelReflection() { return getSafeFloat("pref_glass_fresnel_reflection", DEFAULT_GLASS_FRESNEL_REFLECTION); }
    public float getGlassLensRefractionScale() { return getSafeFloat("pref_glass_lens_refraction_scale", DEFAULT_GLASS_LENS_REFRACTION_SCALE); }
    public float getGlassLightDirX() { return getSafeFloat("pref_glass_light_dir_x", DEFAULT_GLASS_LIGHT_DIR_X); }
    public float getGlassLightDirY() { return getSafeFloat("pref_glass_light_dir_y", DEFAULT_GLASS_LIGHT_DIR_Y); }
    public float getGlassShadowRadius() { return getSafeFloat("pref_glass_shadow_radius", DEFAULT_GLASS_SHADOW_RADIUS); }
    public int getGlassShadowColor() { return getSafeInt("pref_glass_shadow_color", DEFAULT_GLASS_SHADOW_COLOR); }
    public float getGlassShadowIntensity() { return getSafeFloat("pref_glass_shadow_intensity", DEFAULT_GLASS_SHADOW_INTENSITY); }
    public float getGlassShadowSoftness() { return getSafeFloat("pref_glass_shadow_softness", DEFAULT_GLASS_SHADOW_SOFTNESS); }
    public float getGlassTransmittance() { return getSafeFloat("pref_glass_transmittance", DEFAULT_GLASS_TRANSMITTANCE); }
    public String getGlassCaptureDownsample() { return prefs.getString("pref_glass_capture_downsample", DEFAULT_GLASS_CAPTURE_DOWNSAMPLE); }
    public boolean isGlassAdaptiveTextColorEnabled() { return prefs.getBoolean("pref_glass_adaptive_text_color", DEFAULT_GLASS_ADAPTIVE_TEXT_COLOR); }
    public float getGlassSpringStiffness() { return getSafeFloat("pref_glass_spring_stiffness", DEFAULT_GLASS_SPRING_STIFFNESS); }
    public float getGlassSpringDamping() { return getSafeFloat("pref_glass_spring_damping", DEFAULT_GLASS_SPRING_DAMPING); }

    public void setGlassBrightness(float value) { prefs.edit().putFloat("pref_glass_brightness", value).apply(); }
    public void setGlassBlurRadius(float value) { prefs.edit().putFloat("pref_glass_blur_radius", value).apply(); }
    public void setGlassCornerRadius(float value) { prefs.edit().putFloat("pref_glass_corner_radius", value).apply(); }
    public void setGlassRefractionHeight(float value) { prefs.edit().putFloat("pref_glass_refraction_height", value).apply(); }
    public void setGlassRefractionAmount(float value) { prefs.edit().putFloat("pref_glass_refraction_amount", value).apply(); }
    public void setGlassChromaticAberration(float value) { prefs.edit().putFloat("pref_glass_chromatic_aberration", value).apply(); }

    // Counter Settings
    public boolean isCountAnimationEnabled() { return prefs.getBoolean("pref_count_animation", DEFAULT_COUNT_ANIMATION); }
    public boolean isNegativeCountAllowed() { return prefs.getBoolean("pref_allow_negative", DEFAULT_ALLOW_NEGATIVE); }
    public boolean isConfirmBeforeReset() { return prefs.getBoolean("pref_confirm_reset", DEFAULT_CONFIRM_RESET); }
    public boolean isKeepScreenAwake() { return prefs.getBoolean("pref_keep_awake", DEFAULT_KEEP_AWAKE); }

    // Haptics & Sound
    public boolean isHapticFeedbackEnabled() { return prefs.getBoolean("pref_haptic_feedback", DEFAULT_HAPTIC_FEEDBACK_ENABLED); }
    public float getHapticIntensity() { return getSafeFloat("pref_haptic_intensity", DEFAULT_HAPTIC_INTENSITY); }
    public boolean isSoundFeedbackEnabled() { return prefs.getBoolean("pref_sound_feedback", DEFAULT_SOUND_FEEDBACK_ENABLED); }
    public float getSoundVolume() { return getSafeFloat("pref_sound_volume", DEFAULT_SOUND_VOLUME); }

    // Floating Bubble Appearance
    public boolean isFloatingBubbleEnabled() { return prefs.getBoolean("pref_floating_enabled", false); }
    public boolean isBubbleSnapEnabled() { return prefs.getBoolean("pref_bubble_snap", true); }

    public float getBubbleCornerRadius() { return getSafeFloat("pref_bubble_corner_radius", DEFAULT_BUBBLE_CORNER_RADIUS); }
    public float getBubbleBlurRadius() { return getSafeFloat("pref_bubble_blur_radius", DEFAULT_BUBBLE_BLUR_RADIUS); }
    public float getBubbleRefractionHeight() { return getSafeFloat("pref_bubble_refraction_height", DEFAULT_BUBBLE_REFRACTION_HEIGHT); }
    public float getBubbleRefractionAmount() { return getSafeFloat("pref_bubble_refraction_amount", DEFAULT_BUBBLE_REFRACTION_AMOUNT); }
    public float getBubbleChromaticAberration() {
        float val = getSafeFloat("pref_bubble_chromatic_aberration", DEFAULT_BUBBLE_CHROMATIC_ABERRATION);
        if (val > 1.0f) {
            val = val / 100f;
            prefs.edit().putFloat("pref_bubble_chromatic_aberration", val).apply();
        }
        return val;
    }
    public float getBubbleVibrancy() { return getSafeFloat("pref_bubble_vibrancy", DEFAULT_BUBBLE_VIBRANCY); }
    public float getBubbleTintStrength() { return getSafeFloat("pref_bubble_tint_strength", DEFAULT_BUBBLE_TINT_STRENGTH); }
    public float getBubbleScale() { return getSafeFloat("pref_bubble_scale", DEFAULT_BUBBLE_SCALE); }
    public boolean isBubbleAdaptiveLuminanceEnabled() { return prefs.getBoolean("pref_bubble_adaptive_luminance", DEFAULT_BUBBLE_ADAPTIVE_LUMINANCE); }
    public boolean isBubbleDynamicColorsEnabled() { return prefs.getBoolean("pref_bubble_dynamic_colors", DEFAULT_BUBBLE_DYNAMIC_COLORS); }
    public int getBubbleAccentColor() { return getSafeInt("pref_bubble_accent_color", DEFAULT_BUBBLE_ACCENT_COLOR); }

    public void setBubbleCornerRadius(float value) { prefs.edit().putFloat("pref_bubble_corner_radius", value).apply(); }
    public void setBubbleBlurRadius(float value) { prefs.edit().putFloat("pref_bubble_blur_radius", value).apply(); }
    public void setBubbleRefractionHeight(float value) { prefs.edit().putFloat("pref_bubble_refraction_height", value).apply(); }
    public void setBubbleRefractionAmount(float value) { prefs.edit().putFloat("pref_bubble_refraction_amount", value).apply(); }
    public void setBubbleChromaticAberration(float value) { prefs.edit().putFloat("pref_bubble_chromatic_aberration", value).apply(); }
    public void setBubbleVibrancy(float value) { prefs.edit().putFloat("pref_bubble_vibrancy", value).apply(); }
    public void setBubbleTintStrength(float value) { prefs.edit().putFloat("pref_bubble_tint_strength", value).apply(); }
    public void setBubbleScale(float value) { prefs.edit().putFloat("pref_bubble_scale", value).apply(); }
    public void setBubbleAdaptiveLuminanceEnabled(boolean enabled) { prefs.edit().putBoolean("pref_bubble_adaptive_luminance", enabled).apply(); }
    public void setBubbleDynamicColorsEnabled(boolean enabled) { prefs.edit().putBoolean("pref_bubble_dynamic_colors", enabled).apply(); }
    public void setBubbleAccentColor(int color) { prefs.edit().putInt("pref_bubble_accent_color", color).apply(); }

    public void resetBubbleAppearance() {
        prefs.edit()
            .remove("pref_bubble_corner_radius")
            .remove("pref_bubble_blur_radius")
            .remove("pref_bubble_refraction_height")
            .remove("pref_bubble_refraction_amount")
            .remove("pref_bubble_chromatic_aberration")
            .remove("pref_bubble_vibrancy")
            .remove("pref_bubble_tint_strength")
            .remove("pref_bubble_scale")
            .remove("pref_bubble_adaptive_luminance")
            .remove("pref_bubble_dynamic_colors")
            .remove("pref_bubble_accent_color")
            .apply();
    }

    // Dock Settings
    public float getDockCornerRadius() { return getSafeFloat("pref_dock_corner_radius", DEFAULT_DOCK_CORNER_RADIUS); }
    public float getDockBlurRadius() { return getSafeFloat("pref_dock_blur_radius", DEFAULT_DOCK_BLUR_RADIUS); }
    public float getDockRefractionHeight() { return getSafeFloat("pref_dock_refraction_height", DEFAULT_DOCK_REFRACTION_HEIGHT); }
    public float getDockRefractionAmount() { return getSafeFloat("pref_dock_refraction_amount", DEFAULT_DOCK_REFRACTION_AMOUNT); }
    public float getDockChromaticAberration() {
        float val = getSafeFloat("pref_dock_chromatic_aberration", DEFAULT_DOCK_CHROMATIC_ABERRATION);
        if (val > 1.0f) {
            val = val / 100f;
            prefs.edit().putFloat("pref_dock_chromatic_aberration", val).apply();
        }
        return val;
    }

    public void setDockCornerRadius(float value) { prefs.edit().putFloat("pref_dock_corner_radius", value).apply(); }
    public void setDockBlurRadius(float value) { prefs.edit().putFloat("pref_dock_blur_radius", value).apply(); }
    public void setDockRefractionHeight(float value) { prefs.edit().putFloat("pref_dock_refraction_height", value).apply(); }
    public void setDockRefractionAmount(float value) { prefs.edit().putFloat("pref_dock_refraction_amount", value).apply(); }
    public void setDockChromaticAberration(float value) { prefs.edit().putFloat("pref_dock_chromatic_aberration", value).apply(); }

    // Custom Wallpaper
    public String getAppWallpaperUri() { return prefs.getString("pref_app_wallpaper_uri", ""); }
    public void setAppWallpaperUri(String uri) { prefs.edit().putString("pref_app_wallpaper_uri", uri).apply(); }

    public long getActiveSessionId() {
        return getSafeLong("active_session_id", -1L);
    }

    public long getLastBackupTimestamp() {
        return getSafeLong("last_backup_timestamp", 0L);
    }

    private long getSafeLong(String key, long defaultValue) {
        try {
            return prefs.getLong(key, defaultValue);
        } catch (ClassCastException e) {
            Object val = prefs.getAll().get(key);
            if (val instanceof Number) {
                long recovered = ((Number) val).longValue();
                prefs.edit().putLong(key, recovered).apply();
                return recovered;
            }
            return defaultValue;
        }
    }

    private int getSafeInt(String key, int defaultValue) {
        try {
            return prefs.getInt(key, defaultValue);
        } catch (ClassCastException e) {
            Object val = prefs.getAll().get(key);
            if (val instanceof Number) {
                int recovered = ((Number) val).intValue();
                prefs.edit().putInt(key, recovered).apply();
                return recovered;
            }
            return defaultValue;
        }
    }

    private float getSafeFloat(String key, float defaultValue) {
        try {
            return prefs.getFloat(key, defaultValue);
        } catch (ClassCastException e) {
            Object val = prefs.getAll().get(key);
            if (val instanceof Number) {
                float recovered = ((Number) val).floatValue();
                prefs.edit().putFloat(key, recovered).apply();
                return recovered;
            }
            return defaultValue;
        }
    }
    public void setActiveSessionId(long id) { prefs.edit().putLong("active_session_id", id).apply(); }

    public int getFontTintFallbackMode() { return getSafeInt("pref_font_tint_fallback_mode", DEFAULT_FONT_TINT_FALLBACK_MODE); }
    public void setFontTintFallbackMode(int mode) { prefs.edit().putInt("pref_font_tint_fallback_mode", mode).apply(); }

    public int getFontTintPaletteColor() { return getSafeInt("pref_font_tint_palette_color", DEFAULT_FONT_TINT_PALETTE_COLOR); }
    public void setFontTintPaletteColor(int color) { prefs.edit().putInt("pref_font_tint_palette_color", color).apply(); }

    public int getFontTintCustomColor() { return getSafeInt("pref_font_tint_custom_color", DEFAULT_FONT_TINT_CUSTOM_COLOR); }
    public void setFontTintCustomColor(int color) { prefs.edit().putInt("pref_font_tint_custom_color", color).apply(); }

    public int getFontTintAutoColor() { return getSafeInt("pref_font_tint_auto_color", -1); }
    public void setFontTintAutoColor(int color) { prefs.edit().putInt("pref_font_tint_auto_color", color).apply(); }

    public String getAppFontFamily() { return prefs.getString("pref_app_font_family", DEFAULT_APP_FONT_FAMILY); }
    public void setAppFontFamily(String value) { prefs.edit().putString("pref_app_font_family", value).apply(); }

    public String getAppFontWeight() { return prefs.getString("pref_app_font_weight", DEFAULT_APP_FONT_WEIGHT); }
    public void setAppFontWeight(String value) { prefs.edit().putString("pref_app_font_weight", value).apply(); }

    public int getAdaptiveLuminanceInterval() { return getSafeInt("pref_adaptive_luminance_interval", DEFAULT_ADAPTIVE_LUMINANCE_INTERVAL); }
    public void setAdaptiveLuminanceInterval(int value) { prefs.edit().putInt("pref_adaptive_luminance_interval", value).apply(); }

    public SharedPreferences getPrefs() { return prefs; }

    public String getWidgetStyle() { return prefs.getString("pref_widget_style", "Material 3"); }
    public float getWidgetCornerRadius() { return prefs.getFloat("pref_widget_corner_radius", getCornerRadius()); }
    public float getWidgetScale() { return prefs.getFloat("pref_widget_scale", 1.0f); }
    public float getWidgetDepth() { return prefs.getFloat("pref_widget_depth", 0.5f); }
    public float getWidgetOpacity() { return prefs.getFloat("pref_widget_opacity", 1.0f); }
    public String getWidgetAnimation() { return prefs.getString("pref_widget_animation", "Smooth"); }

    public boolean isWidgetThemeSyncEnabled() { return prefs.getBoolean("pref_widget_theme_sync", DEFAULT_WIDGET_THEME_SYNC); }
    public void setWidgetThemeSyncEnabled(boolean enabled) { prefs.edit().putBoolean("pref_widget_theme_sync", enabled).apply(); }

    public int getWidgetLuminanceOverride() { return getSafeInt("pref_widget_luminance_override", DEFAULT_WIDGET_LUMINANCE_OVERRIDE); }
    public void setWidgetLuminanceOverride(int value) { prefs.edit().putInt("pref_widget_luminance_override", value).apply(); }

    public int getWidgetAccentOverride() { return getSafeInt("pref_widget_accent_override", DEFAULT_WIDGET_ACCENT_OVERRIDE); }
    public void setWidgetAccentOverride(int color) { prefs.edit().putInt("pref_widget_accent_override", color).apply(); }

    public boolean isWidgetShowCount() { return prefs.getBoolean("pref_widget_show_count", DEFAULT_WIDGET_SHOW_COUNT); }
    public void setWidgetShowCount(boolean enabled) { prefs.edit().putBoolean("pref_widget_show_count", enabled).apply(); }

    public boolean isWidgetShowGoal() { return prefs.getBoolean("pref_widget_show_goal", DEFAULT_WIDGET_SHOW_GOAL); }
    public void setWidgetShowGoal(boolean enabled) { prefs.edit().putBoolean("pref_widget_show_goal", enabled).apply(); }

    public boolean isWidgetShowStreak() { return prefs.getBoolean("pref_widget_show_streak", DEFAULT_WIDGET_SHOW_STREAK); }
    public void setWidgetShowStreak(boolean enabled) { prefs.edit().putBoolean("pref_widget_show_streak", enabled).apply(); }

    public boolean isWidgetShowTimer() { return prefs.getBoolean("pref_widget_show_timer", DEFAULT_WIDGET_SHOW_TIMER); }
    public void setWidgetShowTimer(boolean enabled) { prefs.edit().putBoolean("pref_widget_show_timer", enabled).apply(); }

    public String getWidgetTapAction() { return prefs.getString("pref_widget_tap_action", DEFAULT_WIDGET_TAP_ACTION); }
    public void setWidgetTapAction(String value) { prefs.edit().putString("pref_widget_tap_action", value).apply(); }

    public float getWidgetBlurRadius() { return getSafeFloat("pref_widget_blur_radius", DEFAULT_WIDGET_BLUR_RADIUS); }
    public void setWidgetBlurRadius(float value) { prefs.edit().putFloat("pref_widget_blur_radius", value).apply(); }

    public int getWidgetTintColor() { return getSafeInt("pref_widget_tint_color", DEFAULT_WIDGET_TINT_COLOR); }
    public void setWidgetTintColor(int color) { prefs.edit().putInt("pref_widget_tint_color", color).apply(); }

    public float getWidgetTintIntensity() { return getSafeFloat("pref_widget_tint_intensity", DEFAULT_WIDGET_TINT_INTENSITY); }
    public void setWidgetTintIntensity(float value) { prefs.edit().putFloat("pref_widget_tint_intensity", value).apply(); }

    public float getWidgetFontScale() { return getSafeFloat("pref_widget_font_scale", DEFAULT_WIDGET_FONT_SCALE); }
    public void setWidgetFontScale(float value) { prefs.edit().putFloat("pref_widget_font_scale", value).apply(); }

    public boolean isWidgetAutoFitEnabled() { return prefs.getBoolean("pref_widget_auto_fit", DEFAULT_WIDGET_AUTO_FIT); }
    public void setWidgetAutoFitEnabled(boolean enabled) { prefs.edit().putBoolean("pref_widget_auto_fit", enabled).apply(); }

    public String getWidgetBCategory() { return prefs.getString("pref_widget_b_category", DEFAULT_WIDGET_B_CATEGORY); }
    public void setWidgetBCategory(String category) { prefs.edit().putString("pref_widget_b_category", category).apply(); }

    public void resetWidgetAppearance() {
        prefs.edit()
            .remove("pref_widget_theme_sync")
            .remove("pref_widget_luminance_override")
            .remove("pref_widget_accent_override")
            .remove("pref_widget_show_count")
            .remove("pref_widget_show_goal")
            .remove("pref_widget_show_streak")
            .remove("pref_widget_show_timer")
            .remove("pref_widget_tap_action")
            .remove("pref_widget_blur_radius")
            .remove("pref_widget_tint_color")
            .remove("pref_widget_tint_intensity")
            .remove("pref_widget_font_scale")
            .remove("pref_widget_auto_fit")
            .remove("pref_widget_b_category")
            .apply();
    }
}
