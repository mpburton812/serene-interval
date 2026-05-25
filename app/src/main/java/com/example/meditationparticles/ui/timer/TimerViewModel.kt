package com.example.meditationparticles.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.timer.TimerBellSoundChoice
import com.example.meditationparticles.domain.timer.TimerDisplayMode
import com.example.meditationparticles.domain.timer.TimerEngine
import com.example.meditationparticles.domain.timer.TimerPhase
import com.example.meditationparticles.domain.timer.TimerSessionState
import com.example.meditationparticles.domain.timer.TimerSoundOption
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = TimerEngine()
    private val sessionRepository = AppGraph.sessions(application)

    val sessionState: StateFlow<TimerSessionState> = engine.state

    init {
        viewModelScope.launch {
            var loggedCompletion = false
            sessionState.collect { state ->
                if (state.phase == TimerPhase.Complete && !loggedCompletion) {
                    loggedCompletion = true
                    sessionRepository.logTimer(state.targetMinutes)
                }
                if (state.phase != TimerPhase.Complete) {
                    loggedCompletion = false
                }
            }
        }
    }

    fun setDisplayMode(mode: TimerDisplayMode) = engine.setDisplayMode(mode)
    fun cycleTargetMinutes() = engine.cycleTargetMinutes()
    fun setTargetMinutes(minutes: Int) = engine.setTargetMinutes(minutes)
    fun setSound(sound: TimerSoundOption) = engine.setSound(sound)
    fun setBellSound(choice: TimerBellSoundChoice, systemUri: String? = null) =
        engine.setBellSound(choice, systemUri)
    fun setReminder(enabled: Boolean, hour: Int, minute: Int) = engine.setReminder(enabled, hour, minute)
    fun toggleRunning() = engine.toggleRunning()
    fun reset() = engine.reset()

    fun restorePreferences(
        displayMode: TimerDisplayMode,
        targetMinutes: Int,
        sound: TimerSoundOption,
        bellSound: TimerBellSoundChoice,
        bellSystemUri: String?,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
    ) {
        engine.restoreFromPreferences(
            displayMode = displayMode,
            targetMinutes = targetMinutes,
            sound = sound,
            bellSound = bellSound,
            bellSystemUri = bellSystemUri,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
        )
    }

    override fun onCleared() {
        engine.pause()
        super.onCleared()
    }
}
