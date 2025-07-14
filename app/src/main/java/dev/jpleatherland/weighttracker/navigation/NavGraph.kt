package dev.jpleatherland.weighttracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import dev.jpleatherland.weighttracker.ui.ChartScreen
import dev.jpleatherland.weighttracker.ui.DailyEntryScreen
import dev.jpleatherland.weighttracker.ui.GoalScreen
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel


@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: WeightViewModel,
    startDestination: String = "entry"
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("entry") { DailyEntryScreen(viewModel) }
        composable("charts") { ChartScreen(viewModel) }
        composable("goals") { GoalScreen() }
//        if (BuildConfig.DEBUG) {
//                viewModel.dao?.let { dao -> DebugScreen(dao = dao) }
//            includeDebugRoutes(viewModel)
//        }
        appNavHost_addDebugRoutes(
            navController,
            viewModel,
            this
        )
    }
}