package com.Crescent.DhikrCounter.utils;

import android.graphics.BlendMode;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

public class GlassUtils {

    /**
     * Applies a hardware-accelerated "Liquid Glass" blur effect to a View.
     * This effect uses RenderEffect which is available on API 31+.
     * 
     * @param view The view to apply the blur to.
     * @param radius The blur radius (e.g., 40f to 80f for strong depth).
     * @param tintColor The translucent tint color (e.g., #88FFFFFF for light, #88121212 for dark).
     */
    public static void applyBlur(View view, float radius, int tintColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffect blurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP);
            android.graphics.BlendModeColorFilter filter = new android.graphics.BlendModeColorFilter(tintColor, BlendMode.SRC_OVER);
            RenderEffect colorEffect = RenderEffect.createColorFilterEffect(filter);
            RenderEffect blendEffect = RenderEffect.createBlendModeEffect(blurEffect, colorEffect, BlendMode.SRC_OVER);
            view.setRenderEffect(blendEffect);
            // Ensure clipping to outline so blur doesn't bleed outside the rounded corners
            view.setClipToOutline(true);
        }
    }
}
