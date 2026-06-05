package com.example.meditationparticles.breathing.test

import com.example.meditationparticles.canvas.BreathStructureLayout
import com.example.meditationparticles.canvas.computeModeBLayout
import com.example.meditationparticles.canvas.computeStructureLayout
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.BreathingSessionState

object BreathTestFixtures {
    const val PHONE_WIDTH = 1080f
    const val PHONE_HEIGHT = 2200f
    const val SMALL_WIDTH = 720f
    const val SMALL_HEIGHT = 1280f
    const val LARGE_WIDTH = 1344f
    const val LARGE_HEIGHT = 2992f
    const val TOP_INSET = 120f
    const val BOTTOM_INSET = 160f

    fun layoutFor(
        pattern: BreathingPattern,
        width: Float = PHONE_WIDTH,
        height: Float = PHONE_HEIGHT,
        topInset: Float = TOP_INSET,
        bottomInset: Float = BOTTOM_INSET,
    ): BreathStructureLayout = computeStructureLayout(
        pattern = pattern,
        width = width,
        height = height,
        topInset = topInset,
        bottomInset = bottomInset,
    )

    fun layoutForModeB(
        pattern: BreathingPattern,
        width: Float = PHONE_WIDTH,
        height: Float = PHONE_HEIGHT,
        topInset: Float = TOP_INSET,
        bottomInset: Float = BOTTOM_INSET,
    ): BreathStructureLayout = computeModeBLayout(
        pattern = pattern,
        width = width,
        height = height,
        topInset = topInset,
        bottomInset = bottomInset,
    )

    fun session(
        pattern: BreathingPattern = BreathingPattern.BoxBreathing,
        phase: BreathPhase = BreathPhase.Inhale,
        progress: Float = 0f,
        isRunning: Boolean = true,
        cycleCount: Int = 0,
    ): BreathingSessionState = BreathingSessionState(
        pattern = pattern,
        phase = phase,
        phaseProgress = progress,
        isRunning = isRunning,
        cycleCount = cycleCount,
    )

    fun pipesConnect(layout: BreathStructureLayout, fromId: Int, toId: Int): Boolean {
        return layout.pipes.any { pipe ->
            (pipe.fromSphereId == fromId && pipe.toSphereId == toId) ||
                (pipe.fromSphereId == toId && pipe.toSphereId == fromId)
        }
    }

    fun hasClosedBreathCircuit(layout: BreathStructureLayout): Boolean {
        if (layout.inhalePath.isEmpty() || layout.exhalePath.isEmpty()) return false
        val firstInhale = layout.inhalePath.first()
        val lastInhale = layout.inhalePath.last()
        val firstExhale = layout.exhalePath.first()
        val lastExhale = layout.exhalePath.last()
        val bottomHold = layout.bottomHoldId
        val topHold = layout.topHoldId

        val bottomClosed = if (bottomHold != null) {
            pipesConnect(layout, lastExhale, bottomHold) &&
                pipesConnect(layout, bottomHold, firstInhale)
        } else {
            pipesConnect(layout, lastExhale, firstInhale)
        }

        val topClosed = if (topHold != null) {
            pipesConnect(layout, lastInhale, topHold) &&
                pipesConnect(layout, topHold, firstExhale)
        } else {
            pipesConnect(layout, lastInhale, firstExhale)
        }

        return bottomClosed && topClosed
    }
}
