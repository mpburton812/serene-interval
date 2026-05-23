package com.example.meditationparticles.navigation

sealed class SereneDestination(val route: String) {
    data object Home : SereneDestination("home")
    data object Breathe : SereneDestination("breathe")
    data object Timer : SereneDestination("timer")
    data object Toolkit : SereneDestination("toolkit/{tab}") {
        fun createRoute(tab: String = ToolkitTab.AFFIRMATIONS): String = "toolkit/$tab"
    }
    data object Visualizations : SereneDestination("visualizations") {
        fun playerRoute(vizId: String) = "visualizations/player/$vizId"
    }
    data object Settings : SereneDestination("settings")
    data object Onboarding : SereneDestination("onboarding")

    object ToolkitTab {
        const val AFFIRMATIONS = "affirmations"
        const val TOOLKIT = "toolkit"
    }

    companion object {
        val bottomNavDestinations = listOf(Home, Breathe, Timer, Toolkit, Visualizations)
    }
}
