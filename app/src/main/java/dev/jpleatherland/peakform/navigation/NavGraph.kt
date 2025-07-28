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
    startDestination: String = Screen.Entry.route,
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Entry.route) { DailyEntryScreen(viewModel) }
        composable(Screen.Charts.route) { ChartScreen(viewModel) }
        composable(Screen.Goals.route) { GoalScreen(viewModel) }
        composable(Screen.History.route) { HistoryScreen(viewModel) }
        composable(Screen.Settings.route) { SettingsScreen(viewModel) }
        appNavHostAddDebugRoutes(
            navController,
            viewModel,
            this,
        )
    }
}
