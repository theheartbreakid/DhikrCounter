package com.Crescent.DhikrCounter.ui.components.AppleFonts

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.Crescent.DhikrCounter.R

val SfProText = FontFamily(
    Font(R.font.sf_pro_text_thin, FontWeight.Thin),
    Font(R.font.sf_pro_text_light, FontWeight.Light),
    Font(R.font.sf_pro_text_regular, FontWeight.Normal),
    Font(R.font.sf_pro_text_medium, FontWeight.Medium),
    Font(R.font.sf_pro_text_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_text_bold, FontWeight.Bold),
    Font(R.font.sf_pro_text_heavy, FontWeight.ExtraBold)
)

val SfProRounded = FontFamily(
    Font(R.font.sf_pro_rounded_thin, FontWeight.Thin),
    Font(R.font.sf_pro_rounded_light, FontWeight.Light),
    Font(R.font.sf_pro_rounded_regular, FontWeight.Normal),
    Font(R.font.sf_pro_rounded_medium, FontWeight.Medium),
    Font(R.font.sf_pro_rounded_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_rounded_bold, FontWeight.Bold),
    Font(R.font.sf_pro_rounded_black, FontWeight.Black)
)

enum class AppleFontStyle(val label: String, val fontFamily: FontFamily) {
    TEXT("SF Pro Text", SfProText),
    ROUNDED("SF Pro Rounded", SfProRounded);

    companion object {
        fun fromLabel(label: String): AppleFontStyle = values().find { it.label == label } ?: TEXT
    }
}

enum class AppleFontWeight(val label: String, val weight: FontWeight) {
    THIN("Thin", FontWeight.Thin),
    LIGHT("Light", FontWeight.Light),
    REGULAR("Regular", FontWeight.Normal),
    MEDIUM("Medium", FontWeight.Medium),
    SEMIBOLD("Semibold", FontWeight.SemiBold),
    BOLD("Bold", FontWeight.Bold),
    HEAVY("Heavy", FontWeight.ExtraBold),
    BLACK("Black", FontWeight.Black);

    companion object {
        fun fromLabel(label: String): AppleFontWeight = values().find { it.label == label } ?: REGULAR
    }
}
