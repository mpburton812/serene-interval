package com.example.meditationparticles.testing

import android.content.Context
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.SettingsPreferences
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.SanctuaryLandscapeThemeId
import com.example.meditationparticles.domain.settings.ThemeMode

object InstrumentedTestFixtures {
    fun clearExperienceSettings(context: Context) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences("experience_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        AppGraph.settings(appContext).save(ExperienceSettings())
    }

    fun clearLaunchMigration(context: Context) {
        context.applicationContext
            .getSharedPreferences("app_launch_migration", Context.MODE_PRIVATE)
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
                // Classic uses WEBP assets; gradient XML drawables crash Compose painterResource.
                landscapeThemeId = SanctuaryLandscapeThemeId.Classic,
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

    fun prepareMainActivityTest(context: Context) {
        clearLaunchMigration(context)
        clearExperienceSettings(context)
        seedOnboardingComplete(context)
    }

    fun prepareFreshOnboarding(context: Context) {
        clearLaunchMigration(context)
        clearExperienceSettings(context)
    }
}
