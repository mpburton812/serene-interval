package com.example.meditationparticles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditationparticles.testing.InstrumentedTestFixtures
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full-app instrumented tests using Espresso's AndroidJUnit runner plus Compose UI Test
 * (createAndroidComposeRule). Compose tests integrate with Espresso idling automatically.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        // ActivityScenarioRule starts the activity before @Before, so re-apply prefs each test.
        InstrumentedTestFixtures.prepareMainActivityTest(targetContext())
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        dismissUpdateDialogIfShown()
        waitForHomeScreen()
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

    private fun dismissUpdateDialogIfShown() {
        val laterNodes = composeTestRule.onAllNodesWithText("Later").fetchSemanticsNodes()
        if (laterNodes.isNotEmpty()) {
            composeTestRule.onNodeWithText("Later").performClick()
            composeTestRule.waitForIdle()
        }
    }

    private fun waitForHomeScreen() {
        composeTestRule.waitUntil(timeoutMillis = 30_000) {
            composeTestRule.onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun seedCompletedOnboardingBeforeActivity() {
            // Runs before ActivityScenarioRule launches MainActivity for this class.
            InstrumentedTestFixtures.prepareMainActivityTest(targetContext())
        }

        private fun targetContext() =
            InstrumentationRegistry.getInstrumentation().targetContext
    }
}
