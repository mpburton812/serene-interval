package com.example.meditationparticles.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.domain.timer.TimerBellSoundChoice
import com.example.meditationparticles.domain.timer.TimerDisplayMode
import com.example.meditationparticles.domain.timer.TimerEngine
import com.example.meditationparticles.domain.timer.TimerPhase
import com.example.meditationparticles.domain.timer.TimerSessionState
import com.example.meditationparticles.domain.timer.TimerSoundOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = TimerEngine()
    private val sessionRepository = AppGraph.sessions(application)
    private val reflectionRepository = AppGraph.meditationReflections(application)
    private val oneNoteSync = AppGraph.oneNoteSync(application)

    val sessionState: StateFlow<TimerSessionState> = engine.state
    val reflectionText = MutableStateFlow("")
    val reflectionMoodLevel = MutableStateFlow<Int?>(null)
    val reflectionSaved = MutableStateFlow(false)
    private val appContext = application.applicationContext

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
                    reflectionText.value = ""
                    reflectionMoodLevel.value = null
                    reflectionSaved.value = false
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

    fun updateReflection(text: String) {
        if (reflectionSaved.value) return
        reflectionText.value = text
    }

    fun updateReflectionMoodLevel(level: Int?) {
        if (reflectionSaved.value) return
        reflectionMoodLevel.value = level?.coerceIn(1, 5)
    }

    fun saveReflection() {
        if (reflectionSaved.value) return
        viewModelScope.launch {
            val state = sessionState.value
            if (state.phase != TimerPhase.Complete) return@launch
            val savedId = reflectionRepository.save(
                reflection = reflectionText.value,
                durationSeconds = state.targetMinutes * 60,
                completedAt = System.currentTimeMillis(),
                moodLevel = reflectionMoodLevel.value,
            ) ?: return@launch
            oneNoteSync.enqueueSync(OneNoteEntryType.MEDITATION_REFLECTION, savedId)
            reflectionSaved.value = true
        }
    }

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
