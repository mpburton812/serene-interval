package com.safehaven.affirmations.ui.thermometer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safehaven.affirmations.data.AppGraph
import com.safehaven.affirmations.data.local.ThermometerEntity
import com.safehaven.affirmations.data.local.ThermometerEventEntity
import com.safehaven.affirmations.data.local.ThermometerEventType
import com.safehaven.affirmations.domain.thermometer.ThermometerChangeText
import com.safehaven.affirmations.domain.thermometer.ThermometerLiquid
import com.safehaven.affirmations.domain.thermometer.ThermometerRules
import kotlinx.coroutines.Job
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
    val draftRelief: String = "",
    val liquid: ThermometerLiquid = ThermometerLiquid.RED,
    val baselineScore: Int? = null,
    val scoreTouched: Boolean = false,
) {
    val open: Boolean get() = creating || editingId != null
}

data class SlideCapture(
    val thermometerId: Long,
    val score: Int,
    val note: String = "",
    val relief: String = "",
)

class ThermometersViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppGraph.thermometers(application)
    private var editorJob: Job? = null

    val uiState: StateFlow<ThermometersUiState> = combine(
        repository.observeThermometers(),
        repository.observeEvents(),
    ) { thermometers, events ->
        ThermometersUiState(thermometers = thermometers, events = events)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThermometersUiState())

    private val _draft = MutableStateFlow(EditorDraft())
    val draft: StateFlow<EditorDraft> = _draft.asStateFlow()

    private val _slide = MutableStateFlow<SlideCapture?>(null)
    val slide: StateFlow<SlideCapture?> = _slide.asStateFlow()

    fun startCreate() {
        if (!uiState.value.canCreate) return
        _draft.value = EditorDraft(creating = true)
    }

    fun open(id: Long) {
        val thermometer = uiState.value.thermometers.find { it.id == id } ?: return
        val latest = uiState.value.latestScore(id)
        _draft.value = EditorDraft(
            editingId = id,
            draftName = thermometer.name,
            draftScore = (latest ?: 50).toFloat(),
            liquid = ThermometerLiquid.fromStored(thermometer.liquidColor),
            baselineScore = latest,
        )
    }

    fun closeEditor() {
        enqueue {
            val current = _draft.value
            if (!current.open) return@enqueue
            _draft.value = EditorDraft()
            val id = persistIdentity(current) ?: return@enqueue
            persistReading(id, current)
        }
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

    fun updateRelief(relief: String) {
        _draft.value = _draft.value.copy(draftRelief = relief)
    }

    fun updateLiquid(liquid: ThermometerLiquid) {
        _draft.value = _draft.value.copy(liquid = liquid)
    }

    fun saveName() {
        enqueue {
            val current = _draft.value
            val id = persistIdentity(current) ?: return@enqueue
            if (current.creating) {
                _draft.value = current.copy(
                    creating = false,
                    editingId = id,
                    scoreTouched = false,
                )
            }
        }
    }

    fun archive() {
        enqueue {
            val current = _draft.value
            val id = current.editingId ?: return@enqueue
            persistReading(id, current)
            repository.archive(id)
            _draft.value = EditorDraft()
        }
    }

    fun delete() {
        enqueue {
            val id = _draft.value.editingId ?: return@enqueue
            repository.delete(id)
            _draft.value = EditorDraft()
        }
    }

    fun commitSlide(id: Long, score: Int) {
        _slide.value = SlideCapture(
            thermometerId = id,
            score = ThermometerRules.clampScore(score),
        )
    }

    fun updateSlideNote(note: String) {
        _slide.value = _slide.value?.copy(note = note)
    }

    fun updateSlideRelief(relief: String) {
        _slide.value = _slide.value?.copy(relief = relief)
    }

    fun closeSlide() {
        val current = _slide.value ?: return
        _slide.value = null
        viewModelScope.launch {
            repository.record(
                id = current.thermometerId,
                score = current.score,
                note = current.note,
                reliefNote = current.relief,
            )
        }
    }

    private fun enqueue(block: suspend () -> Unit) {
        val previous = editorJob
        editorJob = viewModelScope.launch {
            previous?.join()
            block()
        }
    }

    private suspend fun persistIdentity(current: EditorDraft): Long? {
        if (current.creating) {
            return repository.create(current.draftName, current.liquid)
        }
        val id = current.editingId ?: return null
        if (current.draftName.isNotBlank()) repository.rename(id, current.draftName)
        repository.setLiquidColor(id, current.liquid)
        return id
    }

    private suspend fun persistReading(id: Long, current: EditorDraft) {
        if (
            !ThermometerChangeText.shouldSaveOnClose(
                baselineScore = current.baselineScore,
                score = current.draftScore.toInt(),
                reason = current.draftNote,
                lowerPlan = current.draftRelief,
                scoreTouched = current.scoreTouched,
            )
        ) {
            return
        }
        repository.record(
            id = id,
            score = current.draftScore.toInt(),
            note = current.draftNote,
            reliefNote = current.draftRelief,
        )
    }
}
