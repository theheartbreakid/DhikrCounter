package com.Crescent.DhikrCounter.ui.components.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun BackdropDemoScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: LayerBackdrop) -> Unit
) {
    val backdrop = rememberLayerBackdrop()
    Box(
        modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content(backdrop)
    }
}
