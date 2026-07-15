package com.Crescent.DhikrCounter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.Crescent.DhikrCounter.ui.components.AppleFonts.AppleFontStyle
import com.Crescent.DhikrCounter.ui.components.AppleFonts.AppleFontWeight

private val PremiumDarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF022C22),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF94A3B8)
)

private val AmoledDarkColorScheme = PremiumDarkColorScheme.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF121212)
)

private val PremiumLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF059669),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = Color(0xFFD97706),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF64748B)
)

private fun getTypography(fontFamily: FontFamily, fontWeight: FontWeight): Typography {
    val baseStyle = TextStyle(
        fontFamily = fontFamily,
        fontWeight = fontWeight
    )
    return Typography(
        displayLarge = baseStyle.copy(fontWeight = fontWeight),
        displayMedium = baseStyle.copy(fontWeight = fontWeight),
        displaySmall = baseStyle.copy(fontWeight = fontWeight),
        headlineLarge = baseStyle.copy(fontWeight = fontWeight),
        headlineMedium = baseStyle.copy(fontWeight = fontWeight),
        headlineSmall = baseStyle.copy(fontWeight = fontWeight),
        titleLarge = baseStyle.copy(fontWeight = fontWeight),
        titleMedium = baseStyle.copy(fontWeight = fontWeight),
        titleSmall = baseStyle.copy(fontWeight = fontWeight),
        bodyLarge = baseStyle.copy(fontWeight = fontWeight),
        bodyMedium = baseStyle.copy(fontWeight = fontWeight),
        bodySmall = baseStyle.copy(fontWeight = fontWeight),
        labelLarge = baseStyle.copy(fontWeight = fontWeight),
        labelMedium = baseStyle.copy(fontWeight = fontWeight),
        labelSmall = baseStyle.copy(fontWeight = fontWeight)
    )
}

@Composable
fun DhikrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    appFontFamily: String = "SF Pro Text",
    appFontWeight: String = "Regular",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme && amoledMode -> AmoledDarkColorScheme
        darkTheme -> PremiumDarkColorScheme
        else -> PremiumLightColorScheme
    }

    val selectedFont = AppleFontStyle.fromLabel(appFontFamily).fontFamily
    val selectedWeight = AppleFontWeight.fromLabel(appFontWeight).weight
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(selectedFont, selectedWeight),
        content = content
    )
}
