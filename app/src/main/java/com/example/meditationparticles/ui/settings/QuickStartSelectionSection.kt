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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.quickstart.QuickStartId
import com.example.meditationparticles.domain.quickstart.QuickStartLayout
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun QuickStartSelectionSection(
    settings: ExperienceSettings,
    selectedIds: List<QuickStartId>,
    onToggle: (QuickStartId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = QuickStartLayout.availableIds(settings)
    val selectionCount = selectedIds.size

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
            text = "Choose ${QuickStartLayout.SELECTION_COUNT} tools to feature on your home screen.",
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
                text = "Enable at least ${QuickStartLayout.SELECTION_COUNT} tools in Your Experience " +
                    "to choose your Quick Start row.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter)) {
            QuickStartLayout.displayOrder.forEach { id ->
                if (id in available) {
                    QuickStartSelectionRow(
                        id = id,
                        checked = id in selectedIds,
                        enabled = id in selectedIds || selectionCount < QuickStartLayout.SELECTION_COUNT,
                        onToggle = { onToggle(id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStartSelectionRow(
    id: QuickStartId,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val (label, icon) = quickStartMeta(id)

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
                onCheckedChange = { if (enabled || checked) onToggle() },
                enabled = enabled || checked,
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

private fun quickStartMeta(id: QuickStartId): Pair<String, ImageVector> = when (id) {
    QuickStartId.BREATHING -> "Breathing" to Icons.Default.Air
    QuickStartId.TIMER -> "Meditation" to Icons.Default.SelfImprovement
    QuickStartId.AFFIRMATIONS -> "Affirmations" to Icons.Default.AutoAwesome
    QuickStartId.TOOLKIT -> "Toolkit" to Icons.Default.Handyman
    QuickStartId.VISUALS -> "Visualizations" to Icons.Default.Landscape
}
