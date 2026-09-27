package com.safehaven.affirmations.widget

import androidx.compose.ui.graphics.Color
import com.safehaven.affirmations.domain.mood.MoodScale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodWidgetSelectionFlashTest {
    @Test
    fun run_emitsThreeBounceCyclesThenClears() = runBlocking {
        val frames = mutableListOf<MoodWidgetSelectionFlash.Frame>()
        MoodWidgetSelectionFlash.run(halfCycleMs = 0L) { frame ->
            frames += frame
        }

        val expected = MoodWidgetSelectionFlash.bounceFrames
        assertEquals(MoodWidgetSelectionFlash.CYCLES, 3)
        assertEquals(expected, frames)
        assertTrue(expected.any { it.faceEnlarged })
        assertTrue(expected.any { !it.faceEnlarged })
        assertEquals(0f, expected.last().backgroundAlpha, 0.001f)
    }

    @Test
    fun resolveWidgetBackground_usesFlashColorAtFullStrength() {
        val color = resolveWidgetBackground(
            baseArgb = 0xFFFFFFFF,
            transparency = 0.85f,
            flash = MoodWidgetFlash(colorArgb = MoodScale.COLOR_RED, alpha = 1f),
        )
        val expected = Color(MoodScale.COLOR_RED).copy(alpha = 0.85f)
        assertEquals(expected.red, color.red, 0.001f)
        assertEquals(expected.green, color.green, 0.001f)
        assertEquals(expected.blue, color.blue, 0.001f)
        assertEquals(expected.alpha, color.alpha, 0.001f)
    }

    @Test
    fun resolveWidgetBackground_usesConfiguredChromeWhenNoFlash() {
        val color = resolveWidgetBackground(
            baseArgb = 0xFF000000,
            transparency = 0.5f,
            flash = null,
        )
        assertEquals(0f, color.red, 0.02f)
        assertEquals(0f, color.green, 0.02f)
        assertEquals(0f, color.blue, 0.02f)
        // Compose stores alpha in 8-bit channels (128/255 ≈ 0.502).
        assertEquals(0.5f, color.alpha, 0.02f)
    }

    @Test
    fun flashStore_keepsZeroAlphaUntilExplicitlyCleared() {
        MoodWidgetFlashStore.set(99, MoodWidgetFlash(MoodScale.COLOR_GREEN, 0.8f, level = 4))
        assertEquals(0.8f, MoodWidgetFlashStore.get(99)?.alpha)
        MoodWidgetFlashStore.set(99, MoodWidgetFlash(MoodScale.COLOR_GREEN, 0f, level = 4))
        assertEquals(0f, MoodWidgetFlashStore.get(99)?.alpha)
        MoodWidgetFlashStore.clear(99)
        assertNull(MoodWidgetFlashStore.get(99))
    }

    @Test
    fun isFaceEnlarged_usesExplicitBounceFlagForMatchingLevel() {
        val enlarged = MoodWidgetFlash(
            MoodScale.COLOR_BLUE,
            alpha = 0.2f,
            level = 3,
            faceEnlarged = true,
        )
        val resting = MoodWidgetFlash(
            MoodScale.COLOR_BLUE,
            alpha = 0.7f,
            level = 3,
            faceEnlarged = false,
        )

        assertTrue(MoodWidgetSelectionFlash.isFaceEnlarged(enlarged, level = 3))
        assertTrue(!MoodWidgetSelectionFlash.isFaceEnlarged(resting, level = 3))
        assertTrue(!MoodWidgetSelectionFlash.isFaceEnlarged(enlarged, level = 1))
        assertEquals(
            MoodWidgetSelectionFlash.HIGHLIGHT_CIRCLE_DP,
            MoodWidgetSelectionFlash.circleSizeDp(enlarged = true),
        )
        assertEquals(
            MoodWidgetSelectionFlash.NORMAL_ICON_DP,
            MoodWidgetSelectionFlash.iconSizeDp(enlarged = false),
        )
    }
}
