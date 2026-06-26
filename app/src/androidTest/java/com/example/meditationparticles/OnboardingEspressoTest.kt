package com.example.meditationparticles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.meditationparticles.testing.InstrumentedTestFixtures
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingEspressoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearOnboardingState() {
        InstrumentedTestFixtures.clearLaunchMigration(composeTestRule.activity.applicationContext)
        InstrumentedTestFixtures.clearExperienceSettings(composeTestRule.activity.applicationContext)
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
    }

    @Test
    fun freshInstallShowsOnboardingWalkthrough() {
        composeTestRule.onNodeWithText("Build your Sway").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }
}
