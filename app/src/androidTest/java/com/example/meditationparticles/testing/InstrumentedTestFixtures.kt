package com.example.meditationparticles.testing

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.SanctuaryLandscapeThemeId
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitToolId

object InstrumentedTestFixtures {
    private const val PREFS_NAME = "experience_settings"
    private const val TOOLKIT_PREFS_NAME = "toolkit_preferences"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LANDSCAPE_THEME_ID = "landscape_theme_id"
    private const val KEY_PREFERRED_NAME = "preferred_name"
    private const val KEY_SANCTUARY_NAME = "sanctuary_name"
    private const val KEY_ENABLE_BREATHING = "enable_breathing"
    private const val KEY_ENABLE_TIMER = "enable_timer"
    private const val KEY_ENABLE_AFFIRMATIONS = "enable_affirmations"
    private const val KEY_ENABLE_TOOLKIT = "enable_toolkit"
    private const val KEY_ENABLE_VISUALS = "enable_visuals"
    private const val KEY_ENABLE_LIVING_TREE = "enable_living_tree"
    private const val KEY_TOOLKIT_CONFIGURED = "toolkit_configured"
    private const val KEY_TOOLKIT_ENABLED_TOOLS = "enabled_tool_ids"

    fun targetContext(): Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    fun prepareMainActivityTest(context: Context = targetContext()) {
        AppGraph.resetCachedStateForInstrumentation()
        clearLaunchMigration(context)
        clearExperienceSettingsOnDisk(context)
        seedDefaultToolkitToolsOnDisk(context)
        persistExperienceSettingsOnDisk(
            context,
            ExperienceSettings(
                themeMode = ThemeMode.Light,
                // Classic uses WEBP assets; gradient XML drawables crash Compose painterResource.
                landscapeThemeId = SanctuaryLandscapeThemeId.Classic,
                preferredName = "Test",
                sanctuaryName = "Test Sanctuary",
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

    fun prepareFreshOnboarding(context: Context = targetContext()) {
        AppGraph.resetCachedStateForInstrumentation()
        clearLaunchMigration(context)
        clearExperienceSettingsOnDisk(context)
        clearToolkitPreferencesOnDisk(context)
    }

    fun dismissUpdateDialogIfShown(composeRule: ComposeTestRule) {
        val laterNodes = composeRule.onAllNodesWithText("Later").fetchSemanticsNodes()
        if (laterNodes.isNotEmpty()) {
            composeRule.onNodeWithText("Later").performClick()
            composeRule.waitForIdle()
        }
    }

    fun waitForHomeScreen(composeRule: ComposeTestRule, timeoutMillis: Long = 30_000) {
        composeRule.waitUntil(timeoutMillis) {
            dismissUpdateDialogIfShown(composeRule)
            composeRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty() &&
                composeRule.onAllNodesWithText("Build your Sway")
                    .fetchSemanticsNodes()
                    .isEmpty()
        }
    }

    private fun clearExperienceSettingsOnDisk(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun clearToolkitPreferencesOnDisk(context: Context) {
        context.applicationContext
            .getSharedPreferences(TOOLKIT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun clearLaunchMigration(context: Context) {
        context.applicationContext
            .getSharedPreferences("app_launch_migration", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private fun seedDefaultToolkitToolsOnDisk(context: Context) {
        val enabledTools = ToolkitLayout.defaultEnabledTools()
        context.applicationContext
            .getSharedPreferences(TOOLKIT_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TOOLKIT_CONFIGURED, true)
            .putStringSet(
                KEY_TOOLKIT_ENABLED_TOOLS,
                enabledTools.map(ToolkitToolId::name).toSet(),
            )
            .commit()
    }

    private fun persistExperienceSettingsOnDisk(context: Context, settings: ExperienceSettings) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_LANDSCAPE_THEME_ID, settings.landscapeThemeId.name)
            .putString(KEY_PREFERRED_NAME, settings.preferredName.trim())
            .putString(KEY_SANCTUARY_NAME, settings.sanctuaryName.trim())
            .putBoolean(KEY_ONBOARDING_COMPLETED, settings.onboardingCompleted)
            .putBoolean(KEY_ENABLE_BREATHING, settings.enableBreathing)
            .putBoolean(KEY_ENABLE_TIMER, settings.enableTimer)
            .putBoolean(KEY_ENABLE_AFFIRMATIONS, settings.enableAffirmations)
            .putBoolean(KEY_ENABLE_TOOLKIT, settings.enableToolkit)
            .putBoolean(KEY_ENABLE_VISUALS, settings.enableVisuals)
            .putBoolean(KEY_ENABLE_LIVING_TREE, settings.enableLivingTree)
            .commit()
    }
}
