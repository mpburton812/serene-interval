package com.example.meditationparticles.domain.timer

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

class TimerEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(TimerSessionState.initial())
    val state: StateFlow<TimerSessionState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var sessionStartMs: Long = 0L
    private var pausedElapsedMs: Long = 0L

    private companion object {
        const val PREPARE_MS = 2_000L
        const val TICK_MS = 16L
    }

    fun setDisplayMode(mode: TimerDisplayMode) {
        if (_state.value.isRunning) return
        _state.update { it.copy(displayMode = mode) }
    }

    fun setTargetMinutes(minutes: Int) {
        if (_state.value.isRunning) return
        _state.update { it.copy(targetMinutes = minutes.coerceIn(1, 120)) }
    }

    fun cycleTargetMinutes() {
        if (_state.value.isRunning) return
        val presets = TimerPresets.minutes
        val current = _state.value.targetMinutes
        val index = presets.indexOf(current).let { if (it >= 0) it else presets.indexOf(TimerPresets.DEFAULT_MINUTES) }
        val next = presets[(index + 1) % presets.size]
        setTargetMinutes(next)
    }

    fun setSound(sound: TimerSoundOption) {
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                sound = sound,
                customSoundUri = if (sound != TimerSoundOption.Custom) null else it.customSoundUri,
            )
        }
    }

    fun setCustomSoundUri(uri: String?) {
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                sound = TimerSoundOption.Custom,
                customSoundUri = uri,
            )
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                reminderEnabled = enabled,
                reminderHour = hour.coerceIn(0, 23),
                reminderMinute = minute.coerceIn(0, 59),
            )
        }
    }

    fun toggleRunning() {
        if (_state.value.isRunning) pause() else start()
    }

    fun start() {
        val current = _state.value
        when {
            current.phase == TimerPhase.Complete -> {
                pausedElapsedMs = 0L
                _state.update {
                    it.copy(phase = TimerPhase.Prepare, elapsedMs = 0L, isRunning = true)
                }
            }
            current.phase == TimerPhase.Running && current.elapsedMs > 0L -> {
                _state.update { it.copy(isRunning = true) }
            }
            else -> {
                _state.update { it.copy(isRunning = true, phase = TimerPhase.Prepare) }
            }
        }
        sessionStartMs = System.currentTimeMillis()
        startTickLoop()
    }

    fun pause() {
        tickJob?.cancel()
        tickJob = null
        pausedElapsedMs = when (_state.value.phase) {
            TimerPhase.Running -> _state.value.elapsedMs
            TimerPhase.Prepare -> 0L
            else -> pausedElapsedMs
        }
        _state.update {
            it.copy(
                isRunning = false,
                phase = when (it.phase) {
                    TimerPhase.Complete -> TimerPhase.Complete
                    TimerPhase.Running -> TimerPhase.Running
                    else -> TimerPhase.Idle
                },
            )
        }
    }

    fun reset() {
        pause()
        pausedElapsedMs = 0L
        _state.update {
            TimerSessionState(
                displayMode = it.displayMode,
                targetMinutes = it.targetMinutes,
                sound = it.sound,
                customSoundUri = it.customSoundUri,
                reminderEnabled = it.reminderEnabled,
                reminderHour = it.reminderHour,
                reminderMinute = it.reminderMinute,
            )
        }
    }

    fun restoreFromPreferences(
        displayMode: TimerDisplayMode,
        targetMinutes: Int,
        sound: TimerSoundOption,
        customSoundUri: String?,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
    ) {
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                displayMode = displayMode,
                targetMinutes = targetMinutes,
                sound = sound,
                customSoundUri = customSoundUri,
                reminderEnabled = reminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
            )
        }
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && _state.value.isRunning) {
                tick(System.currentTimeMillis())
                delay(TICK_MS)
            }
        }
    }

    private fun tick(now: Long) {
        val current = _state.value
        if (!current.isRunning) return

        val sessionElapsed = now - sessionStartMs + pausedElapsedMs

        if (current.phase == TimerPhase.Prepare) {
            if (sessionElapsed >= PREPARE_MS) {
                sessionStartMs = now
                pausedElapsedMs = 0L
                _state.update {
                    it.copy(phase = TimerPhase.Running, elapsedMs = 0L)
                }
            }
            return
        }

        if (current.phase != TimerPhase.Running) return

        val elapsed = sessionElapsed.coerceAtMost(current.totalMs)
        _state.update { it.copy(elapsedMs = elapsed) }

        if (elapsed >= current.totalMs) {
            _state.update {
                it.copy(
                    phase = TimerPhase.Complete,
                    elapsedMs = current.totalMs,
                    isRunning = false,
                )
            }
            tickJob?.cancel()
        }
    }
}
