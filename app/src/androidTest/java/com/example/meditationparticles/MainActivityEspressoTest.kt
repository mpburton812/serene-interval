package com.example.meditationparticles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.meditationparticles.testing.InstrumentedTestFixtures
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full-app instrumented tests. Prefs are seeded in [setUp] before [MainActivity] is launched
 * so onboarding is skipped and the home screen is shown.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        InstrumentedTestFixtures.prepareMainActivityTest()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.waitForIdle()
        InstrumentedTestFixtures.dismissUpdateDialogIfShown(composeTestRule)
        InstrumentedTestFixtures.waitForHomeScreen(composeTestRule)
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }

    @Test
    fun launchesHomeWithBottomNavigation() {
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Breathe").assertIsDisplayed()
        composeTestRule.onNodeWithText("Welcome to Test Sanctuary.").assertIsDisplayed()
    }

    @Test
    fun canOpenSettingsAndReturnHome() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Home").assertIsDisplayed()
    }

    @Test
    fun canNavigateToBreatheTab() {
        composeTestRule.onNodeWithContentDescription("Breathe").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("BREATHING PATTERN").assertIsDisplayed()
    }
}
