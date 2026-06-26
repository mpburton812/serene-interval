package com.example.meditationparticles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.meditationparticles.testing.InstrumentedTestFixtures
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingEspressoTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        InstrumentedTestFixtures.prepareFreshOnboarding()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }

    @Test
    fun freshInstallShowsOnboardingWalkthrough() {
        composeTestRule.onNodeWithText("Build your Sway").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }
}
