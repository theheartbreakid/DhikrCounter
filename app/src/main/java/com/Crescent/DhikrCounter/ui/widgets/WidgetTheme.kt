package com.Crescent.DhikrCounter.ui.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.padding
import com.Crescent.DhikrCounter.DhikrApplication

@Composable
fun GlanceModifier.applyWidgetStyle(context: Context): GlanceModifier {
    val app = context.applicationContext as DhikrApplication
    val sm = app.settingsManager
    
    val radius = sm.getWidgetCornerRadius()
    val opacity = sm.getWidgetOpacity()
    val isAmoled = sm.isAmoledWidgetEnabled()
    val isCompact = sm.isWidgetCompactMode()
    val tintIntensity = sm.getBackgroundTintIntensity()
    
    val baseColor = if (isAmoled) Color.Black else GlanceTheme.colors.surface.getColor(context)
    val tintColor = GlanceTheme.colors.primary.getColor(context)
    
    val blendedColor = Color(
        red = (baseColor.red * (1 - tintIntensity) + tintColor.red * tintIntensity).coerceIn(0f, 1f),
        green = (baseColor.green * (1 - tintIntensity) + tintColor.green * tintIntensity).coerceIn(0f, 1f),
        blue = (baseColor.blue * (1 - tintIntensity) + tintColor.blue * tintIntensity).coerceIn(0f, 1f),
        alpha = opacity
    )

    var modifier = this
        .background(blendedColor)
        .cornerRadius(radius.dp)
        .appWidgetBackground()
    
    if (isCompact) {
        modifier = modifier.padding(6.dp)
    } else {
        modifier = modifier.padding(12.dp)
    }
    
    return modifier
}

@Composable
fun getWidgetPadding(context: Context): Dp {
    val app = context.applicationContext as DhikrApplication
    return if (app.settingsManager.isWidgetCompactMode()) 4.dp else 8.dp
}

@Composable
fun getWidgetFontSize(context: Context, baseSize: Float): TextUnit {
    val app = context.applicationContext as DhikrApplication
    val scale = app.settingsManager.getWidgetFontScale()
    return (baseSize * scale).sp
}

@Composable
fun getWidgetIconSize(context: Context): Dp {
    val app = context.applicationContext as DhikrApplication
    return app.settingsManager.getWidgetIconSize().dp
}
