package com.example.meditationparticles.domain.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathStructureSpecTest {

    @Test
    fun boxBreathing_hasFourInhaleFourExhaleAndBothHolds() {
        val spec = computeStructureSpec(BreathingPattern.BoxBreathing)

        assertEquals(4, spec.inhaleSphereCount)
        assertEquals(4, spec.exhaleSphereCount)
        assertTrue(spec.hasTopHold)
        assertTrue(spec.hasBottomHold)
        assertEquals(4f, spec.topHoldSeconds)
        assertEquals(4f, spec.bottomHoldSeconds)
    }

    @Test
    fun boxBreathing_usesFourLadderRows() {
        val spec = computeStructureSpec(BreathingPattern.BoxBreathing)
        assertEquals(4, spec.ladderRows)
    }

    @Test
    fun fourSevenEight_usesEightLadderRows() {
        val spec = computeStructureSpec(BreathingPattern.FourSevenEight)
        assertEquals(8, spec.ladderRows)
    }

    @Test
    fun fourSevenEight_hasSevenSecondTopHoldOnly() {
        val spec = computeStructureSpec(BreathingPattern.FourSevenEight)

        assertEquals(4, spec.inhaleSphereCount)
        assertEquals(8, spec.exhaleSphereCount)
        assertTrue(spec.hasTopHold)
        assertFalse(spec.hasBottomHold)
        assertEquals(7f, spec.topHoldSeconds)
        assertNull(spec.bottomHoldSeconds)
    }

    @Test
    fun resonant_hasNoHolds() {
        val spec = computeStructureSpec(BreathingPattern.Resonant)

        assertEquals(5, spec.inhaleSphereCount)
        assertEquals(5, spec.exhaleSphereCount)
        assertFalse(spec.hasTopHold)
        assertFalse(spec.hasBottomHold)
    }

    @Test
    fun resonant_usesSixLadderRows() {
        val spec = computeStructureSpec(BreathingPattern.Resonant)
        assertEquals(6, spec.ladderRows)
    }

    @Test
    fun physiologicalSigh_splitsInhaleSegments() {
        val spec = computeStructureSpec(BreathingPattern.PhysiologicalSigh)

        assertEquals(3, spec.inhaleSphereCount)
        assertEquals(6, spec.exhaleSphereCount)
        assertEquals(1f, spec.inhaleSegments[0].durationSeconds, 0.01f)
        assertEquals(0.5f, spec.inhaleSegments[1].durationSeconds, 0.01f)
        assertEquals(0.5f, spec.inhaleSegments[2].durationSeconds, 0.01f)
    }

    @Test
    fun segmentDurations_sumToPhaseLength() {
        val spec = computeStructureSpec(BreathingPattern.PhysiologicalSigh)
        val inhaleTotal = spec.inhaleSegments.sumOf { it.durationSeconds.toDouble() }.toFloat()
        assertEquals(2f, inhaleTotal, 0.01f)
    }

    @Test
    fun requiredLadderRows_assignsUniqueSlotsForBoxBreathing() {
        val inhale = inhaleGridSlots(4, requiredLadderRows(4, 4))
        val exhale = exhaleGridSlots(4, requiredLadderRows(4, 4))
        assertEquals(4, inhale.size)
        assertEquals(4, exhale.size)
        assertTrue(inhale.intersect(exhale).isEmpty())
    }
}
