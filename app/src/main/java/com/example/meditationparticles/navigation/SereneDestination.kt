package com.example.meditationparticles.navigation

import com.example.meditationparticles.domain.mood.MoodGraphPeriod

sealed class SereneDestination(val route: String) {
    data object Home : SereneDestination("home")
    data object Breathe : SereneDestination("breathe")
    data object Timer : SereneDestination("timer")
    data object Affirmations : SereneDestination("affirmations")
    data object Toolkit : SereneDestination("toolkit")
    data object Visualizations : SereneDestination("visualizations") {
        fun playerRoute(vizId: String) = "visualizations/player/$vizId"
    }
    data object LivingTree : SereneDestination("living_tree")
    data object LivingTreeSetup : SereneDestination("living_tree/setup")
    data object Settings : SereneDestination("settings")
    data object Onboarding : SereneDestination("onboarding")

    data object MoodGraph : SereneDestination("mood_graph/{period}") {
        fun route(period: MoodGraphPeriod): String = "mood_graph/${period.name}"

        fun parsePeriod(periodArg: String?): MoodGraphPeriod =
            runCatching { MoodGraphPeriod.valueOf(periodArg ?: "") }
                .getOrDefault(MoodGraphPeriod.DAY)
    }

    object ToolkitTab {
        const val AFFIRMATIONS = "affirmations"
        const val TOOLKIT = "toolkit"
    }

    companion object {
        val bottomNavDestinations = listOf(Home, Breathe, Timer, Affirmations, Toolkit, LivingTree, Visualizations)
    }
}
