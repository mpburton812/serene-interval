package com.example.meditationparticles.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.quickstart.QuickStartLayout
import com.example.meditationparticles.domain.quickstart.QuickStartTarget
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun QuickStartSelectionSection(
    settings: ExperienceSettings,
    enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    selectedTargets: List<QuickStartTarget>,
    onToggle: (QuickStartTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = QuickStartLayout.availableTargets(settings, enabledToolkitTools)
    val selectionCount = selectedTargets.size

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        Text(
            text = "Quick Start",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Choose ${QuickStartLayout.SELECTION_COUNT} shortcuts for your home screen — " +
                "breathing patterns, toolkit tools, and more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "$selectionCount of ${QuickStartLayout.SELECTION_COUNT} selected",
            style = MaterialTheme.typography.labelMedium,
            color = if (selectionCount == QuickStartLayout.SELECTION_COUNT) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        if (available.size < QuickStartLayout.SELECTION_COUNT) {
            Text(
                text = "Enable at least ${QuickStartLayout.SELECTION_COUNT} options in Your Experience " +
                    "to choose your Quick Start row.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        val breathingAvailable = available.filterIsInstance<QuickStartTarget.Breathing>()
        val coreAvailable = available.filter {
            it is QuickStartTarget.Timer ||
                it is QuickStartTarget.Affirmations ||
                it is QuickStartTarget.Visuals
        }
        val toolkitAvailable = available.filterIsInstance<QuickStartTarget.Toolkit>()

        Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter)) {
            if (breathingAvailable.isNotEmpty()) {
                QuickStartGroupHeader("Breathing patterns")
                breathingAvailable.forEach { target ->
                    QuickStartSelectionRow(
                        target = target,
                        checked = target in selectedTargets,
                        enabled = target in selectedTargets ||
                            selectionCount < QuickStartLayout.SELECTION_COUNT,
                        onToggle = { onToggle(target) },
                    )
                }
            }
            if (coreAvailable.isNotEmpty()) {
                QuickStartGroupHeader("Other tools")
                coreAvailable.forEach { target ->
                    QuickStartSelectionRow(
                        target = target,
                        checked = target in selectedTargets,
                        enabled = target in selectedTargets ||
                            selectionCount < QuickStartLayout.SELECTION_COUNT,
                        onToggle = { onToggle(target) },
                    )
                }
            }
            if (toolkitAvailable.isNotEmpty()) {
                QuickStartGroupHeader("Toolkit tools")
                toolkitAvailable.forEach { target ->
                    QuickStartSelectionRow(
                        target = target,
                        checked = target in selectedTargets,
                        enabled = target in selectedTargets ||
                            selectionCount < QuickStartLayout.SELECTION_COUNT,
                        onToggle = { onToggle(target) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStartGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun QuickStartSelectionRow(
    target: QuickStartTarget,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val (label, icon) = quickStartMeta(target)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.01f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { if (enabled) onToggle() },
                enabled = enabled,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun quickStartMeta(target: QuickStartTarget): Pair<String, ImageVector> = when (target) {
    is QuickStartTarget.Breathing -> {
        val pattern = BreathingPattern.byId(target.patternId)
        pattern.name to Icons.Default.Air
    }
    QuickStartTarget.Timer -> "Meditation" to Icons.Default.SelfImprovement
    QuickStartTarget.Affirmations -> "Affirmations" to Icons.Default.AutoAwesome
    QuickStartTarget.Visuals -> "Visualizations" to Icons.Default.Landscape
    is QuickStartTarget.Toolkit -> {
        val title = ToolkitCatalog.byId(target.toolId)?.title ?: "Toolkit"
        title to Icons.Default.Handyman
    }
}
