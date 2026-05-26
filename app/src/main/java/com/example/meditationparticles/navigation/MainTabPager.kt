package com.example.meditationparticles.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.meditationparticles.ui.components.BottomNavItem

internal fun isTabRoute(route: String?, items: List<BottomNavItem>): Boolean {
    if (route == null || items.isEmpty()) return false
    return items.any { item ->
        when (item.destination) {
            SereneDestination.Visualizations -> route == SereneDestination.Visualizations.route
            else -> route == item.destination.route
        }
    }
}

internal fun tabIndexForRoute(route: String?, items: List<BottomNavItem>): Int? {
    if (route == null || items.isEmpty()) return null
    return items.indexOfFirst { item ->
        when (item.destination) {
            SereneDestination.Visualizations -> route == SereneDestination.Visualizations.route
            else -> route == item.destination.route
        }
    }.takeIf { it >= 0 }
}

@Composable
fun MainTabPager(
    items: List<BottomNavItem>,
    pagerState: PagerState,
    userScrollEnabled: Boolean,
    modifier: Modifier = Modifier,
    pageContent: @Composable (destination: SereneDestination) -> Unit,
) {
    if (items.isEmpty()) return

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = userScrollEnabled,
        beyondViewportPageCount = (items.size - 1).coerceAtLeast(0),
    ) { page ->
        pageContent(items[page].destination)
    }
}
