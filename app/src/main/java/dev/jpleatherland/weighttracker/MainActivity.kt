package dev.jpleatherland.weighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.*
import dev.jpleatherland.weighttracker.data.GoalRepository
import dev.jpleatherland.weighttracker.data.WeightRepository
import dev.jpleatherland.weighttracker.data.WeightViewModelFactory
import dev.jpleatherland.weighttracker.data.provideDatabase
import dev.jpleatherland.weighttracker.navigation.AppNavHost
import dev.jpleatherland.weighttracker.ui.theme.WeightTrackerTheme
import dev.jpleatherland.weighttracker.viewmodel.WeightViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = provideDatabase(this)
        val repo = WeightRepository(db.weightDao())
        val goalRepo = GoalRepository(db.goalDao())
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        val factory = WeightViewModelFactory(repo, goalRepo, healthConnectClient)
        val viewModel = ViewModelProvider(this, factory)[WeightViewModel::class.java]

        setContent {
            WeightTrackerTheme {
                val navController = rememberNavController()
                Scaffold(bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            label = { Text("Log") },
                            selected = navController.currentDestination?.route == "entry",
                            onClick = { navController.navigate("entry") },
                        )
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = null,
                                )
                            },
                            label = { Text("Trends") },
                            selected = navController.currentDestination?.route == "charts",
                            onClick = {
                                navController.navigate("charts") {
                                    launchSingleTop = true
                                }
                            },
                        )
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                )
                            },
                            label = { Text("Goals") },
                            selected = navController.currentDestination?.route == "goals",
                            onClick = {
                                navController.navigate("goals") {
                                    launchSingleTop = true
                                }
                            },
                        )
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                )
                            },
                            label = { Text("History") },
                            selected = navController.currentDestination?.route == "history",
                            onClick = {
                                navController.navigate("history") {
                                    launchSingleTop = true
                                }
                            },
                        )
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                )
                            },
                            label = { Text("Settings") },
                            selected = navController.currentDestination?.route == "settings",
                            onClick = {
                                navController.navigate("settings") {
                                    launchSingleTop = true
                                }
                            },
                        )
                        if (BuildConfig.DEBUG) {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                                label = { Text("Debug") },
                                selected = navController.currentDestination?.route == "debug",
                                onClick = {
                                    navController.navigate("debug") {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    }
                }) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        AppNavHost(navController = navController, viewModel = viewModel)
                    }
                }
            }
        }
    }
}
