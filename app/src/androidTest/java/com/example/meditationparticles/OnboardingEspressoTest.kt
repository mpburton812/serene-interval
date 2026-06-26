package com.example.meditationparticles

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.meditationparticles.testing.InstrumentedTestFixtures
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingEspressoTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshInstallShowsOnboardingWalkthrough() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Build your Sway").assertIsDisplayed()
        composeTestRule.onNodeWithText("Next").assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun clearOnboardingStateBeforeActivity() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            InstrumentedTestFixtures.prepareFreshOnboarding(context)
        }
    }
}
