package dev.jpleatherland.peakform

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.*
import dev.jpleatherland.peakform.data.GoalRepository
import dev.jpleatherland.peakform.data.GoalSegmentRepository
import dev.jpleatherland.peakform.data.WeightRepository
import dev.jpleatherland.peakform.data.WeightViewModelFactory
import dev.jpleatherland.peakform.data.provideDatabase
import dev.jpleatherland.peakform.navigation.AppNavHost
import dev.jpleatherland.peakform.navigation.Screen
import dev.jpleatherland.peakform.ui.theme.WeightTrackerTheme
import dev.jpleatherland.peakform.viewmodel.WeightViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = provideDatabase(this)
        val repo = WeightRepository(db.weightDao())
        val goalRepo = GoalRepository(db.goalDao())
        val goalSegmentRepo = GoalSegmentRepository(db.goalSegmentDao())
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        val factory = WeightViewModelFactory(repo, goalRepo, goalSegmentRepo, healthConnectClient)
        val viewModel = ViewModelProvider(this, factory)[WeightViewModel::class.java]

        setContent {
            WeightTrackerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute =
                    navBackStackEntry?.destination?.route
                val currentScreen = Screen.fromRoute(currentRoute)
                Scaffold(
                    topBar = { MyTopBar(currentScreen, onSettingsClick = { navController.navigate(Screen.Settings.route) }) },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                label = { Text(Screen.Entry.title) },
                                selected = navController.currentDestination?.route == Screen.Entry.route,
                                onClick = { navController.navigate(Screen.Entry.route) },
                            )
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(Screen.Charts.title) },
                                selected = navController.currentDestination?.route == Screen.Charts.route,
                                onClick = {
                                    navController.navigate(Screen.Charts.route) {
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
                                label = { Text(Screen.Goals.title) },
                                selected = navController.currentDestination?.route == Screen.Goals.route,
                                onClick = {
                                    navController.navigate(Screen.Goals.route) {
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
                                label = { Text(Screen.History.title) },
                                selected = navController.currentDestination?.route == Screen.History.route,
                                onClick = {
                                    navController.navigate(Screen.History.route) {
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
                                label = { Text(Screen.Settings.title) },
                                selected = navController.currentDestination?.route == Screen.Settings.route,
                                onClick = {
                                    navController.navigate(Screen.Settings.route) {
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
                    },
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        AppNavHost(navController = navController, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopBar(
    currentScreen: Screen,
    onSettingsClick: () -> Unit,
) {
    when (currentScreen) {
        Screen.Home -> {
            TopAppBar(
                title = { Text("PeakForm") },
                navigationIcon = { R.mipmap.ic_launcher },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
            )
        }
        else -> {
            TopAppBar(
                title = { Text(currentScreen.title) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsCollapsingAppBar(onSettingsClick: () -> Unit) {
    LargeTopAppBar(
        title = { Text(Screen.Charts.title) },
        actions = { IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = "Settings") } },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}
