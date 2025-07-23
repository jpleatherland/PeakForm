package dev.jpleatherland.peakform.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.jpleatherland.peakform.ui.DebugScreen
import dev.jpleatherland.peakform.viewmodel.WeightViewModel

fun appNavHost_addDebugRoutes_internal(
    navController: NavHostController,
    viewModel: WeightViewModel,
    builder: NavGraphBuilder,
) {
    builder.composable("debug") {
        DebugScreen(viewModel, viewModel.dao!!, viewModel.goalDao!!)
    }
}
