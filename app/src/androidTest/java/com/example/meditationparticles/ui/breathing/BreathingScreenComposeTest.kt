package com.example.meditationparticles.ui.breathing

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.SanctuaryLandscapeThemeId
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.ui.settings.LocalExperienceSettings
import com.example.meditationparticles.ui.theme.SereneIntervalTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BreathingScreenComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsPatternChipsAndStartButton() {
        composeRule.setContent {
            SereneIntervalTheme(themeMode = ThemeMode.Light) {
                CompositionLocalProvider(
                    LocalExperienceSettings provides ExperienceSettings(
                        themeMode = ThemeMode.Light,
                        landscapeThemeId = SanctuaryLandscapeThemeId.Classic,
                    ),
                ) {
                    BreathingScreen()
                }
            }
        }

        composeRule.onNodeWithText("BREATHING PATTERN").assertIsDisplayed()
        composeRule.onNodeWithText(BreathingPattern.BoxBreathing.name).assertIsDisplayed()
        BreathingPattern.All.forEach { pattern ->
            composeRule.onNodeWithText(pattern.name).assertExists()
        }
        composeRule.onNodeWithContentDescription("Start").assertIsDisplayed()
    }

    @Test
    fun showsPatternPurposeWhenIdle() {
        composeRule.setContent {
            SereneIntervalTheme(themeMode = ThemeMode.Light) {
                CompositionLocalProvider(
                    LocalExperienceSettings provides ExperienceSettings(
                        themeMode = ThemeMode.Light,
                        landscapeThemeId = SanctuaryLandscapeThemeId.Classic,
                    ),
                ) {
                    BreathingScreen()
                }
            }
        }

        composeRule.onNodeWithText(BreathingPattern.BoxBreathing.purpose).assertIsDisplayed()
    }
}
