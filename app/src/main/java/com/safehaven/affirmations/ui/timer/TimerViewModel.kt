package com.safehaven.affirmations.ui.timer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.data.local.MeditationReflectionEntity
import com.safehaven.affirmations.domain.onenote.OneNoteEntryType
import com.safehaven.affirmations.domain.timer.TimerBellSoundChoice
import com.safehaven.affirmations.domain.timer.TimerEngine
import com.safehaven.affirmations.domain.timer.TimerPhase
import com.safehaven.affirmations.domain.timer.TimerSessionState
import com.safehaven.affirmations.domain.timer.TimerSoundOption
import com.safehaven.affirmations.domain.mood.MoodCalendarLogic
import com.safehaven.affirmations.domain.mood.MoodGraphPeriod
import com.safehaven.affirmations.domain.mood.canShiftPeriodForward
import com.safehaven.affirmations.domain.mood.clampPeriodOffset
import com.safehaven.affirmations.domain.mood.moodPeriodBounds
import com.safehaven.affirmations.domain.mood.moodPeriodTitle
import com.safehaven.affirmations.domain.mood.periodReferenceMillis
import com.safehaven.affirmations.domain.timer.MeditationCalendarLogic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale

data class MeditationCalendarUiState(
    val days: List<com.safehaven.affirmations.domain.timer.MeditationCalendarDay> = emptyList(),
    val weekdayHeaders: List<String> = emptyList(),
    val monthTitle: String = "",
    val monthOffset: Int = 0,
    val canGoForward: Boolean = false,
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = TimerEngine()
    private val sessionRepository = AppGraph.sessions(application)
    private val reflectionRepository = AppGraph.meditationReflections(application)
    private val oneNoteSync = AppGraph.oneNoteSync(application)
    private val calendarMonthOffset = MutableStateFlow(0)
    private val zoneId = ZoneId.systemDefault()
    private val locale = Locale.getDefault()

    val sessionState: StateFlow<TimerSessionState> = engine.state
    val reflectionText = MutableStateFlow("")
    val reflectionMoodLevel = MutableStateFlow<Int?>(null)
    val showReflectionCapture = MutableStateFlow(false)
    val openedReflection = MutableStateFlow<MeditationReflectionEntity?>(null)

    val reflections: StateFlow<List<MeditationReflectionEntity>> =
        reflectionRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val meditationCalendar: StateFlow<MeditationCalendarUiState> = calendarMonthOffset
        .flatMapLatest { offset ->
            val referenceMillis = periodReferenceMillis(MoodGraphPeriod.MONTH, offset, zoneId, locale)
            val bounds = moodPeriodBounds(MoodGraphPeriod.MONTH, referenceMillis, zoneId, locale)
            // Include timer, breathing, and visualization so past practice days stay marked.
            sessionRepository.observeSessionsInRange(bounds.startMillis, bounds.endMillis).map { sessions ->
                val practicedDates = MeditationCalendarLogic.practicedDatesFromCompletedAt(
                    completedAtMillis = sessions.map { it.completedAt },
                    zoneId = zoneId,
                )
                MeditationCalendarUiState(
                    days = MeditationCalendarLogic.buildMonthGrid(
                        yearMonth = MeditationCalendarLogic.yearMonthFromOffset(offset, zoneId),
                        practicedDates = practicedDates,
                        zoneId = zoneId,
                        locale = locale,
                    ),
                    weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                    monthTitle = moodPeriodTitle(MoodGraphPeriod.MONTH, offset, zoneId, locale),
                    monthOffset = offset,
                    canGoForward = canShiftPeriodForward(offset),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MeditationCalendarUiState(
                weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                monthTitle = moodPeriodTitle(MoodGraphPeriod.MONTH, 0, zoneId, locale),
                canGoForward = canShiftPeriodForward(0),
            ),
        )

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
        reflectionMoodLevel.value = MoodScale.normalize(level)
    }

    fun saveReflection() {
        persistReflection(includeText = true)
    }

    fun skipReflection() {
        persistReflection(includeText = false)
    }

    private fun persistReflection(includeText: Boolean) {
        viewModelScope.launch {
            if (!showReflectionCapture.value) return@launch
            val state = sessionState.value
            if (state.phase == TimerPhase.Complete) {
                val text = if (includeText) reflectionText.value else ""
                val mood = reflectionMoodLevel.value
                val savedId = reflectionRepository.save(
                    reflection = text,
                    durationSeconds = state.targetMinutes * 60,
                    completedAt = System.currentTimeMillis(),
                    moodLevel = mood,
                )
                savedId?.let { oneNoteSync.enqueueSync(OneNoteEntryType.MEDITATION_REFLECTION, it) }
            }
            finishReflectionCapture()
        }
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

    fun shiftCalendarMonth(forward: Boolean) {
        if (forward && !canShiftPeriodForward(calendarMonthOffset.value)) return
        calendarMonthOffset.value = clampPeriodOffset(
            calendarMonthOffset.value + if (forward) 1 else -1,
        )
    }

    private fun finishReflectionCapture() {
        clearReflectionDraft()
        showReflectionCapture.value = false
        engine.reset()
    }

    private fun clearReflectionDraft() {
        reflectionText.value = ""
        reflectionMoodLevel.value = null
    }

    fun restorePreferences(
        targetMinutes: Int,
        sound: TimerSoundOption,
        bellSound: TimerBellSoundChoice,
        bellSystemUri: String?,
        reminderEnabled: Boolean,
        reminderHour: Int,
        reminderMinute: Int,
    ) {
        engine.restoreFromPreferences(
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
