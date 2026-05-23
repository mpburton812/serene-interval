package com.example.meditationparticles.ui.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AffirmationPreferences
import com.example.meditationparticles.data.AffirmationRepository
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.reminder.AffirmationReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AffirmationViewMode { Card, List }

data class AffirmationsUiState(
    val affirmations: List<AffirmationEntity> = emptyList(),
    val currentIndex: Int = 0,
    val viewMode: AffirmationViewMode = AffirmationViewMode.Card,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val showAddDialog: Boolean = false,
    val editingAffirmation: AffirmationEntity? = null,
) {
    val currentAffirmation: AffirmationEntity?
        get() = affirmations.getOrNull(currentIndex.coerceIn(0, (affirmations.size - 1).coerceAtLeast(0)))
}

class AffirmationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AffirmationRepository = AppGraph.affirmations(application)
    private val preferences = AffirmationPreferences(application)

    private val _uiState = MutableStateFlow(AffirmationsUiState())
    val uiState: StateFlow<AffirmationsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            val saved = preferences.load()
            _uiState.update {
                it.copy(
                    reminderEnabled = saved.reminderEnabled,
                    reminderHour = saved.reminderHour,
                    reminderMinute = saved.reminderMinute,
                    viewMode = if (saved.viewMode == AffirmationPreferences.ViewMode.List.name) {
                        AffirmationViewMode.List
                    } else {
                        AffirmationViewMode.Card
                    },
                )
            }
        }

        viewModelScope.launch {
            repository.affirmations.collect { list ->
                _uiState.update { state ->
                    val index = state.currentIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                    state.copy(affirmations = list, currentIndex = index)
                }
            }
        }
    }

    fun nextAffirmation() {
        val size = _uiState.value.affirmations.size
        if (size == 0) return
        _uiState.update { it.copy(currentIndex = (it.currentIndex + 1) % size) }
    }

    fun setViewMode(mode: AffirmationViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        persistPrefs()
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true, editingAffirmation = null) }

    fun showEditDialog(entity: AffirmationEntity) =
        _uiState.update { it.copy(showAddDialog = true, editingAffirmation = entity) }

    fun dismissDialog() = _uiState.update { it.copy(showAddDialog = false, editingAffirmation = null) }

    fun saveAffirmation(text: String) {
        viewModelScope.launch {
            val editing = _uiState.value.editingAffirmation
            if (editing != null) {
                repository.update(editing.copy(text = text.trim()))
            } else {
                repository.add(text)
            }
            dismissDialog()
        }
    }

    fun deleteAffirmation(entity: AffirmationEntity) {
        viewModelScope.launch {
            repository.delete(entity)
        }
    }

    fun toggleFavorite(entity: AffirmationEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(entity)
        }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        _uiState.update {
            it.copy(
                reminderEnabled = enabled,
                reminderHour = hour,
                reminderMinute = minute,
            )
        }
        persistPrefs()
        if (enabled) {
            AffirmationReminderScheduler.schedule(getApplication(), hour, minute)
        } else {
            AffirmationReminderScheduler.cancel(getApplication())
        }
    }

    private fun persistPrefs() {
        val state = _uiState.value
        preferences.save(
            AffirmationPreferences.AffirmationPrefsSnapshot(
                reminderEnabled = state.reminderEnabled,
                reminderHour = state.reminderHour,
                reminderMinute = state.reminderMinute,
                viewMode = state.viewMode.name,
            ),
        )
    }
}
