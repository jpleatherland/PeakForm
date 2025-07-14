package com.example.weighttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.DateRange

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.*
import com.example.weighttracker.data.WeightRepository
import com.example.weighttracker.data.WeightViewModelFactory
import com.example.weighttracker.data.provideDatabase
import com.example.weighttracker.navigation.AppNavHost
import com.example.weighttracker.ui.theme.WeightTrackerTheme
import com.example.weighttracker.viewmodel.WeightViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = provideDatabase(this)
        val repo = WeightRepository(db.weightDao())
        val factory = WeightViewModelFactory(repo)
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
                            onClick = { navController.navigate("entry") }
                        )
                        NavigationBarItem(icon = {
                            Icon(
                                Icons.Default.DateRange, contentDescription = null
                            )
                        },
                            label = { Text("Trends") },
                            selected = navController.currentDestination?.route == "charts",
                            onClick = {
                                navController.navigate("charts") {
                                    launchSingleTop = true
                                }
                            })
                        NavigationBarItem(icon = {
                            Icon(
                                Icons.Default.Star, contentDescription = null
                            )
                        },
                            label = { Text("Goals") },
                            selected = navController.currentDestination?.route == "goals",
                            onClick = {
                                navController.navigate("goals") {
                                    launchSingleTop = true
                                }
                            })
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