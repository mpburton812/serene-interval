package com.example.meditationparticles.data

import android.content.Context
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ExperienceSettings> = _settings.asStateFlow()

    fun load(): ExperienceSettings {
        val themeName = prefs.getString(KEY_THEME_MODE, ThemeMode.TimeResponsive.name)
        val themeMode = runCatching {
            ThemeMode.valueOf(themeName ?: ThemeMode.TimeResponsive.name)
        }.getOrDefault(ThemeMode.TimeResponsive)

        val scenes = prefs.getStringSet(KEY_ENABLED_SCENES, null)
            ?: ExperienceSettings.defaultScenes

        val hasLegacySettings = prefs.contains(KEY_THEME_MODE) ||
            prefs.contains(KEY_ENABLE_BREATHING) ||
            prefs.contains(KEY_ENABLE_TIMER)

        val onboardingCompleted = when {
            prefs.contains(KEY_ONBOARDING_COMPLETED) ->
                prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
            hasLegacySettings -> true
            else -> false
        }

        return ExperienceSettings(
            themeMode = themeMode,
            preferredName = prefs.getString(KEY_PREFERRED_NAME, "") ?: "",
            sanctuaryName = prefs.getString(KEY_SANCTUARY_NAME, "") ?: "",
            onboardingCompleted = onboardingCompleted,
            enableBreathing = prefs.getBoolean(KEY_ENABLE_BREATHING, true),
            enableTimer = prefs.getBoolean(KEY_ENABLE_TIMER, true),
            enableAffirmations = prefs.getBoolean(KEY_ENABLE_AFFIRMATIONS, true),
            enableToolkit = prefs.getBoolean(KEY_ENABLE_TOOLKIT, true),
            enableVisuals = prefs.getBoolean(KEY_ENABLE_VISUALS, true),
            enabledScenes = scenes,
            meditationRemindersAvailable = prefs.getBoolean(KEY_MEDITATION_REMINDERS_AVAILABLE, true),
            futureSelfSchedulingAvailable = prefs.getBoolean(KEY_FUTURE_SELF_SCHEDULING_AVAILABLE, true),
        )
    }

    fun save(settings: ExperienceSettings) {
        prefs.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_PREFERRED_NAME, settings.preferredName.trim())
            .putString(KEY_SANCTUARY_NAME, settings.sanctuaryName.trim())
            .putBoolean(KEY_ONBOARDING_COMPLETED, settings.onboardingCompleted)
            .putBoolean(KEY_ENABLE_BREATHING, settings.enableBreathing)
            .putBoolean(KEY_ENABLE_TIMER, settings.enableTimer)
            .putBoolean(KEY_ENABLE_AFFIRMATIONS, settings.enableAffirmations)
            .putBoolean(KEY_ENABLE_TOOLKIT, settings.enableToolkit)
            .putBoolean(KEY_ENABLE_VISUALS, settings.enableVisuals)
            .putStringSet(KEY_ENABLED_SCENES, settings.enabledScenes)
            .putBoolean(KEY_MEDITATION_REMINDERS_AVAILABLE, settings.meditationRemindersAvailable)
            .putBoolean(KEY_FUTURE_SELF_SCHEDULING_AVAILABLE, settings.futureSelfSchedulingAvailable)
            .apply()
        _settings.update { settings }
    }

    fun update(transform: (ExperienceSettings) -> ExperienceSettings) {
        save(transform(_settings.value))
    }

    companion object {
        private const val PREFS_NAME = "experience_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_PREFERRED_NAME = "preferred_name"
        private const val KEY_SANCTUARY_NAME = "sanctuary_name"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ENABLE_BREATHING = "enable_breathing"
        private const val KEY_ENABLE_TIMER = "enable_timer"
        private const val KEY_ENABLE_AFFIRMATIONS = "enable_affirmations"
        private const val KEY_ENABLE_TOOLKIT = "enable_toolkit"
        private const val KEY_ENABLE_VISUALS = "enable_visuals"
        private const val KEY_ENABLED_SCENES = "enabled_scenes"
        private const val KEY_MEDITATION_REMINDERS_AVAILABLE = "meditation_reminders_available"
        private const val KEY_FUTURE_SELF_SCHEDULING_AVAILABLE = "future_self_scheduling_available"
    }
}
