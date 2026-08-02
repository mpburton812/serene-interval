package com.safehaven.affirmations.ui.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.domain.mood.MoodPeriodAverages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MoodTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val moodTracker = AppGraph.moodTracker(application)

    val averages: StateFlow<MoodPeriodAverages> = moodTracker.observeAverages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MoodPeriodAverages(day = null, week = null, month = null),
        )
}
