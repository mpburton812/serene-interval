package com.example.meditationparticles.ui.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.MoodEntryEntity
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.moodPeriodBounds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MoodGraphUiState(
    val entries: List<MoodEntryEntity> = emptyList(),
    val average: Double? = null,
    val startMillis: Long = 0L,
    val endMillis: Long = 0L,
)

class MoodGraphViewModel(
    application: Application,
    private val period: MoodGraphPeriod,
) : AndroidViewModel(application) {
    private val moodTracker = AppGraph.moodTracker(application)
    private val bounds = moodPeriodBounds(period)

    val uiState: StateFlow<MoodGraphUiState> = moodTracker.observeEntriesForPeriod(period)
        .map { entries ->
            MoodGraphUiState(
                entries = entries,
                average = entries.takeIf { it.isNotEmpty() }
                    ?.map { it.moodLevel.toDouble() }
                    ?.average(),
                startMillis = bounds.startMillis,
                endMillis = bounds.endMillis,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoodGraphUiState(
                startMillis = bounds.startMillis,
                endMillis = bounds.endMillis,
            ),
        )
}
