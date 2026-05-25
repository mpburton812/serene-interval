package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.toolkit.ToolkitTool
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing
import com.example.meditationparticles.ui.theme.SereneTertiary

@Composable
fun ToolkitToolSelectionScreen(
    proactiveTools: List<ToolkitTool>,
    reactiveTools: List<ToolkitTool>,
    enabledToolIds: Set<ToolkitToolId>,
    onToggleTool: (ToolkitToolId) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val canContinue = enabledToolIds.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm)) {
            Text(
                text = "Choose Your Tools",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Select the Proactive Care and Reactive Relief tools you want in your toolkit.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ToolSelectionSection(
            title = "Proactive Care",
            icon = Icons.Default.Shield,
            titleColor = MaterialTheme.colorScheme.primary,
            tools = proactiveTools,
            enabledToolIds = enabledToolIds,
            onToggleTool = onToggleTool,
        )

        ToolSelectionSection(
            title = "Reactive Relief",
            icon = Icons.Default.Emergency,
            titleColor = SereneTertiary,
            tools = reactiveTools,
            enabledToolIds = enabledToolIds,
            onToggleTool = onToggleTool,
        )

        if (!canContinue) {
            Text(
                text = "Enable at least one tool to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = onContinue,
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = "Save & Continue",
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ToolSelectionSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleColor: androidx.compose.ui.graphics.Color,
    tools: List<ToolkitTool>,
    enabledToolIds: Set<ToolkitToolId>,
    onToggleTool: (ToolkitToolId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = titleColor)
            Text(text = title, style = MaterialTheme.typography.headlineMedium, color = titleColor)
        }

        Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter)) {
            tools.forEach { tool ->
                ToolSelectionRow(
                    tool = tool,
                    checked = tool.id in enabledToolIds,
                    onToggle = { onToggleTool(tool.id) },
                )
            }
        }
    }
}

@Composable
private fun ToolSelectionRow(
    tool: ToolkitTool,
    checked: Boolean,
    onToggle: () -> Unit,
) {
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
                onCheckedChange = { onToggle() },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tool.title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
