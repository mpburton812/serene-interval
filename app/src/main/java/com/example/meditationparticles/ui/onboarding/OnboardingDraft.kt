package com.example.meditationparticles.ui.onboarding

import com.example.meditationparticles.domain.quickstart.QuickStartLayout
import com.example.meditationparticles.domain.quickstart.QuickStartTarget
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
    val enableKatiesLoveList: Boolean = false,
    val enableToolkit: Boolean = true,
    val enableLivingTree: Boolean = true,
    val enableVisuals: Boolean = true,
    val enabledScenes: Set<String> = ExperienceSettings.defaultScenes,
    val enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    val quickStartTargets: List<QuickStartTarget> = emptyList(),
    val step: OnboardingStep = OnboardingStep.Customization,
    val permissionState: OnboardingPermissionState = OnboardingPermissionState(),
) {
    val canComplete: Boolean
        get() {
            val settings = previewExperienceSettings()
            val hasExperienceTool = enableBreathing || enableTimer || enableAffirmations ||
                enableKatiesLoveList || enableToolkit || enableLivingTree || enableVisuals
            val quickStartReady = QuickStartLayout.hasValidSelection(
                quickStartTargets,
                settings,
                enabledToolkitTools,
            )
            return hasExperienceTool && quickStartReady
        }

    val toolkitTabVisible: Boolean
        get() = enableToolkit && enabledToolkitTools.isNotEmpty()

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
        enableKatiesLoveList = enableKatiesLoveList,
        enableToolkit = toolkitTabVisible,
        enableLivingTree = enableLivingTree,
        enableVisuals = enableVisuals,
        enabledScenes = enabledScenes,
        meditationRemindersAvailable = meditationRemindersAvailable,
        futureSelfSchedulingAvailable = futureSelfSchedulingAvailable,
    )

    companion object {
        fun from(
            settings: ExperienceSettings,
            quickStartTargets: List<QuickStartTarget> = QuickStartLayout.defaultSelection(settings),
            enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
        ): OnboardingDraft {
            val toolkitOn = settings.enableToolkit && enabledToolkitTools.isNotEmpty()
            return OnboardingDraft(
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
                enableKatiesLoveList = settings.enableKatiesLoveList,
                enableToolkit = toolkitOn,
                enableLivingTree = settings.enableLivingTree,
                enableVisuals = settings.enableVisuals,
                enabledScenes = settings.enabledScenes,
                enabledToolkitTools = enabledToolkitTools,
                quickStartTargets = QuickStartLayout.normalizeSelection(
                    quickStartTargets,
                    settings.copy(enableToolkit = toolkitOn),
                    enabledToolkitTools,
                ),
            )
        }
    }
}

fun OnboardingDraft.toggleQuickStart(target: QuickStartTarget): OnboardingDraft {
    val settings = previewExperienceSettings()
    return copy(
        quickStartTargets = QuickStartLayout.toggleSelection(
            quickStartTargets,
            target,
            settings,
            enabledToolkitTools,
        ),
    )
}

private fun OnboardingDraft.pruneQuickStart(): OnboardingDraft {
    val settings = previewExperienceSettings()
    return copy(
        quickStartTargets = QuickStartLayout.sanitizeSelection(
            quickStartTargets,
            settings,
            enabledToolkitTools,
        ),
    )
}

private fun OnboardingDraft.previewExperienceSettings(): ExperienceSettings =
    toExperienceSettings().copy(onboardingCompleted = false)

fun OnboardingDraft.toggleToolkitTool(id: ToolkitToolId): OnboardingDraft {
    val next = enabledToolkitTools.toMutableSet()
    if (id in next) {
        next.remove(id)
    } else {
        next.add(id)
    }
    val toolkitOn = next.isNotEmpty()
    return copy(
        enabledToolkitTools = next,
        enableToolkit = toolkitOn,
    ).pruneQuickStart()
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
    enableKatiesLoveList: Boolean = this.enableKatiesLoveList,
    enableToolkit: Boolean = this.enableToolkit,
    enableLivingTree: Boolean = this.enableLivingTree,
    enableVisuals: Boolean = this.enableVisuals,
): OnboardingDraft {
    val next = copy(
        enableBreathing = enableBreathing,
        enableTimer = enableTimer,
        enableAffirmations = enableAffirmations,
        enableKatiesLoveList = enableKatiesLoveList,
        enableToolkit = enableToolkit && enabledToolkitTools.isNotEmpty(),
        enableLivingTree = enableLivingTree,
        enableVisuals = enableVisuals,
    )
    return (if (next.canComplete || !next.enableToolkit) next else this).pruneQuickStart()
}
