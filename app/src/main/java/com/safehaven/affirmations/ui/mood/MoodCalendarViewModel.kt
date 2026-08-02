package com.safehaven.affirmations.ui.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.domain.mood.MoodCalendarDay
import com.safehaven.affirmations.domain.mood.MoodCalendarDayEvents
import com.safehaven.affirmations.domain.mood.MoodCalendarEvent
import com.safehaven.affirmations.domain.mood.MoodCalendarLogic
import com.safehaven.affirmations.domain.mood.MoodGraphPeriod
import com.safehaven.affirmations.domain.mood.canShiftPeriodForward
import com.safehaven.affirmations.domain.mood.clampPeriodOffset
import com.safehaven.affirmations.domain.mood.moodPeriodTitle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class MoodCalendarUiState(
    val days: List<MoodCalendarDay> = emptyList(),
    val weekdayHeaders: List<String> = emptyList(),
    val monthTitle: String = "",
    val monthOffset: Int = 0,
    val canGoForward: Boolean = false,
    val selectedDay: LocalDate? = null,
    val selectedDayEvents: List<MoodCalendarEvent> = emptyList(),
    val selectedEvent: MoodCalendarEvent? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoodCalendarViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val moodTracker = AppGraph.moodTracker(application)
    private val homeActivity = AppGraph.homeActivity(application)
    private val monthOffset = MutableStateFlow(0)
    private val selectedDay = MutableStateFlow<LocalDate?>(null)
    private val selectedEvent = MutableStateFlow<MoodCalendarEvent?>(null)
    private val zoneId = ZoneId.systemDefault()
    private val locale = Locale.getDefault()

    val uiState: StateFlow<MoodCalendarUiState> = combine(
        monthOffset,
        selectedDay,
        selectedEvent,
    ) { offset, day, event ->
        Triple(offset, day, event)
    }.flatMapLatest { (offset, day, event) ->
        moodTracker.observeEntriesForPeriod(
            period = MoodGraphPeriod.CALENDAR,
            periodOffset = offset,
        ).flatMapLatest { entries ->
            val yearMonth = MoodCalendarLogic.yearMonthFromOffset(offset, zoneId)
            val baseState = MoodCalendarUiState(
                days = MoodCalendarLogic.buildMonthGrid(yearMonth, entries, zoneId, locale),
                weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                monthTitle = moodPeriodTitle(MoodGraphPeriod.CALENDAR, offset, zoneId, locale),
                monthOffset = offset,
                canGoForward = canShiftPeriodForward(offset),
                selectedDay = day,
                selectedEvent = event,
            )
            if (day == null) {
                flowOf(baseState)
            } else {
                homeActivity.observeActivitiesForDay(day, zoneId).map { activities ->
                    baseState.copy(
                        selectedDayEvents = MoodCalendarDayEvents.build(
                            date = day,
                            moodEntries = entries,
                            activities = activities,
                            zoneId = zoneId,
                        ),
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoodCalendarUiState(
            weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
            monthTitle = moodPeriodTitle(MoodGraphPeriod.CALENDAR, 0, zoneId, locale),
        ),
    )

    fun shiftMonth(forward: Boolean) {
        if (forward && !canShiftPeriodForward(monthOffset.value)) return
        monthOffset.value = clampPeriodOffset(monthOffset.value + if (forward) 1 else -1)
        selectedDay.value = null
        selectedEvent.value = null
    }

    fun selectDay(date: LocalDate) {
        selectedDay.value = date
        selectedEvent.value = null
    }

    fun dismissDay() {
        selectedDay.value = null
        selectedEvent.value = null
    }

    fun openEvent(event: MoodCalendarEvent) {
        selectedEvent.value = event
    }

    fun dismissEvent() {
        selectedEvent.value = null
    }
}
