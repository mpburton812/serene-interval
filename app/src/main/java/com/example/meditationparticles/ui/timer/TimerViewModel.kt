package com.example.meditationparticles.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.domain.timer.TimerBellSoundChoice
import com.example.meditationparticles.domain.timer.TimerDisplayMode
import com.example.meditationparticles.domain.timer.TimerEngine
import com.example.meditationparticles.domain.timer.TimerPhase
import com.example.meditationparticles.domain.timer.TimerSessionState
import com.example.meditationparticles.domain.timer.TimerSoundOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = TimerEngine()
    private val sessionRepository = AppGraph.sessions(application)
    private val reflectionRepository = AppGraph.meditationReflections(application)
    private val oneNoteSync = AppGraph.oneNoteSync(application)

    val sessionState: StateFlow<TimerSessionState> = engine.state
    val reflectionText = MutableStateFlow("")
    val reflectionMoodLevel = MutableStateFlow<Int?>(null)
    val pendingAudioPath = MutableStateFlow<String?>(null)
    val showReflectionCapture = MutableStateFlow(false)
    val openedReflection = MutableStateFlow<MeditationReflectionEntity?>(null)

    val reflections: StateFlow<List<MeditationReflectionEntity>> =
        reflectionRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val oneNoteConnected: Boolean = oneNoteSync.isConnected()

    init {
        viewModelScope.launch {
            var loggedCompletion = false
            sessionState.collect { state ->
                if (state.phase == TimerPhase.Complete && !loggedCompletion) {
                    loggedCompletion = true
                    showReflectionCapture.value = true
                    sessionRepository.logTimer(state.targetMinutes)
                }
                if (state.phase != TimerPhase.Complete) {
                    loggedCompletion = false
                    if (state.phase == TimerPhase.Idle) {
                        clearReflectionDraft()
                    }
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
        reflectionText.value = text
    }

    fun updateReflectionMoodLevel(level: Int?) {
        reflectionMoodLevel.value = level?.coerceIn(1, 5)
    }

    fun updatePendingAudio(path: String?) {
        pendingAudioPath.value = path
    }

    fun saveReflection() {
        viewModelScope.launch {
            if (!showReflectionCapture.value) return@launch
            val state = sessionState.value
            if (state.phase != TimerPhase.Complete) return@launch
            val savedId = reflectionRepository.save(
                reflection = reflectionText.value,
                durationSeconds = state.targetMinutes * 60,
                completedAt = System.currentTimeMillis(),
                moodLevel = reflectionMoodLevel.value,
                audioPath = pendingAudioPath.value,
            ) ?: return@launch
            oneNoteSync.enqueueSync(OneNoteEntryType.MEDITATION_REFLECTION, savedId)
            finishReflectionCapture()
        }
    }

    fun skipReflection() {
        if (!showReflectionCapture.value) return
        finishReflectionCapture()
    }

    fun openReflection(entry: MeditationReflectionEntity) {
        openedReflection.value = entry
    }

    fun closeReflection() {
        openedReflection.value = null
    }

    fun deleteReflection(entry: MeditationReflectionEntity) {
        viewModelScope.launch {
            oneNoteSync.deleteForEntry(OneNoteEntryType.MEDITATION_REFLECTION, entry.id)
            reflectionRepository.delete(entry.id)
            if (openedReflection.value?.id == entry.id) {
                openedReflection.value = null
            }
        }
    }

    fun syncReflectionToOneNote(entry: MeditationReflectionEntity) {
        viewModelScope.launch {
            oneNoteSync.enqueueSync(OneNoteEntryType.MEDITATION_REFLECTION, entry.id, manual = true)
        }
    }

    private fun finishReflectionCapture() {
        clearReflectionDraft()
        showReflectionCapture.value = false
        engine.reset()
    }

    private fun clearReflectionDraft() {
        reflectionText.value = ""
        reflectionMoodLevel.value = null
        pendingAudioPath.value = null
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
