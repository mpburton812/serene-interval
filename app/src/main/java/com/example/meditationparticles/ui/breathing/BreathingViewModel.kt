package com.example.meditationparticles.ui.breathing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.BreathingDisplayPreferences
import com.example.meditationparticles.domain.breathing.BreathPhase
import com.example.meditationparticles.domain.breathing.BreathingEngine
import com.example.meditationparticles.domain.breathing.BreathingPattern
import com.example.meditationparticles.domain.breathing.BreathingSessionState
import com.example.meditationparticles.domain.breathing.BreathingVisualMode
import com.example.meditationparticles.domain.breathing.SessionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BreathingViewModel(application: Application) : AndroidViewModel(application) {
    private val engine = BreathingEngine()
    private val sessionRepository = AppGraph.sessions(application)
    private val displayPreferences = BreathingDisplayPreferences(application)

    val sessionState: StateFlow<BreathingSessionState> = engine.state
    private val _visualMode = MutableStateFlow(displayPreferences.loadVisualMode())
    val visualMode: StateFlow<BreathingVisualMode> = _visualMode.asStateFlow()

    init {
        viewModelScope.launch {
            var loggedCompletion = false
            sessionState.collect { state ->
                if (state.phase == BreathPhase.Complete && !loggedCompletion) {
                    loggedCompletion = true
                    val durationSeconds = (state.elapsedSessionMs / 1000L).toInt().coerceAtLeast(1)
                    sessionRepository.logBreathing(
                        patternName = state.pattern.name,
                        durationSeconds = durationSeconds,
                    )
                }
                if (state.phase != BreathPhase.Complete) {
                    loggedCompletion = false
                }
            }
        }
    }

    fun selectPattern(pattern: BreathingPattern) = engine.selectPattern(pattern)
    fun setVisualMode(mode: BreathingVisualMode) {
        _visualMode.value = mode
        displayPreferences.saveVisualMode(mode)
    }
    fun setSessionMode(mode: SessionMode) = engine.setSessionMode(mode)
    fun setTargetMinutes(minutes: Int) = engine.setTargetMinutes(minutes)
    fun setTargetRepetitions(reps: Int) = engine.setTargetRepetitions(reps)
    fun toggleRunning() = engine.toggleRunning()
    fun reset() = engine.reset()

    override fun onCleared() {
        engine.pause()
        super.onCleared()
    }
}
