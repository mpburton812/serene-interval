package com.example.meditationparticles.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.domain.quickstart.QuickStartTarget
import com.example.meditationparticles.domain.sessions.HomeProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val affirmationRepository = AppGraph.affirmations(application)
    private val sessionRepository = AppGraph.sessions(application)
    private val settingsPreferences = AppGraph.settings(application)
    private val quickStartPreferences = AppGraph.quickStart(application)

    private val _dailyAffirmation = MutableStateFlow(DefaultFallback)
    val dailyAffirmation: StateFlow<String> = _dailyAffirmation.asStateFlow()

    val homeProgress: StateFlow<HomeProgress> = sessionRepository.observeHomeProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeProgress.Empty)

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
        }
    }

    fun refreshDailyAffirmation() {
        viewModelScope.launch {
            affirmationRepository.seedIfEmpty()
            _dailyAffirmation.value = affirmationRepository.randomFavoriteAffirmation()?.text
                ?: affirmationRepository.randomAffirmation()?.text
                ?: DefaultFallback
        }
    }

    companion object {
        private const val DefaultFallback =
            "Your calm mind is the ultimate weapon against your challenges. So relax."
    }
}
