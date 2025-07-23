package dev.jpleatherland.peakform.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.jpleatherland.peakform.ui.ChartScreen
import dev.jpleatherland.peakform.ui.DailyEntryScreen
import dev.jpleatherland.peakform.ui.GoalScreen
import dev.jpleatherland.peakform.ui.HistoryScreen
import dev.jpleatherland.peakform.ui.SettingsScreen
import dev.jpleatherland.peakform.viewmodel.WeightViewModel

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: WeightViewModel,
    startDestination: String = "entry",
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("entry") { DailyEntryScreen(viewModel) }
        composable("charts") { ChartScreen(viewModel) }
        composable("goals") { GoalScreen(viewModel) }
        composable("history") { HistoryScreen(viewModel) }
        composable("settings") { SettingsScreen(viewModel) }
        appNavHostAddDebugRoutes(
            navController,
            viewModel,
            this,
        )
    }
}
