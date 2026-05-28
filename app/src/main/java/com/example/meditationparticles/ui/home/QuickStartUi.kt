package com.example.meditationparticles.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.meditationparticles.domain.quickstart.QuickStartId
import com.example.meditationparticles.domain.quickstart.QuickStartLayout
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.navigation.SereneDestination

data class QuickStartTileModel(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val onClick: () -> Unit,
)

@Composable
fun buildQuickStartTiles(
    selectedIds: List<QuickStartId>,
    settings: ExperienceSettings,
    onNavigate: (SereneDestination, String?) -> Unit,
): List<QuickStartTileModel> {
    val scheme = MaterialTheme.colorScheme
    val resolved = QuickStartLayout.normalizeSelection(selectedIds, settings)
        .filter { QuickStartLayout.isToolEnabled(it, settings) }

    return resolved.map { id ->
        val presentation = quickStartPresentation(id, scheme)
        QuickStartTileModel(
            label = presentation.label,
            icon = presentation.icon,
            iconTint = presentation.iconTint,
            iconBackground = presentation.iconBackground,
            onClick = { onNavigate(presentation.destination, null) },
        )
    }
}

private data class QuickStartPresentation(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val destination: SereneDestination,
)

@Composable
private fun quickStartPresentation(
    id: QuickStartId,
    scheme: androidx.compose.material3.ColorScheme,
): QuickStartPresentation = when (id) {
    QuickStartId.BREATHING -> QuickStartPresentation(
        label = "Breathing",
        icon = Icons.Default.Air,
        iconTint = scheme.secondary,
        iconBackground = scheme.secondaryContainer.copy(alpha = 0.4f),
        destination = SereneDestination.Breathe,
    )
    QuickStartId.TIMER -> QuickStartPresentation(
        label = "Meditation",
        icon = Icons.Default.SelfImprovement,
        iconTint = scheme.primary,
        iconBackground = scheme.primaryContainer.copy(alpha = 0.4f),
        destination = SereneDestination.Timer,
    )
    QuickStartId.AFFIRMATIONS -> QuickStartPresentation(
        label = "Affirmations",
        icon = Icons.Default.AutoAwesome,
        iconTint = scheme.tertiary,
        iconBackground = scheme.tertiaryContainer.copy(alpha = 0.3f),
        destination = SereneDestination.Affirmations,
    )
    QuickStartId.TOOLKIT -> QuickStartPresentation(
        label = "Toolkit",
        icon = Icons.Default.Handyman,
        iconTint = scheme.onSurfaceVariant,
        iconBackground = scheme.surfaceContainerHigh,
        destination = SereneDestination.Toolkit,
    )
    QuickStartId.VISUALS -> QuickStartPresentation(
        label = "Visualizations",
        icon = Icons.Default.Landscape,
        iconTint = scheme.tertiary,
        iconBackground = scheme.tertiaryContainer.copy(alpha = 0.25f),
        destination = SereneDestination.Visualizations,
    )
}
