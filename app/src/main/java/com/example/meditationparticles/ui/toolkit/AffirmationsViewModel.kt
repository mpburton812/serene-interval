package com.example.meditationparticles.ui.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AffirmationPreferences
import com.example.meditationparticles.data.AffirmationRepository
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.domain.affirmations.AffirmationCalendarLogic
import com.example.meditationparticles.domain.affirmations.AffirmationListKind
import com.example.meditationparticles.domain.affirmations.AffirmationReviewLogic
import com.example.meditationparticles.domain.mood.MoodCalendarLogic
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.mood.moodPeriodBounds
import com.example.meditationparticles.domain.mood.moodPeriodTitle
import com.example.meditationparticles.domain.mood.periodReferenceMillis
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.reminder.AffirmationReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale

data class AffirmationCalendarUiState(
    val days: List<com.example.meditationparticles.domain.affirmations.AffirmationCalendarDay> = emptyList(),
    val weekdayHeaders: List<String> = emptyList(),
    val monthTitle: String = "",
)

data class AffirmationsUiState(
    val affirmations: List<AffirmationEntity> = emptyList(),
    val archivedAffirmations: List<AffirmationEntity> = emptyList(),
    val showArchived: Boolean = false,
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

class AffirmationsViewModel(
    application: Application,
    val listKind: AffirmationListKind = AffirmationListKind.Affirmations,
) : AndroidViewModel(application) {
    private val repository: AffirmationRepository = AppGraph.affirmations(application, listKind)
    private val reviewSessionRepository = AppGraph.affirmationReviewSessions(application, listKind)
    private val oneNoteSync by lazy { AppGraph.oneNoteSync(application) }
    private val preferences = AffirmationPreferences(application, listKind)
    private val calendarMonthOffset = MutableStateFlow(0)
    private val zoneId = ZoneId.systemDefault()
    private val locale = Locale.getDefault()

    private val _uiState = MutableStateFlow(AffirmationsUiState())
    val uiState: StateFlow<AffirmationsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val affirmationCalendar: StateFlow<AffirmationCalendarUiState> = calendarMonthOffset
        .flatMapLatest { offset ->
            val referenceMillis = periodReferenceMillis(MoodGraphPeriod.MONTH, offset, zoneId, locale)
            val bounds = moodPeriodBounds(MoodGraphPeriod.MONTH, referenceMillis, zoneId, locale)
            reviewSessionRepository.observeInRange(bounds.startMillis, bounds.endMillis).map { sessions ->
                val reviewedDates = AffirmationCalendarLogic.reviewedDatesFromCompletedAt(
                    completedAtMillis = sessions.map { it.completedAt },
                    zoneId = zoneId,
                )
                AffirmationCalendarUiState(
                    days = AffirmationCalendarLogic.buildMonthGrid(
                        yearMonth = AffirmationCalendarLogic.yearMonthFromOffset(offset, zoneId),
                        reviewedDates = reviewedDates,
                        zoneId = zoneId,
                        locale = locale,
                    ),
                    weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                    monthTitle = moodPeriodTitle(MoodGraphPeriod.MONTH, offset, zoneId, locale),
                )
            }
        }
        .catch {
            emit(
                AffirmationCalendarUiState(
                    weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                    monthTitle = moodPeriodTitle(MoodGraphPeriod.MONTH, 0, zoneId, locale),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AffirmationCalendarUiState(
                weekdayHeaders = MoodCalendarLogic.weekdayHeaders(locale),
                monthTitle = moodPeriodTitle(MoodGraphPeriod.MONTH, 0, zoneId, locale),
            ),
        )

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
            repository.affirmations
                .catch { }
                .collect { list ->
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

        viewModelScope.launch {
            repository.archivedAffirmations
                .catch { }
                .collect { list ->
                    _uiState.update { it.copy(archivedAffirmations = list) }
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
            val noun = listKind.itemNoun
            _uiState.update {
                it.copy(
                    showBulkImportDialog = false,
                    importMessage = if (count > 0) {
                        "Imported $count $noun${if (count == 1) "" else "s"}."
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

    fun archiveAffirmation(entity: AffirmationEntity) {
        viewModelScope.launch {
            repository.archive(entity)
        }
    }

    fun unarchiveAffirmation(entity: AffirmationEntity) {
        viewModelScope.launch {
            repository.unarchive(entity)
        }
    }

    fun toggleShowArchived() {
        _uiState.update { it.copy(showArchived = !it.showArchived) }
    }

    fun reorderAffirmations(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            repository.reorder(fromIndex, toIndex)
        }
    }

    fun shiftCalendarMonth(forward: Boolean) {
        calendarMonthOffset.value += if (forward) 1 else -1
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
            AffirmationReminderScheduler.schedule(getApplication(), listKind, hour, minute)
        } else {
            AffirmationReminderScheduler.cancel(getApplication(), listKind)
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

    companion object {
        fun factory(
            application: Application,
            listKind: AffirmationListKind,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AffirmationsViewModel(application, listKind) as T
                }
            }
    }
}
