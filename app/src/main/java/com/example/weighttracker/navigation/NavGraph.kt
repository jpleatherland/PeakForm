package com.example.weighttracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.weighttracker.ui.ChartScreen
import com.example.weighttracker.ui.DailyEntryScreen
import com.example.weighttracker.ui.GoalScreen
import com.example.weighttracker.viewmodel.WeightViewModel

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
    }
}