package com.safehaven.affirmations.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.domain.affirmations.AffirmationListKind
import com.safehaven.affirmations.domain.home.HomeActivityItem
import com.safehaven.affirmations.domain.mood.MoodSource
import com.safehaven.affirmations.domain.quickstart.QuickStartTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val affirmationRepository = AppGraph.affirmations(application)
    private val settingsPreferences = AppGraph.settings(application)
    private val quickStartPreferences = AppGraph.quickStart(application)
    private val moodTracker = AppGraph.moodTracker(application)
    private val homeActivityRepository = AppGraph.homeActivity(application)

    private val _dailyAffirmation = MutableStateFlow(DefaultFallback)
    val dailyAffirmation: StateFlow<String> = _dailyAffirmation.asStateFlow()

    val activityTimeline: StateFlow<List<HomeActivityItem>> =
        homeActivityRepository.observeTimeline()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _openedActivity = MutableStateFlow<HomeActivityItem?>(null)
    val openedActivity: StateFlow<HomeActivityItem?> = _openedActivity.asStateFlow()

    private val _showAllActivity = MutableStateFlow(false)
    val showAllActivity: StateFlow<Boolean> = _showAllActivity.asStateFlow()

    fun openActivity(item: HomeActivityItem) {
        _openedActivity.value = item
    }

    fun closeActivity() {
        _openedActivity.value = null
    }

    fun openAllActivity() {
        _showAllActivity.value = true
    }

    fun closeAllActivity() {
        _showAllActivity.value = false
    }

    val quickStartTargets: StateFlow<List<QuickStartTarget>> = quickStartPreferences.selectedTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        quickStartPreferences.load(settingsPreferences.settings.value)
        viewModelScope.launch {
            settingsPreferences.settings.collect { quickStartPreferences.refresh(it) }
        }
        viewModelScope.launch {
            AppGraph.toolkit(application).snapshot.collect {
                quickStartPreferences.refresh(settingsPreferences.settings.value)
            }
        }
        viewModelScope.launch {
            affirmationRepository.seedIfEmpty()
            AppGraph.affirmations(getApplication(), AffirmationListKind.KatiesLoveList).seedIfEmpty()
        }
    }

    fun refreshDailyAffirmation() {
        viewModelScope.launch {
            affirmationRepository.seedIfEmpty()
            _dailyAffirmation.value = affirmationRepository.randomAffirmation()?.text
                ?: DefaultFallback
        }
    }

    fun logMood(level: Int) {
        viewModelScope.launch {
            moodTracker.record(MoodSource.HOME_SCREEN, level)
        }
    }

    companion object {
        private const val DefaultFallback =
            "Your calm mind is the ultimate weapon against your challenges. So relax."
    }
}
