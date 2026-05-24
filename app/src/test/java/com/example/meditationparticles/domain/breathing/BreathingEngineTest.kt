package com.example.meditationparticles.domain.breathing

import com.example.meditationparticles.breathing.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BreathingEngineTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun engineWithClock(startMs: Long = 1_000L): Pair<BreathingEngine, Long> {
        var now = startMs
        return BreathingEngine { now } to now
    }

    @Test
    fun start_setsRunningTrue() {
        val (engine, _) = engineWithClock()
        engine.start()
        assertTrue(engine.state.value.isRunning)
        engine.pause()
    }

    @Test
    fun pause_setsRunningFalse() {
        val (engine, _) = engineWithClock()
        engine.start()
        engine.pause()
        assertFalse(engine.state.value.isRunning)
    }

    @Test
    fun selectPattern_blockedWhileRunning() {
        val (engine, _) = engineWithClock()
        engine.start()
        engine.selectPattern(BreathingPattern.FourSevenEight)
        assertEquals(BreathingPattern.BoxBreathing.id, engine.state.value.pattern.id)
        engine.pause()
    }

    @Test
    fun selectPattern_appliesWhenIdle() {
        val (engine, _) = engineWithClock()
        engine.selectPattern(BreathingPattern.Resonant)
        assertEquals(BreathingPattern.Resonant.id, engine.state.value.pattern.id)
        assertEquals(BreathPhase.Prepare, engine.state.value.phase)
    }

    @Test
    fun reset_returnsToPrepare() {
        val (engine, startMs) = engineWithClock()
        engine.start()
        engine.runTickAt(startMs + 3_000)
        engine.reset()
        val state = engine.state.value
        assertFalse(state.isRunning)
        assertEquals(BreathPhase.Prepare, state.phase)
        assertEquals(0, state.cycleCount)
    }

    @Test
    fun tick_advancesFromPrepareToInhale() {
        val (engine, startMs) = engineWithClock()
        engine.start()
        engine.runTickAt(startMs + 2_100)
        assertEquals(BreathPhase.Inhale, engine.state.value.phase)
        engine.pause()
    }

    @Test
    fun repetitionMode_completesAfterTargetCycles() {
        val (engine, startMs) = engineWithClock()
        engine.setSessionMode(SessionMode.Repetitions)
        engine.setTargetRepetitions(1)
        engine.selectPattern(BreathingPattern.SamaVritti)
        engine.start()

        engine.runTickAt(startMs + 2_100)
        engine.runTickAt(startMs + 6_100)
        engine.runTickAt(startMs + 10_100)

        val state = engine.state.value
        assertEquals(BreathPhase.Complete, state.phase)
        assertFalse(state.isRunning)
        assertTrue(state.cycleCount >= 1)
    }
}
