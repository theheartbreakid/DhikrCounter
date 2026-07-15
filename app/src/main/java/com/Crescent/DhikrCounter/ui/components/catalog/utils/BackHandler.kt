package com.Crescent.DhikrCounter.ui.components.catalog.utils

import androidx.activity.compose.BackHandler as ComposeBackHandler
import androidx.compose.runtime.Composable

@Composable
fun BackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    ComposeBackHandler(enabled, onBack)
}
