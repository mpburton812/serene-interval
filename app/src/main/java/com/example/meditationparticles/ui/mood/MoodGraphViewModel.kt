package com.example.meditationparticles.ui.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.MoodMonthGraphMode
import com.example.meditationparticles.domain.mood.moodPeriodBounds
import com.example.meditationparticles.domain.mood.moodPeriodTitle
import com.example.meditationparticles.domain.mood.periodReferenceMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MoodGraphUiState(
    val entries: List<MoodEntryEntity> = emptyList(),
    val average: Double? = null,
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
    val periodOffset: Int = 0,
    val periodTitle: String = "",
    val monthGraphMode: MoodMonthGraphMode = MoodMonthGraphMode.TOTAL_AVERAGE,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MoodGraphViewModel(
    application: Application,
    private val period: MoodGraphPeriod,
) : AndroidViewModel(application) {
    private val moodTracker = AppGraph.moodTracker(application)
    private val periodOffset = MutableStateFlow(0)
    private val monthGraphMode = MutableStateFlow(MoodMonthGraphMode.TOTAL_AVERAGE)

    val uiState: StateFlow<MoodGraphUiState> = combine(
        periodOffset,
        monthGraphMode,
    ) { offset, mode ->
        offset to mode
    }.flatMapLatest { (offset, mode) ->
        moodTracker.observeEntriesForPeriod(period, periodOffset = offset).map { entries ->
            val referenceMillis = periodReferenceMillis(period, offset)
            val bounds = moodPeriodBounds(period, referenceMillis)
            MoodGraphUiState(
                entries = entries,
                average = entries.takeIf { it.isNotEmpty() }
                    ?.map { it.moodLevel.toDouble() }
                    ?.average(),
                startMillis = bounds.startMillis,
                endMillis = bounds.endMillis,
                periodOffset = offset,
                periodTitle = moodPeriodTitle(period, offset),
                monthGraphMode = mode,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MoodGraphUiState(
            periodTitle = moodPeriodTitle(period, 0),
        ),
    )

    val monthGraphModeState: StateFlow<MoodMonthGraphMode> = monthGraphMode.asStateFlow()

    fun shiftPeriod(forward: Boolean) {
        periodOffset.value += if (forward) 1 else -1
    }

    fun setMonthGraphMode(mode: MoodMonthGraphMode) {
        monthGraphMode.value = mode
    }
}
