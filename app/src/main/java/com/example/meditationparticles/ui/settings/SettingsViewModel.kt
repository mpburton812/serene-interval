package com.example.meditationparticles.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppGraph.settings(application)

    val settings: StateFlow<ExperienceSettings> = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExperienceSettings())

    fun setPreferredName(name: String) {
        preferences.update { it.copy(preferredName = name) }
    }

    fun setSanctuaryName(name: String) {
        preferences.update { it.copy(sanctuaryName = name) }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.update { it.copy(themeMode = mode) }
    }

    fun setEnableBreathing(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableBreathing = enabled) }
        }
    }

    fun setEnableTimer(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableTimer = enabled) }
        }
    }

    fun setEnableAffirmations(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableAffirmations = enabled) }
        }
    }

    fun setEnableToolkit(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableToolkit = enabled) }
        }
    }

    fun setEnableVisuals(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableVisuals = enabled) }
        }
    }

    fun toggleScene(id: CalmingVisualizationId) {
        preferences.update { current ->
            val scenes = current.enabledScenes.toMutableSet()
            if (scenes.contains(id.name)) {
                if (scenes.size > 1) scenes.remove(id.name)
            } else {
                scenes.add(id.name)
            }
            current.copy(enabledScenes = scenes)
        }
    }

    fun resetOnboarding() {
        preferences.update { it.copy(onboardingCompleted = false) }
    }
}
