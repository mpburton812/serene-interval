package com.example.meditationparticles.domain.settings

import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId

data class ExperienceSettings(
    val themeMode: ThemeMode = ThemeMode.TimeResponsive,
    val preferredName: String = "",
    val sanctuaryName: String = "",
    val onboardingCompleted: Boolean = false,
    val enableBreathing: Boolean = true,
    val enableTimer: Boolean = true,
    val enableAffirmations: Boolean = true,
    val enableToolkit: Boolean = true,
    val enableVisuals: Boolean = true,
    val enabledScenes: Set<String> = defaultScenes,
    val meditationRemindersAvailable: Boolean = true,
    val futureSelfSchedulingAvailable: Boolean = true,
) {
    val showToolkitTab: Boolean get() = enableToolkit

    val hasAnyToolEnabled: Boolean
        get() = enableBreathing || enableTimer || enableAffirmations || enableToolkit || enableVisuals

    val displayName: String get() = preferredName.trim().ifBlank { "there" }

    val sanctuaryTitle: String get() = sanctuaryName.trim().ifBlank { "Your Sanctuary" }

    fun withToolToggle(transform: (ExperienceSettings) -> ExperienceSettings): ExperienceSettings {
        val next = transform(this)
        return if (next.hasAnyToolEnabled) next else this
    }

    companion object {
        val defaultScenes: Set<String> = CalmingVisualizationId.entries.map { it.name }.toSet()
    }
}
