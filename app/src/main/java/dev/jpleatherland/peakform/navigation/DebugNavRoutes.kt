package dev.jpleatherland.peakform.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import dev.jpleatherland.peakform.viewmodel.WeightViewModel

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
