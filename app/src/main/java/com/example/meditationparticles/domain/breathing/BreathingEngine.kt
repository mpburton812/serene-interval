package com.example.meditationparticles.domain.breathing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BreathingEngine(
    private val currentTimeMs: () -> Long = System::currentTimeMillis,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(BreathingSessionState.initial())
    val state: StateFlow<BreathingSessionState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var phaseStartMs: Long = 0L
    private var sessionStartMs: Long = 0L

    private companion object {
        const val PREPARE_SECONDS = 2f
        const val TICK_MS = 16L
    }

    fun selectPattern(pattern: BreathingPattern) {
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                pattern = pattern,
                phase = BreathPhase.Prepare,
                phaseProgress = 0f,
                cycleCount = 0,
                elapsedSessionMs = 0L,
            )
        }
    }

    fun setSessionMode(mode: SessionMode) {
        if (_state.value.isRunning) return
        _state.update { it.copy(sessionMode = mode) }
    }

    fun setTargetMinutes(minutes: Int) {
        if (_state.value.isRunning) return
        _state.update { it.copy(targetMinutes = minutes.coerceIn(1, 60)) }
    }

    fun setTargetRepetitions(reps: Int) {
        if (_state.value.isRunning) return
        _state.update { it.copy(targetRepetitions = reps.coerceIn(1, 99)) }
    }

    fun toggleRunning() {
        if (_state.value.isRunning) pause() else start()
    }

    fun start() {
        val now = currentTimeMs()
        if (_state.value.phase == BreathPhase.Complete) {
            _state.update {
                it.copy(
                    phase = BreathPhase.Prepare,
                    phaseProgress = 0f,
                    cycleCount = 0,
                    elapsedSessionMs = 0L,
                )
            }
        }
        sessionStartMs = now
        phaseStartMs = now
        _state.update { it.copy(isRunning = true) }
        startTickLoop()
    }

    fun pause() {
        tickJob?.cancel()
        tickJob = null
        _state.update { it.copy(isRunning = false) }
    }

    fun reset() {
        pause()
        _state.update {
            BreathingSessionState(
                pattern = it.pattern,
                sessionMode = it.sessionMode,
                targetMinutes = it.targetMinutes,
                targetRepetitions = it.targetRepetitions,
            )
        }
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && _state.value.isRunning) {
                tick(currentTimeMs())
                delay(TICK_MS)
            }
        }
    }

    /** Test hook: advance engine state as if a tick occurred at [nowMs]. */
    internal fun runTickAt(nowMs: Long) {
        tick(nowMs)
    }

    private fun tick(now: Long) {
        val current = _state.value
        if (!current.isRunning) return

        val elapsedSession = now - sessionStartMs
        val phaseDurationMs = phaseDurationMs(current.phase, current.pattern)
        val phaseElapsed = now - phaseStartMs
        val progress = if (phaseDurationMs <= 0L) {
            1f
        } else {
            (phaseElapsed.toFloat() / phaseDurationMs).coerceIn(0f, 1f)
        }

        _state.update {
            it.copy(
                phaseProgress = progress,
                elapsedSessionMs = elapsedSession,
            )
        }

        if (current.sessionMode == SessionMode.Duration &&
            elapsedSession >= current.targetMinutes * 60_000L
        ) {
            _state.update {
                it.copy(
                    phase = BreathPhase.Complete,
                    phaseProgress = 1f,
                    isRunning = false,
                )
            }
            tickJob?.cancel()
            return
        }

        if (phaseDurationMs > 0L && phaseElapsed >= phaseDurationMs) {
            advancePhase(now)
        } else if (phaseDurationMs == 0L) {
            advancePhase(now)
        }
    }

    private fun advancePhase(now: Long) {
        val current = _state.value
        val pattern = current.pattern
        var nextPhase = current.phase
        var nextCycle = current.cycleCount

        when (current.phase) {
            BreathPhase.Prepare -> nextPhase = BreathPhase.Inhale
            BreathPhase.Inhale -> {
                nextPhase = when {
                    pattern.secondInhaleSeconds > 0f -> BreathPhase.SecondInhale
                    pattern.holdAfterInhaleSeconds > 0f -> BreathPhase.HoldIn
                    else -> BreathPhase.Exhale
                }
            }
            BreathPhase.SecondInhale -> nextPhase = BreathPhase.Exhale
            BreathPhase.HoldIn -> nextPhase = BreathPhase.Exhale
            BreathPhase.Exhale -> {
                nextPhase = if (pattern.holdAfterExhaleSeconds > 0f) {
                    BreathPhase.HoldOut
                } else {
                    BreathPhase.Inhale
                }
                if (nextPhase == BreathPhase.Inhale) {
                    nextCycle = current.cycleCount + 1
                }
            }
            BreathPhase.HoldOut -> {
                nextCycle = current.cycleCount + 1
                nextPhase = BreathPhase.Inhale
            }
            BreathPhase.Complete -> return
        }

        if (nextPhase == BreathPhase.Inhale && nextCycle > 0 && isSessionComplete(nextCycle, now)) {
            _state.update {
                it.copy(
                    phase = BreathPhase.Complete,
                    phaseProgress = 1f,
                    cycleCount = nextCycle,
                    isRunning = false,
                )
            }
            tickJob?.cancel()
            return
        }

        phaseStartMs = now
        _state.update {
            it.copy(
                phase = nextPhase,
                phaseProgress = 0f,
                cycleCount = nextCycle,
            )
        }
    }

    private fun isSessionComplete(cycleCount: Int, now: Long): Boolean {
        val current = _state.value
        return when (current.sessionMode) {
            SessionMode.Repetitions -> cycleCount >= current.targetRepetitions
            SessionMode.Duration -> {
                (now - sessionStartMs) >= current.targetMinutes * 60_000L
            }
        }
    }

    private fun phaseDurationMs(phase: BreathPhase, pattern: BreathingPattern): Long =
        (phaseDurationSeconds(phase, pattern) * 1000f).toLong()

    private fun phaseDurationSeconds(phase: BreathPhase, pattern: BreathingPattern): Float =
        when (phase) {
            BreathPhase.Prepare -> PREPARE_SECONDS
            BreathPhase.Inhale -> pattern.inhaleSeconds
            BreathPhase.SecondInhale -> pattern.secondInhaleSeconds
            BreathPhase.HoldIn -> pattern.holdAfterInhaleSeconds
            BreathPhase.Exhale -> pattern.exhaleSeconds
            BreathPhase.HoldOut -> pattern.holdAfterExhaleSeconds
            BreathPhase.Complete -> 0f
        }
}
