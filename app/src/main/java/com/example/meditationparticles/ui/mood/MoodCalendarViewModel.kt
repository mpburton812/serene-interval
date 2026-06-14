package com.example.meditationparticles.ui.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.mood.MoodCalendarDay
import com.example.meditationparticles.domain.mood.MoodCalendarLogic
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.moodPeriodTitle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.util.Locale

data class MoodCalendarUiState(
    val days: List<MoodCalendarDay> = emptyList(),
    val weekdayHeaders: List<String> = emptyList(),
    val monthTitle: String = "",
    val monthOffset: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoodCalendarViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val moodTracker = AppGraph.moodTracker(application)
    private val monthOffset = MutableStateFlow(0)
    private val zoneId = ZoneId.systemDefault()
    private val locale = Locale.getDefault()

    val uiState: StateFlow<MoodCalendarUiState> = monthOffset
        .flatMapLatest { offset ->
            moodTracker.observeEntriesForPeriod(
                period = MoodGraphPeriod.CALENDAR,
                periodOffset = offset,
            ).map { entries ->
                val yearMonth = MoodCalendarLogic.yearMonthFromOffset(offset, zoneId)
                MoodCalendarUiState(
                    days = MoodCalendarLogic.buildMonthGrid(yearMonth, entries, zoneId, locale),
                    weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                    monthTitle = moodPeriodTitle(MoodGraphPeriod.CALENDAR, offset, zoneId, locale),
                    monthOffset = offset,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoodCalendarUiState(
                weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                monthTitle = moodPeriodTitle(MoodGraphPeriod.CALENDAR, 0, zoneId, locale),
            ),
        )

    fun shiftMonth(forward: Boolean) {
        monthOffset.value += if (forward) 1 else -1
    }
}
