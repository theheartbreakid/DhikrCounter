package com.Crescent.DhikrCounter.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: CounterViewModel = viewModel()
    val backdrop = rememberLayerBackdrop()

    NavHost(navController = navController, startDestination = "counter") {
        composable("counter") {
            CounterScreen(
                viewModel = viewModel,
                backdrop = backdrop,
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToDashboard = { navController.navigate("statistics") }
            )
        }
        composable("settings") {
            SettingsScreen(
                backdrop = backdrop,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("statistics") {
            DashboardScreen(
                viewModel = viewModel,
                backdrop = backdrop,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
