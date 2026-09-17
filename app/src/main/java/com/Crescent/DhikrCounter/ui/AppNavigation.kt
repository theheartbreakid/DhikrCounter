package com.Crescent.DhikrCounter.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidBottomTab
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidBottomTabs
import com.Crescent.DhikrCounter.ui.components.catalog.utils.LocalBackdrop
import com.Crescent.DhikrCounter.ui.components.PrismalScene

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: CounterViewModel = viewModel()
    val backdrop = LocalBackdrop.current
    
    val currentRoute = remember { mutableStateOf("counter") }

    val selectedIndex = when (currentRoute.value) {
        "counter" -> 0
        "sessions" -> 1
        "history" -> 2
        "statistics" -> 3
        "settings" -> 4
        else -> 0
    }

    val destinations = listOf(
        Triple("Counter", Icons.Outlined.AddCircleOutline, "counter"),
        Triple("Sessions", Icons.Outlined.Layers, "sessions"),
        Triple("History", Icons.Outlined.History, "history"),
        Triple("Stats", Icons.Outlined.BarChart, "statistics"),
        Triple("Settings", Icons.Outlined.Settings, "settings")
    )

    PrismalScene(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                NavHost(
                    navController = navController, 
                    startDestination = "counter",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("counter") {
                        currentRoute.value = "counter"
                        CounterScreen(viewModel = viewModel)
                    }
                    composable("sessions") {
                        currentRoute.value = "sessions"
                        SessionsScreen(viewModel = viewModel)
                    }
                    composable("history") {
                        currentRoute.value = "history"
                        HistoryScreen(viewModel = viewModel)
                    }
                    composable("statistics") {
                        currentRoute.value = "statistics"
                        StatisticsScreen(viewModel = viewModel)
                    }
                    composable("settings") {
                        currentRoute.value = "settings"
                        SettingsScreen(viewModel = viewModel)
                    }
                }

                if (backdrop != null) {
                    val sizeDetails = com.Crescent.DhikrCounter.ui.components.LocalAppWindowSizeDetails.current
                    val isCompact = sizeDetails.widthClass == com.Crescent.DhikrCounter.ui.components.AppWindowWidthSizeClass.COMPACT || sizeDetails.heightClass == com.Crescent.DhikrCounter.ui.components.AppWindowHeightSizeClass.COMPACT
                    val isExpanded = sizeDetails.widthClass == com.Crescent.DhikrCounter.ui.components.AppWindowWidthSizeClass.EXPANDED
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = if (isExpanded) 120.dp else if (sizeDetails.isLandscape) 64.dp else 16.dp,
                                vertical = if (isCompact) 8.dp else 24.dp
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        LiquidBottomTabs(
                            selectedTabIndex = { selectedIndex },
                            onTabSelected = { index ->
                                val route = destinations[index].third
                                if (currentRoute.value != route) {
                                    currentRoute.value = route
                                    navController.navigate(route) {
                                        popUpTo("counter") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            backdrop = backdrop,
                            tabsCount = destinations.size,
                            accentColor = MaterialTheme.colorScheme.primary
                        ) {
                            destinations.forEach { (label, icon, route) ->
                                val isSelected = currentRoute.value == route
                                val adaptiveColor = com.Crescent.DhikrCounter.ui.components.LocalPrismalAdaptiveColor.current
                                LiquidBottomTab(onClick = {
                                    if (currentRoute.value != route) {
                                        currentRoute.value = route
                                        navController.navigate(route) {
                                            popUpTo("counter") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }) {
                                    Icon(
                                        icon, 
                                        contentDescription = label,
                                        modifier = Modifier.size(if (isCompact) 20.dp else 24.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else adaptiveColor
                                    )
                                    if (!isCompact) {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else adaptiveColor.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
