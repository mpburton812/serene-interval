package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun ToolkitScreen(
    pendingNavigation: PendingToolkitNavigation? = null,
    onNavigateToBreathe: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ToolkitViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(SereneSpacing.containerMargin),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        Text(
            text = "Toolkit",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            if (!state.toolkitConfigured) {
                ToolkitToolSelectionScreen(
                    proactiveTools = state.selectionProactiveTools,
                    reactiveTools = state.selectionReactiveTools,
                    enabledToolIds = state.enabledToolIds,
                    onToggleTool = viewModel::toggleToolEnabled,
                    onContinue = viewModel::saveToolkitConfiguration,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AnxietyToolkitTab(
                    onNavigateToBreathe = onNavigateToBreathe,
                    pendingNavigation = pendingNavigation,
                    viewModel = viewModel,
                )
            }
        }
    }
}
