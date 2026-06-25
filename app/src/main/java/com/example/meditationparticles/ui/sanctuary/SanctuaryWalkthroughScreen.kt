package com.example.meditationparticles.ui.sanctuary

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitLane
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.settings.ExperienceSection
import com.example.meditationparticles.ui.settings.LandscapeThemeSection
import com.example.meditationparticles.ui.settings.NamingSection
import com.example.meditationparticles.ui.settings.QuickStartSelectionSection
import com.example.meditationparticles.ui.settings.ThemeSection
import com.example.meditationparticles.ui.settings.VisualSanctuarySection
import com.example.meditationparticles.ui.theme.SereneSpacing
import com.example.meditationparticles.ui.toolkit.ToolkitToolSelectionContent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SanctuaryWalkthroughScreen(
    mode: SanctuaryWalkthroughMode,
    onFinished: () -> Unit,
    onBackOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SanctuaryWalkthroughViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val draft by viewModel.draft.collectAsState()
    val currentStep by viewModel.walkthroughStep.collectAsState()
    val settingsPreview = draft.toExperienceSettings().copy(onboardingCompleted = false)
    val steps = viewModel.visibleSteps(draft)
    if (steps.isEmpty()) return

    val stepIndex = steps.indexOf(currentStep).coerceIn(0, steps.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = stepIndex,
        pageCount = { steps.size },
    )

    LaunchedEffect(currentStep, steps) {
        val target = steps.indexOf(currentStep).coerceIn(0, steps.lastIndex)
        if (target != pagerState.currentPage) {
            pagerState.scrollToPage(target)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(SereneSpacing.containerMargin),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        WalkthroughHeader(mode = mode, step = currentStep)

        if (steps.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { (stepIndex + 1).toFloat() / steps.size },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Step ${stepIndex + 1} of ${steps.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = false,
        ) { page ->
            val step = steps.getOrElse(page) { steps.first() }
            WalkthroughStepContent(
                step = step,
                draft = draft,
                settingsPreview = settingsPreview,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val validationMessage = validationMessageFor(currentStep, draft)
        validationMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
        ) {
            if (stepIndex > 0) {
                OutlinedButton(
                    onClick = { viewModel.goBack() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Back")
                }
            } else if (mode == SanctuaryWalkthroughMode.Remodel) {
                OutlinedButton(
                    onClick = onBackOut,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancel")
                }
            }

            val isLast = stepIndex >= steps.lastIndex
            Button(
                onClick = {
                    if (isLast) {
                        onFinished()
                    } else {
                        viewModel.goNext()
                    }
                },
                enabled = viewModel.canAdvanceFrom(currentStep, draft),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = when {
                        isLast && mode == SanctuaryWalkthroughMode.Remodel -> "Save changes"
                        isLast -> "Continue"
                        else -> "Next"
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(SereneSpacing.stackSm))
    }
}

@Composable
private fun WalkthroughHeader(
    mode: SanctuaryWalkthroughMode,
    step: SanctuaryWalkthroughStep,
) {
    val (title, subtitle) = when (step) {
        SanctuaryWalkthroughStep.Welcome -> when (mode) {
            SanctuaryWalkthroughMode.FirstVisit -> "Build your Sway" to
                "A personal calm space for breath, meditation, affirmations, and gentle anxiety tools."
            SanctuaryWalkthroughMode.Remodel -> "Remodel your Sway" to
                "Change your spaces and tools anytime. Your journals and history stay safe."
        }
        else -> step.title to stepSubtitle(step)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun stepSubtitle(step: SanctuaryWalkthroughStep): String = when (step) {
    SanctuaryWalkthroughStep.Welcome -> ""
    SanctuaryWalkthroughStep.Name -> "Name this space and how we greet you."
    SanctuaryWalkthroughStep.Appearance -> "Choose colors and a tab backdrop landscape."
    SanctuaryWalkthroughStep.Spaces -> "Turn on the areas you want in your Sway."
    SanctuaryWalkthroughStep.QuickStart -> "Pick four home-screen shortcuts."
    SanctuaryWalkthroughStep.Toolkit -> "Select practices to include. Your past entries are never deleted."
    SanctuaryWalkthroughStep.Review -> "Confirm your setup before continuing."
}

@Composable
private fun WalkthroughStepContent(
    step: SanctuaryWalkthroughStep,
    draft: com.example.meditationparticles.ui.onboarding.OnboardingDraft,
    settingsPreview: ExperienceSettings,
    viewModel: SanctuaryWalkthroughViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        when (step) {
            SanctuaryWalkthroughStep.Welcome -> WelcomeStepContent()
            SanctuaryWalkthroughStep.Name -> WalkthroughCard {
                NamingSection(
                    sanctuaryName = draft.sanctuaryName,
                    onSanctuaryNameChange = viewModel::setSanctuaryName,
                    preferredName = draft.preferredName,
                    onPreferredNameChange = viewModel::setPreferredName,
                )
            }
            SanctuaryWalkthroughStep.Appearance -> WalkthroughCard {
                ThemeSection(
                    settings = settingsPreview,
                    onThemeModeSelected = viewModel::setThemeMode,
                )
                LandscapeThemeSection(
                    settings = settingsPreview,
                    onLandscapeThemeSelected = viewModel::setLandscapeTheme,
                )
            }
            SanctuaryWalkthroughStep.Spaces -> WalkthroughCard {
                SpacesStepContent(
                    draft = draft,
                    settings = settingsPreview,
                    viewModel = viewModel,
                )
            }
            SanctuaryWalkthroughStep.QuickStart -> WalkthroughCard {
                QuickStartSelectionSection(
                    settings = settingsPreview,
                    enabledToolkitTools = draft.enabledToolkitTools,
                    selectedTargets = draft.quickStartTargets,
                    onToggle = viewModel::toggleQuickStart,
                )
            }
            SanctuaryWalkthroughStep.Toolkit -> ToolkitStepContent(
                draft = draft,
                viewModel = viewModel,
            )
            SanctuaryWalkthroughStep.Review -> ReviewStepContent(draft = draft)
        }
    }
}

@Composable
private fun WelcomeStepContent() {
    WalkthroughCard {
        Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
            Text(
                text = "Sway helps you downshift stress and show up with more presence.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "• Breathe and pause when stress spikes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "• Practice with timer and affirmations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "• Process thoughts with guided toolkit exercises",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "• Track connections with your Living Flower",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "You choose what belongs here — nothing is required except what helps you feel grounded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SpacesStepContent(
    draft: com.example.meditationparticles.ui.onboarding.OnboardingDraft,
    settings: ExperienceSettings,
    viewModel: SanctuaryWalkthroughViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm)) {
        Text(
            text = "Each space becomes a tab in your Sway. Turn off what you do not need right now.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExperienceSection(
            settings = settings,
            onBreathingChanged = viewModel::setEnableBreathing,
            onTimerChanged = viewModel::setEnableTimer,
            onAffirmationsChanged = viewModel::setEnableAffirmations,
            onToolkitChanged = viewModel::setEnableToolkit,
            onLivingTreeChanged = viewModel::setEnableLivingTree,
            onVisualsChanged = viewModel::setEnableVisuals,
        )
        if (settings.enableVisuals) {
            VisualSanctuarySection(
                enabledScenes = draft.enabledScenes,
                onToggleScene = viewModel::toggleScene,
            )
        }
    }
}

@Composable
private fun ToolkitStepContent(
    draft: com.example.meditationparticles.ui.onboarding.OnboardingDraft,
    viewModel: SanctuaryWalkthroughViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Text(
            text = "Proactive tools build resilience; reactive tools help in anxious moments. " +
                "HEARTS tools support secure connection.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!draft.toolkitTabVisible) {
            Text(
                text = "No toolkit tools selected — the Toolkit tab will be hidden. " +
                    "Enable any tool below to bring it back. Past entries remain on your home history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        ToolkitToolSelectionContent(
            proactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Core) +
                ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Hearts),
            reactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Core) +
                ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Hearts),
            enabledToolIds = draft.enabledToolkitTools,
            onToggleTool = viewModel::toggleToolkitTool,
            showHeader = false,
        )
    }
}

@Composable
private fun ReviewStepContent(
    draft: com.example.meditationparticles.ui.onboarding.OnboardingDraft,
) {
    val settings = draft.toExperienceSettings().copy(onboardingCompleted = false)
    val spaces = buildList {
        if (settings.enableBreathing) add("Breathing")
        if (settings.enableTimer) add("Timer")
        if (settings.enableAffirmations) add("Affirmations")
        if (draft.toolkitTabVisible) add("Toolkit (${draft.enabledToolkitTools.size} tools)")
        if (settings.enableLivingTree) add("Living Flower")
        if (settings.enableVisuals) add("Visualizations")
    }

    WalkthroughCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReviewRow("Sway name", settings.sanctuaryTitle)
            if (settings.preferredName.isNotBlank()) {
                ReviewRow("Greeting", settings.preferredName)
            }
            ReviewRow("Appearance", "${settings.themeMode.label}, ${settings.landscapeThemeId.label}")
            ReviewRow("Spaces", spaces.joinToString(", ").ifBlank { "None" })
            ReviewRow("Quick Start", "${draft.quickStartTargets.size} shortcuts")
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WalkthroughCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Column(
            modifier = Modifier.padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
            content = { content() },
        )
    }
}

private fun validationMessageFor(
    step: SanctuaryWalkthroughStep,
    draft: com.example.meditationparticles.ui.onboarding.OnboardingDraft,
): String? = when (step) {
    SanctuaryWalkthroughStep.Spaces -> if (
        !draft.enableBreathing && !draft.enableTimer && !draft.enableAffirmations &&
        !draft.enableToolkit && !draft.enableLivingTree && !draft.enableVisuals
    ) {
        "Enable at least one space to continue."
    } else {
        null
    }
    SanctuaryWalkthroughStep.QuickStart -> if (draft.quickStartTargets.size < 4) {
        "Choose 4 Quick Start shortcuts to continue."
    } else {
        null
    }
    SanctuaryWalkthroughStep.Review -> if (!draft.canComplete) {
        "Complete the required choices on earlier steps."
    } else {
        null
    }
    else -> null
}
