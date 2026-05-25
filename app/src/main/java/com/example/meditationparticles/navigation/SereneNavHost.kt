package com.example.meditationparticles.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.FormatQuote
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
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationCatalog
import com.example.meditationparticles.navigation.SereneDestination.ToolkitTab
import com.example.meditationparticles.ui.breathing.BreathingScreen
import com.example.meditationparticles.ui.components.BottomNavItem
import com.example.meditationparticles.ui.components.BuildInfoFooter
import com.example.meditationparticles.ui.components.SereneBottomBar
import com.example.meditationparticles.ui.home.FutureSelfNotificationOverlay
import com.example.meditationparticles.ui.home.HomeScreen
import com.example.meditationparticles.ui.onboarding.OnboardingScreen
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.settings.SettingsScreen
import com.example.meditationparticles.ui.timer.TimerScreen
import com.example.meditationparticles.ui.toolkit.AffirmationsScreen
import com.example.meditationparticles.ui.toolkit.ToolkitScreen
import com.example.meditationparticles.ui.update.UpdateViewModel
import com.example.meditationparticles.ui.visualizations.VisualizationPlayerScreen
import com.example.meditationparticles.ui.visualizations.VisualizationsScreen

private val allBottomNavItems = listOf(
    BottomNavItem(SereneDestination.Home, "Home", Icons.Outlined.Home, Icons.Default.Home),
    BottomNavItem(SereneDestination.Breathe, "Breathe", Icons.Outlined.Air, Icons.Default.Air),
    BottomNavItem(SereneDestination.Timer, "Meditation", Icons.Outlined.Timer, Icons.Default.Timer),
    BottomNavItem(
        SereneDestination.Affirmations,
        "Affirmations",
        Icons.Outlined.FormatQuote,
        Icons.Default.FormatQuote,
    ),
    BottomNavItem(SereneDestination.Toolkit, "Toolkit", Icons.Outlined.Handyman, Icons.Default.Handyman),
    BottomNavItem(SereneDestination.Visualizations, "Visuals", Icons.Outlined.Landscape, Icons.Default.Landscape),
)

@Composable
fun SereneNavHost(
    updateViewModel: UpdateViewModel,
    pendingNavigation: PendingToolkitNavigation? = null,
    pendingFutureSelfMessageId: Long? = null,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val settings = LocalExperienceSettings.current
    var breathingSessionActive by remember { mutableStateOf(false) }
    var activeFutureSelfMessageId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(pendingFutureSelfMessageId, settings.onboardingCompleted) {
        val messageId = pendingFutureSelfMessageId ?: return@LaunchedEffect
        if (!settings.onboardingCompleted) return@LaunchedEffect
        navController.navigate(SereneDestination.Home.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        activeFutureSelfMessageId = messageId
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute != SereneDestination.Breathe.route) {
            breathingSessionActive = false
        }
    }

    val bottomNavItems = remember(
        settings.enableBreathing,
        settings.enableTimer,
        settings.enableAffirmations,
        settings.enableToolkit,
        settings.enableVisuals,
    ) {
        allBottomNavItems.filter { item ->
            when (item.destination) {
                SereneDestination.Home -> true
                SereneDestination.Breathe -> settings.enableBreathing
                SereneDestination.Timer -> settings.enableTimer
                SereneDestination.Affirmations -> settings.enableAffirmations
                SereneDestination.Toolkit -> settings.enableToolkit
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

    val startDestination = if (settings.onboardingCompleted) {
        SereneDestination.Home.route
    } else {
        SereneDestination.Onboarding.route
    }

    LaunchedEffect(pendingNavigation, settings.onboardingCompleted) {
        val navigation = pendingNavigation ?: return@LaunchedEffect
        if (!settings.onboardingCompleted) return@LaunchedEffect
        if (navigation.toolkitTab == null && navigation.toolId == null) return@LaunchedEffect
        val route = when (navigation.toolkitTab) {
            ToolkitTab.AFFIRMATIONS -> {
                if (settings.enableAffirmations) {
                    SereneDestination.Affirmations.route
                } else {
                    null
                }
            }
            else -> {
                if (settings.enableToolkit) {
                    SereneDestination.Toolkit.route
                } else {
                    null
                }
            }
        } ?: return@LaunchedEffect
        navController.navigate(route) {
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
                                navController.navigate(destination.route) {
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
                        val route = if (toolkitTab != null) {
                            destination.navigationRouteFromLegacyTab(toolkitTab)
                        } else {
                            destination.navigationRoute()
                        }
                        navController.navigate(route) {
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
            composable(SereneDestination.Affirmations.route) {
                AffirmationsScreen()
            }
            composable(SereneDestination.Toolkit.route) {
                ToolkitScreen(
                    pendingNavigation = pendingNavigation,
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
            composable(
                route = "toolkit/{tab}",
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = ToolkitTab.TOOLKIT
                    },
                ),
            ) { entry ->
                val tab = entry.arguments?.getString("tab")
                val route = when (tab) {
                    ToolkitTab.AFFIRMATIONS -> SereneDestination.Affirmations.route
                    else -> SereneDestination.Toolkit.route
                }
                LaunchedEffect(route) {
                    navController.navigate(route) {
                        popUpTo("toolkit/{tab}") { inclusive = true }
                        launchSingleTop = true
                    }
                }
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
            activeFutureSelfMessageId?.let { messageId ->
                FutureSelfNotificationOverlay(
                    messageId = messageId,
                    onDismiss = { activeFutureSelfMessageId = null },
                    onDeleted = { activeFutureSelfMessageId = null },
                )
            }
        }
    }
}
