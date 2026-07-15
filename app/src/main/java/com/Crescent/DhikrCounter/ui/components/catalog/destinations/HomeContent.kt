package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.ui.components.catalog.CatalogDestination

@Composable
fun HomeContent(onNavigate: (CatalogDestination) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val adaptiveColor = if (isDark) Color.White else Color.Black

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "UI Catalog",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = adaptiveColor
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Liquid Glass Components",
                style = MaterialTheme.typography.bodyLarge,
                color = adaptiveColor.copy(alpha = 0.6f)
            )
        }

        item { SectionHeader("Core Components") }
        items(
            listOf(
                "Buttons" to CatalogDestination.Buttons,
                "Toggle" to CatalogDestination.Toggle,
                "Slider" to CatalogDestination.Slider,
                "Bottom Tabs" to CatalogDestination.BottomTabs,
                "Dialog" to CatalogDestination.Dialog
            )
        ) { (title, dest) ->
            CatalogItem(title, adaptiveColor) { onNavigate(dest) }
        }

        item { SectionHeader("Demos") }
        items(
            listOf(
                "Control Center" to CatalogDestination.ControlCenter,
                "Lock Screen" to CatalogDestination.LockScreen,
                "Magnifier" to CatalogDestination.Magnifier
            )
        ) { (title, dest) ->
            CatalogItem(title, adaptiveColor) { onNavigate(dest) }
        }

        item { SectionHeader("Experimental") }
        items(
            listOf(
                "Glass Playground" to CatalogDestination.GlassPlayground,
                "Adaptive Luminance" to CatalogDestination.AdaptiveLuminanceGlass,
                "Progressive Blur" to CatalogDestination.ProgressiveBlur,
                "Scroll Container" to CatalogDestination.ScrollContainer,
                "Lazy Scroll Container" to CatalogDestination.LazyScrollContainer
            )
        ) { (title, dest) ->
            CatalogItem(title, adaptiveColor) { onNavigate(dest) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0088FF),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun CatalogItem(title: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = color.copy(alpha = 0.05f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, color = color)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = color.copy(alpha = 0.3f))
        }
    }
}
