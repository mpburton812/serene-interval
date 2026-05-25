package com.example.meditationparticles.navigation

sealed class SereneDestination(val route: String) {
    data object Home : SereneDestination("home")
    data object Breathe : SereneDestination("breathe")
    data object Timer : SereneDestination("timer")
    data object Affirmations : SereneDestination("affirmations")
    data object Toolkit : SereneDestination("toolkit")
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
        val bottomNavDestinations = listOf(Home, Breathe, Timer, Affirmations, Toolkit, Visualizations)
    }
}
