package com.example.meditationparticles.ui.onboarding

import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId

data class OnboardingDraft(
    val preferredName: String = "",
    val sanctuaryName: String = "",
    val themeMode: ThemeMode = ThemeMode.TimeResponsive,
    val enableBreathing: Boolean = true,
    val enableTimer: Boolean = true,
    val enableAffirmations: Boolean = true,
    val enableToolkit: Boolean = true,
    val enableVisuals: Boolean = true,
    val enabledScenes: Set<String> = ExperienceSettings.defaultScenes,
    val enabledToolkitTools: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
) {
    val canComplete: Boolean
        get() {
            val hasExperienceTool = enableBreathing || enableTimer || enableAffirmations ||
                enableToolkit || enableVisuals
            val toolkitReady = !enableToolkit || enabledToolkitTools.isNotEmpty()
            return hasExperienceTool && toolkitReady
        }

    fun toExperienceSettings(): ExperienceSettings = ExperienceSettings(
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
    )

    companion object {
        fun from(settings: ExperienceSettings): OnboardingDraft = OnboardingDraft(
            preferredName = settings.preferredName,
            sanctuaryName = settings.sanctuaryName,
            themeMode = settings.themeMode,
            enableBreathing = settings.enableBreathing,
            enableTimer = settings.enableTimer,
            enableAffirmations = settings.enableAffirmations,
            enableToolkit = settings.enableToolkit,
            enableVisuals = settings.enableVisuals,
            enabledScenes = settings.enabledScenes,
        )
    }
}

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
    return if (next.canComplete) next else this
}
