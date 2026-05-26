package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.ui.components.SereneTabBackground
import com.example.meditationparticles.ui.components.SereneTabHeader
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun ToolkitScreen(
    pendingNavigation: PendingToolkitNavigation? = null,
    resetSignal: Int = 0,
    onNavigateToBreathe: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ToolkitViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            viewModel.closeTool()
        }
    }

    SereneTabBackground(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
        SereneTabHeader(title = "Toolkit")

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = SereneSpacing.containerMargin),
        ) {
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
}
