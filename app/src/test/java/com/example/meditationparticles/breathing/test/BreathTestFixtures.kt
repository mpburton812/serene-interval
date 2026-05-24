package com.example.meditationparticles.breathing.test

import com.example.meditationparticles.canvas.BreathStructureLayout
import com.example.meditationparticles.canvas.computeStructureLayout
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import kotlin.math.sqrt

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
        val from = layout.sphere(fromId) ?: return false
        val to = layout.sphere(toId) ?: return false
        val fromEdge = pipeEdge(from, to)
        val toEdge = pipeEdge(to, from)

        fun involvesSphere(
            point: androidx.compose.ui.geometry.Offset,
            sphere: com.example.meditationparticles.canvas.BreathSphere,
            toward: com.example.meditationparticles.canvas.BreathSphere,
        ): Boolean {
            return offsetsNear(point, sphere.center) || offsetsNear(point, pipeEdge(sphere, toward))
        }

        return layout.pipes.any { (a, b) ->
            involvesSphere(a, from, to) && involvesSphere(b, to, from) ||
                involvesSphere(b, from, to) && involvesSphere(a, to, from) ||
                (offsetsNear(a, fromEdge) && offsetsNear(b, toEdge)) ||
                (offsetsNear(b, fromEdge) && offsetsNear(a, toEdge))
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

    private fun pipeEdge(
        from: com.example.meditationparticles.canvas.BreathSphere,
        toward: com.example.meditationparticles.canvas.BreathSphere,
    ): androidx.compose.ui.geometry.Offset {
        val dx = toward.center.x - from.center.x
        val dy = toward.center.y - from.center.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
        return androidx.compose.ui.geometry.Offset(
            from.center.x + dx / len * from.radius,
            from.center.y + dy / len * from.radius,
        )
    }

    private fun offsetsNear(
        a: androidx.compose.ui.geometry.Offset,
        b: androidx.compose.ui.geometry.Offset,
        tolerance: Float = 3f,
    ): Boolean {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy <= tolerance * tolerance
    }
}
