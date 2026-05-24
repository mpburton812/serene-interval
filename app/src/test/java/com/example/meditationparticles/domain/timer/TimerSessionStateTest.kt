package com.example.meditationparticles.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSessionStateTest {
    @Test
    fun idle_showsOnlySetupDescription() {
        val state = TimerSessionState(phase = TimerPhase.Idle)
        assertEquals("", state.statusLabel)
        assertTrue(state.statusDescription.contains("Choose a duration"))
    }

    @Test
    fun running_hidesStatusTexts() {
        val state = TimerSessionState(phase = TimerPhase.Running, isRunning = true)
        assertEquals("", state.statusLabel)
        assertEquals("", state.statusDescription)
    }

    @Test
    fun prepareCountdownSeconds_countsDownFromTen() {
        val state = TimerSessionState(phase = TimerPhase.Prepare, prepareElapsedMs = 0L)
        assertEquals(10, state.prepareCountdownSeconds)
        assertEquals(5, state.copy(prepareElapsedMs = 5_500L).prepareCountdownSeconds)
    }

    @Test
    fun isPrepareBeginVisible_onlyAfterCountdown() {
        val duringCountdown = TimerSessionState(phase = TimerPhase.Prepare, prepareElapsedMs = 5_000L)
        assertFalse(duringCountdown.isPrepareBeginVisible)
        val duringBegin = TimerSessionState(
            phase = TimerPhase.Prepare,
            prepareElapsedMs = TimerPrepareTiming.COUNTDOWN_MS + 500L,
        )
        assertTrue(duringBegin.isPrepareBeginVisible)
    }
}
