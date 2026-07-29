package com.example.meditationparticles.widget

import androidx.compose.ui.graphics.Color
import com.example.meditationparticles.domain.mood.MoodScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodWidgetFlashTest {
    @Test
    fun fadeInSteps_rampsFromTransparentToFull() {
        val steps = MoodWidgetFlash.fadeInSteps()

        assertEquals(5, steps.size)
        assertEquals(0f, steps.first(), 0.001f)
        assertEquals(1f, steps.last(), 0.001f)
        assertTrue(steps.zipWithNext().all { (left, right) -> right > left })
    }

    @Test
    fun fadeOutSteps_rampsFromFullToTransparent() {
        val steps = MoodWidgetFlash.fadeOutSteps()

        assertEquals(4, steps.size)
        assertEquals(0.75f, steps.first(), 0.001f)
        assertEquals(0f, steps.last(), 0.001f)
        assertTrue(steps.zipWithNext().all { (left, right) -> right < left })
    }

    @Test
    fun blendedBackground_returnsBaseWhenBlendIsZero() {
        val base = Color.White.copy(alpha = 0.85f)

        val blended = MoodWidgetFlash.blendedBackground(
            baseBackground = base,
            moodLevel = 1,
            blend = 0f,
        )

        assertEquals(base.red, blended.red, 0.001f)
        assertEquals(base.green, blended.green, 0.001f)
        assertEquals(base.blue, blended.blue, 0.001f)
        assertEquals(base.alpha, blended.alpha, 0.001f)
    }

    @Test
    fun blendedBackground_usesSelectedMoodColorAtFullBlend() {
        val base = Color.Black.copy(alpha = 0.5f)
        val mood = Color(MoodScale.colorArgb(1))

        val blended = MoodWidgetFlash.blendedBackground(
            baseBackground = base,
            moodLevel = 1,
            blend = 1f,
        )

        assertEquals(mood.red, blended.red, 0.001f)
        assertEquals(mood.green, blended.green, 0.001f)
        assertEquals(mood.blue, blended.blue, 0.001f)
        assertEquals(1f, blended.alpha, 0.001f)
    }
}
