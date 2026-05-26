package com.example.meditationparticles.canvas

import com.example.meditationparticles.breathing.test.BreathTestFixtures
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.FillDirection
import com.example.meditationparticles.domain.breathing.computeStructureSpec
import com.example.meditationparticles.canvas.PREVIEW_ZONE_FILL_RATIO
import com.example.meditationparticles.canvas.computeStructureLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathSphereFillTest {

    @Test
    fun boxInhale_atZeroProgress_allInhaleSpheresEmpty() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.Inhale, progress = 0f),
            layout,
        )

        layout.inhalePath.forEach { id ->
            assertEquals(0f, visuals[id]?.fillLevel ?: -1f, 0.001f)
            assertFalse(visuals[id]?.isActive ?: true)
        }
    }

    @Test
    fun boxInhale_atHalfSecond_firstSphereHalfFilledBottomToTop() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.Inhale, progress = 0.125f),
            layout,
        )

        val first = layout.inhalePath.first()
        assertEquals(0.5f, visuals[first]?.fillLevel ?: -1f, 0.02f)
        assertTrue(visuals[first]?.isActive ?: false)
        assertEquals(FillDirection.BottomToTop, visuals[first]?.fillDirection)
    }

    @Test
    fun boxInhale_afterOneSecond_firstSphereDrainsWhileSecondFills() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.Inhale, progress = 0.26f),
            layout,
        )

        val first = layout.inhalePath[0]
        val second = layout.inhalePath[1]
        assertEquals(0.96f, visuals[first]?.fillLevel ?: -1f, 0.05f)
        assertTrue(visuals[first]?.isActive ?: false)
        assertEquals(0.04f, visuals[second]?.fillLevel ?: -1f, 0.05f)
        assertTrue(visuals[second]?.isActive ?: false)
    }

    @Test
    fun boxInhale_atFourSeconds_onlyLastInhaleSphereFull() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.Inhale, progress = 1f),
            layout,
        )

        layout.inhalePath.dropLast(1).forEach { id ->
            assertEquals(0f, visuals[id]?.fillLevel ?: -1f, 0.001f)
        }
        assertEquals(1f, visuals[layout.inhalePath.last()]?.fillLevel ?: -1f, 0.001f)
    }

    @Test
    fun boxHoldIn_lastInhaleDrainsIntoHold() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val holdId = requireNotNull(layout.topHoldId)
        val lastInhale = layout.inhalePath.last()

        val halfway = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.HoldIn, progress = 0.5f),
            layout,
        )
        assertEquals(0.5f, halfway[holdId]?.fillLevel ?: -1f, 0.001f)
        assertEquals(0.5f, halfway[lastInhale]?.fillLevel ?: -1f, 0.001f)
        assertTrue(halfway[holdId]?.isActive ?: false)
        assertTrue(halfway[lastInhale]?.isActive ?: false)

        val complete = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.HoldIn, progress = 1f),
            layout,
        )
        assertEquals(1f, complete[holdId]?.fillLevel ?: -1f, 0.001f)
        assertEquals(0f, complete[lastInhale]?.fillLevel ?: -1f, 0.001f)
    }

    @Test
    fun fourSevenEight_holdIn_atOneSecond_isNotFullyFilled() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.FourSevenEight)
        val holdId = requireNotNull(layout.topHoldId)
        val elapsedProgress = 1f / 7f

        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(
                pattern = BreathingPattern.FourSevenEight,
                phase = BreathPhase.HoldIn,
                progress = elapsedProgress,
            ),
            layout,
        )

        val fill = visuals[holdId]?.fillLevel ?: -1f
        assertTrue(fill in 0.13f..0.16f)
        assertTrue(visuals[holdId]?.isActive ?: false)
    }

    @Test
    fun boxExhale_atHalfSecond_holdDrainsIntoFirstExhale() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.Exhale, progress = 0.125f),
            layout,
        )

        val holdId = requireNotNull(layout.topHoldId)
        val first = layout.exhalePath.first()
        assertEquals(0.5f, visuals[first]?.fillLevel ?: -1f, 0.02f)
        assertEquals(0.5f, visuals[holdId]?.fillLevel ?: -1f, 0.02f)
        assertEquals(FillDirection.TopToBottom, visuals[first]?.fillDirection)

        layout.inhalePath.forEach { id ->
            assertEquals(0f, visuals[id]?.fillLevel ?: -1f, 0.001f)
        }
    }

    @Test
    fun boxExhale_atFourSeconds_onlyLastExhaleSphereFull() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.BoxBreathing)
        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.Exhale, progress = 1f),
            layout,
        )

        layout.exhalePath.dropLast(1).forEach { id ->
            assertEquals(0f, visuals[id]?.fillLevel ?: -1f, 0.001f)
        }
        assertEquals(1f, visuals[layout.exhalePath.last()]?.fillLevel ?: -1f, 0.001f)
    }

    @Test
    fun resonantExhale_atStart_lastInhaleDrainsIntoFirstExhale() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.Resonant)
        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(
                pattern = BreathingPattern.Resonant,
                phase = BreathPhase.Exhale,
                progress = 0.1f,
            ),
            layout,
        )

        val lastInhale = layout.inhalePath.last()
        val firstExhale = layout.exhalePath.first()
        assertEquals(0.5f, visuals[firstExhale]?.fillLevel ?: -1f, 0.02f)
        assertEquals(0.5f, visuals[lastInhale]?.fillLevel ?: -1f, 0.02f)
    }

    @Test
    fun physiologicalSecondInhale_transfersFromSecondToThirdSphere() {
        val pattern = BreathingPattern.PhysiologicalSigh
        val layout = BreathTestFixtures.layoutFor(pattern)
        val firstCount = 2

        val visuals = computeSphereVisuals(
            BreathTestFixtures.session(pattern = pattern, phase = BreathPhase.SecondInhale, progress = 0.5f),
            layout,
        )

        assertEquals(0f, visuals[layout.inhalePath[0]]?.fillLevel ?: -1f, 0.001f)
        assertEquals(0.5f, visuals[layout.inhalePath[1]]?.fillLevel ?: -1f, 0.05f)
        assertEquals(0.5f, visuals[layout.inhalePath[2]]?.fillLevel ?: -1f, 0.05f)
    }

    @Test
    fun previewLayout_fillsMostOfPreviewZone() {
        val zoneHeight = BreathTestFixtures.PHONE_HEIGHT -
            BreathTestFixtures.TOP_INSET - BreathTestFixtures.BOTTOM_INSET - 20f
        val previewExtra = zoneHeight * 0.14f
        val previewZoneHeight = zoneHeight - previewExtra * 2f

        val layout = computeStructureLayout(
            pattern = BreathingPattern.FourSevenEight,
            width = BreathTestFixtures.PHONE_WIDTH,
            height = BreathTestFixtures.PHONE_HEIGHT,
            topInset = BreathTestFixtures.TOP_INSET + previewExtra,
            bottomInset = BreathTestFixtures.BOTTOM_INSET + previewExtra,
            zoneFillRatio = PREVIEW_ZONE_FILL_RATIO,
        )

        val top = layout.allSpheres.minOf { it.center.y - it.radius }
        val bottom = layout.allSpheres.maxOf { it.center.y + it.radius }
        val spanRatio = (bottom - top) / previewZoneHeight
        assertTrue("Preview should fill preview zone: $spanRatio", spanRatio >= 0.88f)
    }

    @Test
    fun previewVisuals_fillAllSpheresWithRoleColors() {
        val layout = BreathTestFixtures.layoutFor(BreathingPattern.Resonant)
        val visuals = computePreviewSphereVisuals(layout)

        layout.allSpheres.forEach { sphere ->
            assertEquals(1f, visuals[sphere.id]?.fillLevel ?: -1f, 0.001f)
        }
        assertEquals(layout.inhalePath.first(), breathingStartSphereId(layout))
    }

    @Test
    fun allPatterns_inhaleFillAdvancesOneSpherePerSecond() {
        BreathingPattern.All.forEach { pattern ->
            val layout = BreathTestFixtures.layoutFor(pattern)
            val spec = computeStructureSpec(pattern)
            val firstInhaleCount = kotlin.math.ceil(pattern.inhaleSeconds.toDouble()).toInt()
            val segments = spec.inhaleSegments.take(firstInhaleCount)
            if (segments.isEmpty()) return@forEach

            segments.forEachIndexed { index, segment ->
                val elapsed = segments.take(index).sumOf { it.durationSeconds.toDouble() }.toFloat() +
                    segment.durationSeconds * 0.5f
                val progress = (elapsed / pattern.inhaleSeconds.coerceAtLeast(0.01f)).coerceIn(0f, 1f)
                val visuals = computeSphereVisuals(
                    BreathTestFixtures.session(
                        pattern = pattern,
                        phase = BreathPhase.Inhale,
                        progress = progress,
                    ),
                    layout,
                )

                val activeId = layout.inhalePath[index]
                assertNotNull("Missing active inhale sphere for ${pattern.name}", visuals[activeId])
                assertTrue(
                    "${pattern.name} inhale sphere $index should be active",
                    visuals[activeId]?.isActive == true,
                )
            }
        }
    }

    @Test
    fun modeB_resonantInhale_fillsOverFullPhaseDuration() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.Resonant)
        val inhaleId = layout.inhalePath.first()

        val empty = computeModeBSphereVisuals(
            BreathTestFixtures.session(pattern = BreathingPattern.Resonant, phase = BreathPhase.Inhale, progress = 0f),
            layout,
        )
        assertEquals(0f, empty[inhaleId]?.fillLevel ?: -1f, 0.001f)

        val halfway = computeModeBSphereVisuals(
            BreathTestFixtures.session(pattern = BreathingPattern.Resonant, phase = BreathPhase.Inhale, progress = 0.5f),
            layout,
        )
        assertEquals(0.5f, halfway[inhaleId]?.fillLevel ?: -1f, 0.02f)
        assertTrue(halfway[inhaleId]?.isActive ?: false)

        val full = computeModeBSphereVisuals(
            BreathTestFixtures.session(pattern = BreathingPattern.Resonant, phase = BreathPhase.Inhale, progress = 1f),
            layout,
        )
        assertEquals(1f, full[inhaleId]?.fillLevel ?: -1f, 0.001f)
    }

    @Test
    fun modeB_boxHoldIn_transfersInhaleIntoTopHold() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.BoxBreathing)
        val inhaleId = layout.inhalePath.first()
        val holdId = requireNotNull(layout.topHoldId)

        val halfway = computeModeBSphereVisuals(
            BreathTestFixtures.session(phase = BreathPhase.HoldIn, progress = 0.5f),
            layout,
        )
        assertEquals(0.5f, halfway[holdId]?.fillLevel ?: -1f, 0.02f)
        assertEquals(0.5f, halfway[inhaleId]?.fillLevel ?: -1f, 0.02f)
    }

    @Test
    fun modeB_physiologicalSigh_blueFillsAcrossBothInhales() {
        val pattern = BreathingPattern.PhysiologicalSigh
        val layout = BreathTestFixtures.layoutForModeB(pattern)
        val inhaleId = layout.inhalePath.first()
        val total = pattern.inhaleSeconds + pattern.secondInhaleSeconds

        val firstInhaleDone = computeModeBSphereVisuals(
            BreathTestFixtures.session(pattern = pattern, phase = BreathPhase.Inhale, progress = 1f),
            layout,
        )
        assertEquals(
            pattern.inhaleSeconds / total,
            firstInhaleDone[inhaleId]?.fillLevel ?: -1f,
            0.02f,
        )

        val secondHalf = computeModeBSphereVisuals(
            BreathTestFixtures.session(pattern = pattern, phase = BreathPhase.SecondInhale, progress = 0.5f),
            layout,
        )
        val expected = (pattern.inhaleSeconds + pattern.secondInhaleSeconds * 0.5f) / total
        assertEquals(expected, secondHalf[inhaleId]?.fillLevel ?: -1f, 0.03f)
    }

    @Test
    fun modeB_prepareAndComplete_allSpheresEmpty() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.BoxBreathing)

        listOf(BreathPhase.Prepare, BreathPhase.Complete).forEach { phase ->
            val visuals = computeModeBSphereVisuals(
                BreathTestFixtures.session(phase = phase, progress = 0f, isRunning = false),
                layout,
            )
            layout.allSpheres.forEach { sphere ->
                assertEquals(0f, visuals[sphere.id]?.fillLevel ?: -1f, 0.001f)
            }
        }
    }

    @Test
    fun modeB_fourSevenEight_holdInUsesFullPhaseProgress() {
        val layout = BreathTestFixtures.layoutForModeB(BreathingPattern.FourSevenEight)
        val holdId = requireNotNull(layout.topHoldId)

        val visuals = computeModeBSphereVisuals(
            BreathTestFixtures.session(
                pattern = BreathingPattern.FourSevenEight,
                phase = BreathPhase.HoldIn,
                progress = 0.5f,
            ),
            layout,
        )
        assertEquals(0.5f, visuals[holdId]?.fillLevel ?: -1f, 0.02f)
    }
}
