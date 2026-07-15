package com.Crescent.DhikrCounter.ui.components.catalog.utils

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing

fun interface ProgressConverter : Easing {

    override fun transform(fraction: Float): Float
}

val LinearProgressConverter = ProgressConverter { it }
