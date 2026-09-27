package com.safehaven.affirmations.ui.thermometer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.data.local.ThermometerEntity
import com.safehaven.affirmations.data.local.ThermometerEventEntity
import com.safehaven.affirmations.data.local.ThermometerEventType
import com.safehaven.affirmations.domain.thermometer.ThermometerRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThermometersUiState(
    val thermometers: List<ThermometerEntity> = emptyList(),
    val events: List<ThermometerEventEntity> = emptyList(),
) {
    val active: List<ThermometerEntity> get() = thermometers.filter { !it.isArchived }
    val archived: List<ThermometerEntity> get() = thermometers.filter { it.isArchived }
    val canCreate: Boolean get() = ThermometerRules.canCreate(active.size)

    fun eventsFor(id: Long): List<ThermometerEventEntity> =
        events.filter { it.thermometerId == id }

    fun latestScore(id: Long): Int? =
        eventsFor(id)
            .filter { it.type == ThermometerEventType.READING }
            .maxWithOrNull(compareBy<ThermometerEventEntity> { it.recordedAt }.thenBy { it.id })
            ?.score
}

data class EditorDraft(
    val creating: Boolean = false,
    val editingId: Long? = null,
    val draftName: String = "",
    val draftScore: Float = 50f,
    val draftNote: String = "",
    val scoreTouched: Boolean = false,
) {
    val open: Boolean get() = creating || editingId != null
}

class ThermometersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppGraph.thermometers(application)

    val uiState: StateFlow<ThermometersUiState> = combine(
        repository.observeThermometers(),
        repository.observeEvents(),
    ) { thermometers, events ->
        ThermometersUiState(thermometers = thermometers, events = events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThermometersUiState())

    private val _draft = MutableStateFlow(EditorDraft())
    val draft: StateFlow<EditorDraft> = _draft.asStateFlow()

    fun startCreate() {
        if (!uiState.value.canCreate) return
        _draft.value = EditorDraft(creating = true)
    }

    fun open(id: Long) {
        val name = uiState.value.thermometers.find { it.id == id }?.name.orEmpty()
        val latest = uiState.value.latestScore(id)?.toFloat() ?: 50f
        _draft.value = EditorDraft(
            editingId = id,
            draftName = name,
            draftScore = latest,
        )
    }

    fun closeEditor() {
        _draft.value = EditorDraft()
    }

    fun updateName(name: String) {
        _draft.value = _draft.value.copy(draftName = name)
    }

    fun updateScore(score: Float) {
        _draft.value = _draft.value.copy(draftScore = score, scoreTouched = true)
    }

    fun updateNote(note: String) {
        _draft.value = _draft.value.copy(draftNote = note)
    }

    fun saveName() {
        val current = _draft.value
        viewModelScope.launch {
            if (current.creating) {
                val id = repository.create(current.draftName) ?: return@launch
                _draft.value = current.copy(creating = false, editingId = id, scoreTouched = false)
            } else {
                val id = current.editingId ?: return@launch
                repository.rename(id, current.draftName)
            }
        }
    }

    fun recordTemperature() {
        val current = _draft.value
        val id = current.editingId ?: return
        viewModelScope.launch {
            repository.record(id, current.draftScore.toInt(), current.draftNote)
            _draft.value = current.copy(draftNote = "", scoreTouched = false)
        }
    }

    fun archive() {
        val id = _draft.value.editingId ?: return
        viewModelScope.launch {
            repository.archive(id)
            closeEditor()
        }
    }

    fun delete() {
        val id = _draft.value.editingId ?: return
        viewModelScope.launch {
            repository.delete(id)
            closeEditor()
        }
    }
}
