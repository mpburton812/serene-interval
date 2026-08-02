package com.safehaven.affirmations.domain.affirmations

import org.junit.Assert.assertTrue
import org.junit.Test

class AffirmationReviewBeadColorsTest {
    @Test
    fun colorForIndex_singleBead_isRedHue() {
        val color = AffirmationReviewBeadColors.colorForIndex(index = 0, total = 1)
        assertTrue(color.red >= color.blue)
    }

    @Test
    fun colorForIndex_firstAndLast_spanRedToPurple() {
        val first = AffirmationReviewBeadColors.colorForIndex(index = 0, total = 6)
        val last = AffirmationReviewBeadColors.colorForIndex(index = 5, total = 6)

        assertTrue(first.red >= first.blue)
        assertTrue(last.blue >= last.red)
        assertTrue(last.blue > first.blue)
    }

    @Test
    fun colorForIndex_progressesTowardPurple() {
        val colors = (0 until 7).map { index ->
            AffirmationReviewBeadColors.colorForIndex(index, total = 7)
        }
        for (index in 1 until colors.size) {
            assertTrue(colors[index].blue >= colors[index - 1].blue - 0.001f)
        }
    }
}
