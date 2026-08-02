package com.safehaven.affirmations.domain.affirmations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AffirmationReviewLogicTest {
    @Test
    fun canStartReview_requiresAtLeastOneAffirmation() {
        assertFalse(AffirmationReviewLogic.canStartReview(0))
        assertTrue(AffirmationReviewLogic.canStartReview(1))
    }

    @Test
    fun nextIndex_advancesUntilLast() {
        assertEquals(1, AffirmationReviewLogic.nextIndex(currentIndex = 0, lastIndex = 2))
        assertEquals(2, AffirmationReviewLogic.nextIndex(currentIndex = 1, lastIndex = 2))
        assertNull(AffirmationReviewLogic.nextIndex(currentIndex = 2, lastIndex = 2))
    }

    @Test
    fun previousIndex_movesBackWhilePossible() {
        assertNull(AffirmationReviewLogic.previousIndex(0))
        assertEquals(0, AffirmationReviewLogic.previousIndex(1))
    }

    @Test
    fun shouldCompleteReview_onlyOnLastAffirmation() {
        assertFalse(AffirmationReviewLogic.shouldCompleteReview(0, 2))
        assertFalse(AffirmationReviewLogic.shouldCompleteReview(1, 2))
        assertTrue(AffirmationReviewLogic.shouldCompleteReview(2, 2))
        assertTrue(AffirmationReviewLogic.shouldCompleteReview(0, 0))
    }

    @Test
    fun forwardIndexSequence_advancesOneIndexPerStep() {
        assertEquals(listOf(0, 1, 2, 3, 4), AffirmationReviewLogic.forwardIndexSequence(lastIndex = 4))
        assertEquals(listOf(0), AffirmationReviewLogic.forwardIndexSequence(lastIndex = 0))
        assertEquals(emptyList<Int>(), AffirmationReviewLogic.forwardIndexSequence(lastIndex = -1))
    }

    @Test
    fun navigation_textChangesOnEveryIndexForDistinctAffirmations() {
        val texts = listOf("Alpha", "Beta", "Gamma", "Delta")
        val lastIndex = texts.lastIndex
        val seenTexts = AffirmationReviewLogic.forwardIndexSequence(lastIndex).map { index ->
            AffirmationReviewLogic.textAt(texts, index)
        }

        assertEquals(texts, seenTexts)
        assertEquals(texts.size, seenTexts.distinct().size)
    }

    @Test
    fun canSaveAssessment_requiresMoodOrNotes() {
        assertFalse(AffirmationReviewLogic.canSaveAssessment(moodLevel = null, notes = ""))
        assertFalse(AffirmationReviewLogic.canSaveAssessment(moodLevel = null, notes = "   "))
        assertTrue(AffirmationReviewLogic.canSaveAssessment(moodLevel = 3, notes = ""))
        assertTrue(AffirmationReviewLogic.canSaveAssessment(moodLevel = null, notes = "Grounded"))
        assertTrue(AffirmationReviewLogic.canSaveAssessment(moodLevel = 4, notes = "Calm"))
    }
}
