package com.example.meditationparticles.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationCatalog
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.navigation.SereneDestination.ToolkitTab
import com.example.meditationparticles.ui.breathing.BreathingScreen
import com.example.meditationparticles.ui.components.BottomNavItem
import com.example.meditationparticles.ui.components.BuildInfoFooter
import com.example.meditationparticles.ui.components.SereneBottomBar
import com.example.meditationparticles.ui.home.HomeScreen
import com.example.meditationparticles.ui.onboarding.OnboardingScreen
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.settings.SettingsScreen
import com.example.meditationparticles.ui.timer.TimerScreen
import com.example.meditationparticles.ui.toolkit.ToolkitScreen
import com.example.meditationparticles.ui.update.UpdateViewModel
import com.example.meditationparticles.ui.visualizations.VisualizationPlayerScreen
import com.example.meditationparticles.ui.visualizations.VisualizationsScreen

private val allBottomNavItems = listOf(
    BottomNavItem(SereneDestination.Home, "Home", Icons.Outlined.Home, Icons.Default.Home),
    BottomNavItem(SereneDestination.Breathe, "Breathe", Icons.Outlined.Air, Icons.Default.Air),
    BottomNavItem(SereneDestination.Timer, "Timer", Icons.Outlined.Timer, Icons.Default.Timer),
    BottomNavItem(SereneDestination.Toolkit, "Toolkit", Icons.Outlined.Handyman, Icons.Default.Handyman),
    BottomNavItem(SereneDestination.Visualizations, "Visuals", Icons.Outlined.Landscape, Icons.Default.Landscape),
)

@Composable
fun SereneNavHost(
    updateViewModel: UpdateViewModel,
    pendingNavigation: PendingToolkitNavigation? = null,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val settings = LocalExperienceSettings.current
    var breathingSessionActive by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != SereneDestination.Breathe.route) {
            breathingSessionActive = false
        }
    }

    val bottomNavItems = remember(
        settings.enableBreathing,
        settings.enableTimer,
        settings.showToolkitTab,
        settings.enableVisuals,
    ) {
        allBottomNavItems.filter { item ->
            when (item.destination) {
                SereneDestination.Home -> true
                SereneDestination.Breathe -> settings.enableBreathing
                SereneDestination.Timer -> settings.enableTimer
                SereneDestination.Toolkit -> settings.showToolkitTab
                SereneDestination.Visualizations -> settings.enableVisuals
                else -> false
            }
        }
    }

    val showBottomBar = currentRoute != SereneDestination.Settings.route &&
        currentRoute != SereneDestination.Onboarding.route &&
        currentRoute?.startsWith("visualizations/player") != true &&
        !breathingSessionActive

    val showBuildFooter = currentRoute?.startsWith("visualizations/player") != true &&
        !breathingSessionActive

    val defaultToolkitTab = when {
        settings.enableAffirmations -> ToolkitTab.AFFIRMATIONS
        settings.enableToolkit -> ToolkitTab.TOOLKIT
        else -> ToolkitTab.AFFIRMATIONS
    }

    val startDestination = if (settings.onboardingCompleted) {
        SereneDestination.Home.route
    } else {
        SereneDestination.Onboarding.route
    }

    LaunchedEffect(pendingNavigation, settings.onboardingCompleted) {
        val navigation = pendingNavigation ?: return@LaunchedEffect
        if (!settings.onboardingCompleted) return@LaunchedEffect
        val tab = navigation.toolkitTab ?: SereneDestination.ToolkitTab.TOOLKIT
        navController.navigate(SereneDestination.Toolkit.createRoute(tab)) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBuildFooter) {
                Column {
                    if (showBottomBar && bottomNavItems.isNotEmpty()) {
                        SereneBottomBar(
                            items = bottomNavItems,
                            currentRoute = currentRoute,
                            onNavigate = { destination ->
                                val route = when (destination) {
                                    SereneDestination.Toolkit -> destination.navigationRoute(defaultToolkitTab)
                                    else -> destination.route
                                }
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                    BuildInfoFooter()
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
            ) {
            composable(SereneDestination.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(SereneDestination.Home.route) {
                            popUpTo(SereneDestination.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(SereneDestination.Home.route) {
                HomeScreen(
                    onNavigate = { destination, toolkitTab ->
                        navController.navigate(destination.navigationRoute(toolkitTab)) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = {
                        navController.navigate(SereneDestination.Settings.route)
                    },
                )
            }
            composable(SereneDestination.Settings.route) {
                SettingsScreen(
                    updateViewModel = updateViewModel,
                    onBack = { navController.popBackStack() },
                    onResetOnboarding = {
                        navController.navigate(SereneDestination.Onboarding.route) {
                            popUpTo(SereneDestination.Home.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(SereneDestination.Breathe.route) {
                BreathingScreen(
                    onSessionActiveChange = { active ->
                        breathingSessionActive = active
                    },
                )
            }
            composable(SereneDestination.Timer.route) {
                TimerScreen()
            }
            composable(
                route = SereneDestination.Toolkit.route,
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = ToolkitTab.AFFIRMATIONS
                    },
                ),
            ) { entry ->
                val requestedTab = entry.arguments?.getString("tab") ?: ToolkitTab.AFFIRMATIONS
                val resolvedTab = when {
                    requestedTab == ToolkitTab.TOOLKIT && settings.enableToolkit -> ToolkitTab.TOOLKIT
                    settings.enableAffirmations -> ToolkitTab.AFFIRMATIONS
                    settings.enableToolkit -> ToolkitTab.TOOLKIT
                    else -> ToolkitTab.AFFIRMATIONS
                }
                ToolkitScreen(
                    initialTab = resolvedTab,
                    pendingNavigation = if (resolvedTab == ToolkitTab.TOOLKIT) pendingNavigation else null,
                    onNavigateToBreathe = {
                        navController.navigate(SereneDestination.Breathe.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(SereneDestination.Visualizations.route) {
                VisualizationsScreen(
                    onOpenVisualization = { id ->
                        navController.navigate(SereneDestination.Visualizations.playerRoute(id.name))
                    },
                )
            }
            composable(
                route = "visualizations/player/{vizId}",
                arguments = listOf(navArgument("vizId") { type = NavType.StringType }),
            ) { entry ->
                val vizId = entry.arguments?.getString("vizId")
                val visualization = vizId?.let { CalmingVisualizationCatalog.byRouteName(it) }
                if (visualization != null) {
                    VisualizationPlayerScreen(
                        visualization = visualization,
                        onClose = { navController.popBackStack() },
                    )
                }
            }
        }
        }
    }
}
