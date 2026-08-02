package com.safehaven.affirmations.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.safehaven.affirmations.domain.breathing.BreathingPattern
import com.safehaven.affirmations.domain.quickstart.QuickStartLayout
import com.safehaven.affirmations.domain.quickstart.QuickStartTarget
import com.safehaven.affirmations.domain.settings.ExperienceSettings
import com.safehaven.affirmations.domain.toolkit.ToolkitCatalog
import com.safehaven.affirmations.domain.toolkit.ToolkitLayout
import com.safehaven.affirmations.domain.toolkit.ToolkitToolId

data class QuickStartTileModel(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
    val onClick: () -> Unit,
)

@Composable
fun buildQuickStartTiles(
    selectedTargets: List<QuickStartTarget>,
    settings: ExperienceSettings,
    enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    onQuickStart: (QuickStartTarget) -> Unit,
): List<QuickStartTileModel> {
    val scheme = MaterialTheme.colorScheme
    val resolved = QuickStartLayout.sanitizeSelection(selectedTargets, settings, enabledToolkitTools)
        .filter { QuickStartLayout.isTargetEnabled(it, settings, enabledToolkitTools) }

    return resolved.map { target ->
        val presentation = quickStartPresentation(target, scheme)
        QuickStartTileModel(
            label = presentation.label,
            icon = presentation.icon,
            iconTint = presentation.iconTint,
            iconBackground = presentation.iconBackground,
            onClick = { onQuickStart(target) },
        )
    }
}

private data class QuickStartPresentation(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBackground: Color,
)

@Composable
private fun quickStartPresentation(
    target: QuickStartTarget,
    scheme: androidx.compose.material3.ColorScheme,
): QuickStartPresentation = when (target) {
    is QuickStartTarget.Breathing -> {
        val pattern = BreathingPattern.byId(target.patternId)
        QuickStartPresentation(
            label = pattern.name,
            icon = Icons.Default.Air,
            iconTint = scheme.secondary,
            iconBackground = scheme.secondaryContainer.copy(alpha = 0.4f),
        )
    }
    QuickStartTarget.Timer -> QuickStartPresentation(
        label = "Meditation",
        icon = Icons.Default.SelfImprovement,
        iconTint = scheme.primary,
        iconBackground = scheme.primaryContainer.copy(alpha = 0.4f),
    )
    QuickStartTarget.Affirmations -> QuickStartPresentation(
        label = "Affirmations",
        icon = Icons.Default.AutoAwesome,
        iconTint = scheme.tertiary,
        iconBackground = scheme.tertiaryContainer.copy(alpha = 0.3f),
    )
    QuickStartTarget.KatiesLoveList -> QuickStartPresentation(
        label = "Love List",
        icon = Icons.Default.Favorite,
        iconTint = scheme.tertiary,
        iconBackground = scheme.tertiaryContainer.copy(alpha = 0.3f),
    )
    is QuickStartTarget.Toolkit -> {
        val title = ToolkitCatalog.byId(target.toolId)?.title ?: "Toolkit"
        QuickStartPresentation(
            label = title,
            icon = Icons.Default.Handyman,
            iconTint = scheme.onSurfaceVariant,
            iconBackground = scheme.surfaceContainerHigh,
        )
    }
    QuickStartTarget.Visuals -> QuickStartPresentation(
        label = "Visualizations",
        icon = Icons.Default.Landscape,
        iconTint = scheme.tertiary,
        iconBackground = scheme.tertiaryContainer.copy(alpha = 0.25f),
    )
}
