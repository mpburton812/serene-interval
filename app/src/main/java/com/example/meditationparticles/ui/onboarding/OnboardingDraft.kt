package com.example.meditationparticles.ui.onboarding

import com.example.meditationparticles.domain.quickstart.QuickStartId
import com.example.meditationparticles.domain.quickstart.QuickStartLayout
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId

data class OnboardingDraft(
    val preferredName: String = "",
    val sanctuaryName: String = "",
    val themeMode: ThemeMode = ThemeMode.Light,
    val enableBreathing: Boolean = true,
    val enableTimer: Boolean = true,
    val enableAffirmations: Boolean = true,
    val enableToolkit: Boolean = true,
    val enableVisuals: Boolean = true,
    val enabledScenes: Set<String> = ExperienceSettings.defaultScenes,
    val enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    val quickStartIds: List<QuickStartId> = emptyList(),
    val step: OnboardingStep = OnboardingStep.Customization,
    val permissionState: OnboardingPermissionState = OnboardingPermissionState(),
) {
    val canComplete: Boolean
        get() {
            val settings = previewExperienceSettings()
            val hasExperienceTool = enableBreathing || enableTimer || enableAffirmations ||
                enableToolkit || enableVisuals
            val toolkitReady = !enableToolkit || enabledToolkitTools.isNotEmpty()
            val quickStartReady = QuickStartLayout.hasValidSelection(quickStartIds, settings)
            return hasExperienceTool && toolkitReady && quickStartReady
        }

    fun toExperienceSettings(
        meditationRemindersAvailable: Boolean = true,
        futureSelfSchedulingAvailable: Boolean = true,
    ): ExperienceSettings = ExperienceSettings(
        themeMode = themeMode,
        preferredName = preferredName.trim(),
        sanctuaryName = sanctuaryName.trim(),
        onboardingCompleted = true,
        enableBreathing = enableBreathing,
        enableTimer = enableTimer,
        enableAffirmations = enableAffirmations,
        enableToolkit = enableToolkit,
        enableVisuals = enableVisuals,
        enabledScenes = enabledScenes,
        meditationRemindersAvailable = meditationRemindersAvailable,
        futureSelfSchedulingAvailable = futureSelfSchedulingAvailable,
    )

    companion object {
        fun from(
            settings: ExperienceSettings,
            quickStartIds: List<QuickStartId> = QuickStartLayout.defaultSelection(settings),
        ): OnboardingDraft = OnboardingDraft(
            preferredName = settings.preferredName,
            sanctuaryName = settings.sanctuaryName,
            themeMode = if (
                !settings.onboardingCompleted && settings.themeMode == ThemeMode.TimeResponsive
            ) {
                ThemeMode.Light
            } else {
                settings.themeMode
            },
            enableBreathing = settings.enableBreathing,
            enableTimer = settings.enableTimer,
            enableAffirmations = settings.enableAffirmations,
            enableToolkit = settings.enableToolkit,
            enableVisuals = settings.enableVisuals,
            enabledScenes = settings.enabledScenes,
            quickStartIds = QuickStartLayout.normalizeSelection(quickStartIds, settings),
        )
    }
}

fun OnboardingDraft.toggleQuickStart(id: QuickStartId): OnboardingDraft {
    val settings = previewExperienceSettings()
    return copy(quickStartIds = QuickStartLayout.toggleSelection(quickStartIds, id, settings))
}

private fun OnboardingDraft.pruneQuickStart(): OnboardingDraft {
    val settings = previewExperienceSettings()
    return copy(quickStartIds = QuickStartLayout.normalizeSelection(quickStartIds, settings))
}

private fun OnboardingDraft.previewExperienceSettings(): ExperienceSettings =
    toExperienceSettings().copy(onboardingCompleted = false)

fun OnboardingDraft.toggleToolkitTool(id: ToolkitToolId): OnboardingDraft {
    val next = enabledToolkitTools.toMutableSet()
    if (id in next) {
        if (next.size > 1) next.remove(id)
    } else {
        next.add(id)
    }
    return copy(enabledToolkitTools = next)
}

fun OnboardingDraft.toggleScene(id: CalmingVisualizationId): OnboardingDraft {
    val scenes = enabledScenes.toMutableSet()
    if (scenes.contains(id.name)) {
        if (scenes.size > 1) scenes.remove(id.name)
    } else {
        scenes.add(id.name)
    }
    return copy(enabledScenes = scenes)
}

fun OnboardingDraft.withToolEnabled(
    enableBreathing: Boolean = this.enableBreathing,
    enableTimer: Boolean = this.enableTimer,
    enableAffirmations: Boolean = this.enableAffirmations,
    enableToolkit: Boolean = this.enableToolkit,
    enableVisuals: Boolean = this.enableVisuals,
): OnboardingDraft {
    val next = copy(
        enableBreathing = enableBreathing,
        enableTimer = enableTimer,
        enableAffirmations = enableAffirmations,
        enableToolkit = enableToolkit,
        enableVisuals = enableVisuals,
    )
    return (if (next.canComplete) next else this).pruneQuickStart()
}
