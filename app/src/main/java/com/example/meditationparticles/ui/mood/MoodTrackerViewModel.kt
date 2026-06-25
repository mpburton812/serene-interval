package com.example.meditationparticles.ui.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.mood.MoodPeriodAverages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn

class MoodTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val moodTracker = AppGraph.moodTracker(application)

    val averages: StateFlow<MoodPeriodAverages> = moodTracker.observeAverages()
        .catch { emit(MoodPeriodAverages(day = null, week = null, month = null)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoodPeriodAverages(day = null, week = null, month = null),
        )
}
