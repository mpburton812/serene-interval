package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.navigation.PendingToolkitNavigation
import com.example.meditationparticles.navigation.SereneDestination
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.theme.SereneSpacing

enum class ToolkitScreenTab { Affirmations, Toolkit }

@Composable
fun ToolkitScreen(
    initialTab: String = SereneDestination.ToolkitTab.AFFIRMATIONS,
    pendingNavigation: PendingToolkitNavigation? = null,
    onNavigateToBreathe: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val settings = LocalExperienceSettings.current
    val showAffirmations = settings.enableAffirmations
    val showToolkit = settings.enableToolkit
    val showTabSwitcher = showAffirmations && showToolkit

    var selectedTab by rememberSaveable {
        mutableStateOf(
            when {
                initialTab == SereneDestination.ToolkitTab.TOOLKIT && showToolkit -> ToolkitScreenTab.Toolkit
                showAffirmations -> ToolkitScreenTab.Affirmations
                showToolkit -> ToolkitScreenTab.Toolkit
                else -> ToolkitScreenTab.Affirmations
            },
        )
    }

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

        if (showTabSwitcher) {
            ToolkitTabSwitcher(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                selectedTab == ToolkitScreenTab.Affirmations && showAffirmations -> AffirmationsTab()
                selectedTab == ToolkitScreenTab.Toolkit && showToolkit ->
                    AnxietyToolkitTab(
                        onNavigateToBreathe = onNavigateToBreathe,
                        pendingNavigation = pendingNavigation,
                    )
            }
        }
    }
}

@Composable
private fun ToolkitTabSwitcher(
    selectedTab: ToolkitScreenTab,
    onTabSelected: (ToolkitScreenTab) -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 999.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToolkitTabButton(
                label = "Affirmations",
                selected = selectedTab == ToolkitScreenTab.Affirmations,
                onClick = { onTabSelected(ToolkitScreenTab.Affirmations) },
                modifier = Modifier.weight(1f),
            )
            ToolkitTabButton(
                label = "Toolkit",
                selected = selectedTab == ToolkitScreenTab.Toolkit,
                onClick = { onTabSelected(ToolkitScreenTab.Toolkit) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToolkitTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.clip(shape),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}
