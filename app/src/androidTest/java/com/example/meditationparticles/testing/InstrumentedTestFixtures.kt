package com.example.meditationparticles.testing

import android.content.Context
import com.example.meditationparticles.data.SettingsPreferences
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.ThemeMode

object InstrumentedTestFixtures {
    fun clearExperienceSettings(context: Context) {
        context.applicationContext
            .getSharedPreferences("experience_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun seedOnboardingComplete(
        context: Context,
        preferredName: String = "Test",
        sanctuaryName: String = "Test Sanctuary",
    ) {
        SettingsPreferences(context.applicationContext).save(
            ExperienceSettings(
                themeMode = ThemeMode.Light,
                preferredName = preferredName,
                sanctuaryName = sanctuaryName,
                onboardingCompleted = true,
                enableBreathing = true,
                enableTimer = true,
                enableAffirmations = true,
                enableToolkit = true,
                enableLivingTree = true,
                enableVisuals = true,
            ),
        )
    }
}
