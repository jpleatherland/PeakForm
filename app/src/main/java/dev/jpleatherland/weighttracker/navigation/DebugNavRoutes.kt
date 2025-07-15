package dev.jpleatherland.weighttracker.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import dev.jpleatherland.weighttracker.navigation.appNavHost_addDebugRoutes_internal
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

fun appNavHostAddDebugRoutes(
    navController: NavHostController,
    viewModel: WeightViewModel,
    builder: NavGraphBuilder,
) {
    appNavHost_addDebugRoutes_internal(
        navController,
        viewModel,
        builder,
    )
}
