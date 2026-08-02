package com.safehaven.affirmations.ui.breathing

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.safehaven.affirmations.domain.breathing.BreathingPattern
import com.safehaven.affirmations.ui.theme.SereneIntervalTheme
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
            SereneIntervalTheme {
                BreathingScreen()
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
            SereneIntervalTheme {
                BreathingScreen()
            }
        }

        composeRule.onNodeWithText(BreathingPattern.BoxBreathing.purpose).assertIsDisplayed()
    }
}
