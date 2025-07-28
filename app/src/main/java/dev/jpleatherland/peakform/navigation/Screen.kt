package dev.jpleatherland.peakform.navigation

sealed class Screen(
    val route: String,
    val title: String,
    val showLogo: Boolean = false,
) {
    data object Entry : Screen("entry", "Log", showLogo = false)

    data object Charts : Screen("charts", "Trends", showLogo = false)

    data object Goals : Screen("goals", "Goals", showLogo = false)

    data object History : Screen("history", "History", showLogo = false)

    data object Settings : Screen("settings", "Settings", showLogo = false)

    data object Home : Screen("home", "PeakForm", showLogo = true)

    companion object {
        fun fromRoute(route: String?): Screen =
            when (route) {
                Entry.route -> Entry
                Charts.route -> Charts
                Goals.route -> Goals
                History.route -> History
                Settings.route -> Settings
                Home.route -> Home
                else -> Entry
            }
    }
}
