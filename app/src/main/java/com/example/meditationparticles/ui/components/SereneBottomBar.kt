package com.example.meditationparticles.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meditationparticles.navigation.SereneDestination
import com.example.meditationparticles.ui.theme.isDarkScheme

data class BottomNavItem(
    val destination: SereneDestination,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

@Composable
fun SereneBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onNavigate: (SereneDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = isDarkScheme(scheme)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = if (isDark) {
            scheme.surface.copy(alpha = 0.92f)
        } else {
            scheme.surfaceContainerLow.copy(alpha = 0.96f)
        },
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = when (item.destination) {
                    SereneDestination.Toolkit -> currentRoute == SereneDestination.Toolkit.route
                    SereneDestination.Affirmations -> currentRoute == SereneDestination.Affirmations.route
                    SereneDestination.KatiesLoveList -> currentRoute == SereneDestination.KatiesLoveList.route
                    SereneDestination.Visualizations -> currentRoute?.startsWith("visualizations") == true
                    SereneDestination.LivingTree -> currentRoute == SereneDestination.LivingTree.route
                    else -> currentRoute == item.destination.route
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.destination) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.sp,
                            ),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant,
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}
