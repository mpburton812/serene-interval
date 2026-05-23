package com.example.meditationparticles.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.settings.ExperienceSection
import com.example.meditationparticles.ui.settings.PreferredNameField
import com.example.meditationparticles.ui.settings.SanctuaryNameField
import com.example.meditationparticles.ui.settings.ThemeSection
import com.example.meditationparticles.ui.settings.VisualSanctuarySection
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val draft by viewModel.draft.collectAsState()
    val settingsPreview = draft.toExperienceSettings().copy(onboardingCompleted = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(SereneSpacing.containerMargin),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
    ) {
        Spacer(modifier = Modifier.height(SereneSpacing.stackMd))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
        ) {
            Text(
                text = "Create Your Sanctuary",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Let's shape a space that feels uniquely yours.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            Column(
                modifier = Modifier.padding(SereneSpacing.containerMargin),
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
            ) {
                SanctuaryNameField(
                    value = draft.sanctuaryName,
                    onValueChange = viewModel::setSanctuaryName,
                )

                PreferredNameField(
                    value = draft.preferredName,
                    onValueChange = viewModel::setPreferredName,
                )

                ThemeSection(
                    settings = settingsPreview,
                    onThemeModeSelected = viewModel::setThemeMode,
                )

                ExperienceSection(
                    settings = settingsPreview,
                    onBreathingChanged = viewModel::setEnableBreathing,
                    onTimerChanged = viewModel::setEnableTimer,
                    onAffirmationsChanged = viewModel::setEnableAffirmations,
                    onToolkitChanged = viewModel::setEnableToolkit,
                    onVisualsChanged = viewModel::setEnableVisuals,
                )

                if (draft.enableVisuals) {
                    VisualSanctuarySection(
                        enabledScenes = draft.enabledScenes,
                        onToggleScene = viewModel::toggleScene,
                    )
                }
            }
        }

        if (!draft.canComplete) {
            Text(
                text = "Keep at least one tool enabled to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Button(
            onClick = {
                viewModel.completeOnboarding()
                onComplete()
            },
            enabled = draft.canComplete,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SereneSpacing.stackLg),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                text = "Enter Your Sanctuary",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
