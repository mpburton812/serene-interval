package com.example.meditationparticles.ui.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AffirmationPreferences
import com.example.meditationparticles.data.AffirmationRepository
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.domain.affirmations.AffirmationReviewLogic
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.reminder.AffirmationReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AffirmationsUiState(
    val affirmations: List<AffirmationEntity> = emptyList(),
    val currentIndex: Int = 0,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val showAddDialog: Boolean = false,
    val showBulkImportDialog: Boolean = false,
    val importMessage: String? = null,
    val editingAffirmation: AffirmationEntity? = null,
    val showReview: Boolean = false,
    val reviewIndex: Int = 0,
    val showReviewAssessment: Boolean = false,
    val completedReviewAffirmationCount: Int = 0,
    val reviewAssessmentNotes: String = "",
    val reviewAssessmentMoodLevel: Int? = null,
) {
    val currentAffirmation: AffirmationEntity?
        get() = affirmations.getOrNull(currentIndex.coerceIn(0, (affirmations.size - 1).coerceAtLeast(0)))

    val reviewAffirmation: AffirmationEntity?
        get() = affirmations.getOrNull(reviewIndex.coerceIn(0, (affirmations.size - 1).coerceAtLeast(0)))

    val canStartReview: Boolean
        get() = AffirmationReviewLogic.canStartReview(affirmations.size)
}

class AffirmationsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AffirmationRepository = AppGraph.affirmations(application)
    private val reviewSessionRepository = AppGraph.affirmationReviewSessions(application)
    private val oneNoteSync = AppGraph.oneNoteSync(application)
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
                )
            }
        }

        viewModelScope.launch {
            repository.affirmations.collect { list ->
                _uiState.update { state ->
                    val index = state.currentIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                    val reviewIndex = state.reviewIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
                    state.copy(
                        affirmations = list,
                        currentIndex = index,
                        reviewIndex = reviewIndex,
                    )
                }
            }
        }
    }

    fun nextAffirmation() {
        val size = _uiState.value.affirmations.size
        if (size == 0) return
        _uiState.update { it.copy(currentIndex = (it.currentIndex + 1) % size) }
    }

    fun startReview() {
        if (!_uiState.value.canStartReview) return
        _uiState.update {
            it.copy(
                showReview = true,
                reviewIndex = 0,
                showReviewAssessment = false,
                completedReviewAffirmationCount = 0,
                reviewAssessmentNotes = "",
                reviewAssessmentMoodLevel = null,
            )
        }
    }

    fun reviewPrevious() {
        val state = _uiState.value
        if (!state.showReview || state.showReviewAssessment) return
        AffirmationReviewLogic.previousIndex(state.reviewIndex)?.let { previous ->
            _uiState.update { it.copy(reviewIndex = previous) }
        }
    }

    fun reviewNext() {
        val state = _uiState.value
        if (!state.showReview || state.showReviewAssessment || state.affirmations.isEmpty()) return
        val lastIndex = state.affirmations.lastIndex
        if (AffirmationReviewLogic.shouldCompleteReview(state.reviewIndex, lastIndex)) {
            completeReview()
            return
        }
        AffirmationReviewLogic.nextIndex(state.reviewIndex, lastIndex)?.let { next ->
            _uiState.update { it.copy(reviewIndex = next) }
        }
    }

    fun exitReview() {
        if (_uiState.value.showReviewAssessment) return
        _uiState.update {
            it.copy(
                showReview = false,
                reviewIndex = 0,
                showReviewAssessment = false,
                completedReviewAffirmationCount = 0,
                reviewAssessmentNotes = "",
                reviewAssessmentMoodLevel = null,
            )
        }
    }

    private fun completeReview() {
        val count = _uiState.value.affirmations.size
        _uiState.update {
            it.copy(
                showReview = false,
                reviewIndex = 0,
                showReviewAssessment = true,
                completedReviewAffirmationCount = count,
                reviewAssessmentNotes = "",
                reviewAssessmentMoodLevel = null,
            )
        }
    }

    fun updateReviewAssessmentNotes(notes: String) {
        _uiState.update { it.copy(reviewAssessmentNotes = notes) }
    }

    fun updateReviewAssessmentMoodLevel(level: Int?) {
        _uiState.update { it.copy(reviewAssessmentMoodLevel = MoodScale.normalize(level)) }
    }

    fun saveReviewAssessment() {
        val state = _uiState.value
        if (!state.showReviewAssessment) return
        if (!AffirmationReviewLogic.canSaveAssessment(state.reviewAssessmentMoodLevel, state.reviewAssessmentNotes)) {
            return
        }

        viewModelScope.launch {
            val completedAt = System.currentTimeMillis()
            val savedId = reviewSessionRepository.save(
                notes = state.reviewAssessmentNotes,
                affirmationCount = state.completedReviewAffirmationCount,
                completedAt = completedAt,
                moodLevel = state.reviewAssessmentMoodLevel,
            )
            oneNoteSync.enqueueSync(OneNoteEntryType.AFFIRMATION_REVIEW, savedId)
            dismissReviewAssessment()
        }
    }

    fun skipReviewAssessment() {
        dismissReviewAssessment()
    }

    private fun dismissReviewAssessment() {
        _uiState.update {
            it.copy(
                showReviewAssessment = false,
                completedReviewAffirmationCount = 0,
                reviewAssessmentNotes = "",
                reviewAssessmentMoodLevel = null,
            )
        }
    }

    fun showAddDialog() = _uiState.update { it.copy(showAddDialog = true, editingAffirmation = null) }

    fun showBulkImportDialog() =
        _uiState.update { it.copy(showBulkImportDialog = true, importMessage = null) }

    fun dismissBulkImportDialog() = _uiState.update { it.copy(showBulkImportDialog = false) }

    fun clearImportMessage() = _uiState.update { it.copy(importMessage = null) }

    fun showEditDialog(entity: AffirmationEntity) =
        _uiState.update { it.copy(showAddDialog = true, editingAffirmation = entity) }

    fun dismissDialog() = _uiState.update { it.copy(showAddDialog = false, editingAffirmation = null) }

    fun bulkImport(text: String) {
        viewModelScope.launch {
            val count = repository.bulkAdd(text)
            _uiState.update {
                it.copy(
                    showBulkImportDialog = false,
                    importMessage = if (count > 0) {
                        "Imported $count affirmation${if (count == 1) "" else "s"}."
                    } else {
                        null
                    },
                )
            }
        }
    }

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

    fun reorderAffirmations(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorder(fromIndex, toIndex)
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
            ),
        )
    }
}
