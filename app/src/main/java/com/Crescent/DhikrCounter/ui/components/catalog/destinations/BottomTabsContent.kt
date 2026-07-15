package com.Crescent.DhikrCounter.ui.components.catalog.destinations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.Crescent.DhikrCounter.ui.components.AdaptiveIcon
import com.Crescent.DhikrCounter.ui.components.catalog.BackdropDemoScaffold
import com.Crescent.DhikrCounter.ui.components.catalog.Block
import com.Crescent.DhikrCounter.ui.components.catalog.FlightIcon
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidBottomTab
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidBottomTabs

@Composable
fun BottomTabsContent() {
    BackdropDemoScaffold { backdrop ->
        var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

        Box(
            Modifier
                .fillMaxSize()
                .padding(24f.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            LiquidBottomTabs(
                selectedTabIndex = { selectedTabIndex },
                onTabSelected = { selectedTabIndex = it },
                backdrop = backdrop,
                tabsCount = 4
            ) {
                LiquidBottomTab({ selectedTabIndex = 0 }) {
                    AdaptiveIcon(Icons.Outlined.Home, darkVariant = Icons.Filled.Home)
                }
                LiquidBottomTab({ selectedTabIndex = 1 }) {
                    AdaptiveIcon(Icons.Outlined.Search, darkVariant = Icons.Filled.Search)
                }
                LiquidBottomTab({ selectedTabIndex = 2 }) {
                    AdaptiveIcon(FlightIcon)
                }
                LiquidBottomTab({ selectedTabIndex = 3 }) {
                    AdaptiveIcon(Icons.Outlined.Settings, darkVariant = Icons.Filled.Settings)
                }
            }
        }
    }
}
