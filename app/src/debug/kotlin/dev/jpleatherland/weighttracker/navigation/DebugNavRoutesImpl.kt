package dev.jpleatherland.weighttracker.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.jpleatherland.weighttracker.ui.DebugScreen
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

fun appNavHost_addDebugRoutes_internal(
    navController: NavHostController,
    viewModel: WeightViewModel,
    builder: NavGraphBuilder,
) {
    builder.composable("debug") {
        DebugScreen(viewModel.dao!!, viewModel.goalDao!!)
    }
}
