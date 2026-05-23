package com.example.meditationparticles.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppGraph.settings(application)

    private val _draft = MutableStateFlow(OnboardingDraft.from(preferences.load()))
    val draft: StateFlow<OnboardingDraft> = _draft.asStateFlow()

    fun setPreferredName(name: String) {
        _draft.update { it.copy(preferredName = name) }
    }

    fun setSanctuaryName(name: String) {
        _draft.update { it.copy(sanctuaryName = name) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _draft.update { it.copy(themeMode = mode) }
    }

    fun setEnableBreathing(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableBreathing = enabled) }
    }

    fun setEnableTimer(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableTimer = enabled) }
    }

    fun setEnableAffirmations(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableAffirmations = enabled) }
    }

    fun setEnableToolkit(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableToolkit = enabled) }
    }

    fun setEnableVisuals(enabled: Boolean) {
        _draft.update { it.withToolEnabled(enableVisuals = enabled) }
    }

    fun toggleScene(id: CalmingVisualizationId) {
        _draft.update { it.toggleScene(id) }
    }

    fun completeOnboarding() {
        val current = _draft.value
        if (!current.canComplete) return
        preferences.save(current.toExperienceSettings())
    }
}
