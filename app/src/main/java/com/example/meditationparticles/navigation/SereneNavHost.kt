package com.example.meditationparticles.navigation

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationCatalog
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.navigation.SereneDestination.ToolkitTab
import com.example.meditationparticles.ui.breathing.BreathingScreen
import com.example.meditationparticles.ui.components.BottomNavItem
import com.example.meditationparticles.ui.components.BuildInfoFooter
import com.example.meditationparticles.ui.components.SereneAppBanner
import com.example.meditationparticles.ui.components.KeepScreenOnEffect
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

private val tabBackgroundRoutes = setOf(
    SereneDestination.Home.route,
    SereneDestination.Breathe.route,
    SereneDestination.Timer.route,
    SereneDestination.Affirmations.route,
    SereneDestination.Toolkit.route,
    SereneDestination.Visualizations.route,
)

private fun isTabBackgroundRoute(route: String?): Boolean = route in tabBackgroundRoutes

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
    val context = LocalContext.current
    val tabBackgroundRotation = remember { AppGraph.tabBackgroundRotation(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeCount by remember { mutableIntStateOf(0) }
    val settings = LocalExperienceSettings.current
    val isSystemDark = isSystemInDarkTheme()
    var breathingSessionActive by remember { mutableStateOf(false) }
    var timerSessionActive by remember { mutableStateOf(false) }
    var visualizationPlayerActive by remember { mutableStateOf(false) }
    var activeFutureSelfMessageId by remember { mutableStateOf<Long?>(null) }
    var toolkitResetSignal by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeCount++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentRoute, settings.themeMode, isSystemDark) {
        if (isTabBackgroundRoute(currentRoute)) {
            tabBackgroundRotation.advance(settings.themeMode, isSystemDark)
        }
    }

    LaunchedEffect(resumeCount, settings.themeMode, isSystemDark) {
        if (resumeCount > 1 && isTabBackgroundRoute(currentRoute)) {
            tabBackgroundRotation.advance(settings.themeMode, isSystemDark)
        }
    }

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
        if (currentRoute != SereneDestination.Timer.route) {
            timerSessionActive = false
        }
        if (currentRoute?.startsWith("visualizations/player") != true) {
            visualizationPlayerActive = false
        }
    }

    KeepScreenOnEffect(
        active = breathingSessionActive || timerSessionActive || visualizationPlayerActive,
    )

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

    val showAppBanner = currentRoute != SereneDestination.Settings.route &&
        currentRoute != SereneDestination.Onboarding.route &&
        currentRoute?.startsWith("visualizations/player") != true

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
        topBar = {
            if (showAppBanner) {
                SereneAppBanner()
            }
        },
        bottomBar = {
            if (showBuildFooter) {
                Column {
                    if (showBottomBar && bottomNavItems.isNotEmpty()) {
                        SereneBottomBar(
                            items = bottomNavItems,
                            currentRoute = currentRoute,
                            onNavigate = { destination ->
                                if (destination == SereneDestination.Toolkit) {
                                    toolkitResetSignal++
                                }
                                if (destination == SereneDestination.Visualizations &&
                                    currentRoute?.startsWith("visualizations/player") == true
                                ) {
                                    navController.popBackStack(
                                        SereneDestination.Visualizations.route,
                                        inclusive = false,
                                    )
                                }
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = false
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
                TimerScreen(
                    onSessionActiveChange = { active ->
                        timerSessionActive = active
                    },
                )
            }
            composable(SereneDestination.Affirmations.route) {
                AffirmationsScreen()
            }
            composable(SereneDestination.Toolkit.route) {
                ToolkitScreen(
                    pendingNavigation = pendingNavigation,
                    resetSignal = toolkitResetSignal,
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
                        onPlayerActiveChange = { active ->
                            visualizationPlayerActive = active
                        },
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
