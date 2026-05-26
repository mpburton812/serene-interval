package com.example.meditationparticles.ui.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.NvcEntryEntity
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import com.example.meditationparticles.domain.toolkit.ToolkitTool
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.reminder.FutureSelfMessageScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RandomToolState { Idle, Finding, Selected }

data class ToolkitUiState(
    val toolkitConfigured: Boolean = true,
    val enabledToolIds: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    val proactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Proactive),
    val reactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Reactive),
    val selectionProactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Proactive),
    val selectionReactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Reactive),
    val selectedTool: ToolkitTool? = null,
    val stepIndex: Int = 0,
    val thoughtDumpText: String = "",
    val anxietyLogText: String = "",
    val pendingAudioPath: String? = null,
    val thoughtDumpEntries: List<ThoughtDumpEntity> = emptyList(),
    val anxietyLogEntries: List<ThoughtDumpEntity> = emptyList(),
    val openedLogEntry: ThoughtDumpEntity? = null,
    val futureSelfText: String = "",
    val futureSelfScheduledAtMillis: Long = defaultFutureSelfScheduleTime(),
    val futureSelfEntries: List<FutureSelfMessageEntity> = emptyList(),
    val editingFutureSelfId: Long? = null,
    val openedFutureSelfEntry: FutureSelfMessageEntity? = null,
    val refactoringStepIndex: Int = 0,
    val refactoringInterpretation: String = "",
    val refactoringActualFacts: String = "",
    val refactoringExplanation1: String = "",
    val refactoringExplanation2: String = "",
    val refactoringExplanation3: String = "",
    val refactoringInterpretationAudio: String? = null,
    val refactoringActualFactsAudio: String? = null,
    val refactoringExplanation1Audio: String? = null,
    val refactoringExplanation2Audio: String? = null,
    val refactoringExplanation3Audio: String? = null,
    val refactoringEntries: List<RefactoringEntryEntity> = emptyList(),
    val openedRefactoringEntry: RefactoringEntryEntity? = null,
    val centerOfGravityStepIndex: Int = 0,
    val centerOfGravityThoughtsAndFeelings: String = "",
    val centerOfGravityBodyAndNeeds: String = "",
    val centerOfGravityThoughtsAndFeelingsAudio: String? = null,
    val centerOfGravityBodyAndNeedsAudio: String? = null,
    val centerOfGravityEntries: List<CenterOfGravityEntryEntity> = emptyList(),
    val openedCenterOfGravityEntry: CenterOfGravityEntryEntity? = null,
    val nvcStepIndex: Int = 0,
    val nvcObservation: String = "",
    val nvcFeeling: String = "",
    val nvcNeed: String = "",
    val nvcRequest: String = "",
    val nvcObservationAudio: String? = null,
    val nvcFeelingAudio: String? = null,
    val nvcNeedAudio: String? = null,
    val nvcRequestAudio: String? = null,
    val nvcEntries: List<NvcEntryEntity> = emptyList(),
    val openedNvcEntry: NvcEntryEntity? = null,
    val randomToolState: RandomToolState = RandomToolState.Idle,
    val randomSelectedTool: ToolkitTool? = null,
) {
    val currentStep: String?
        get() = selectedTool?.steps?.getOrNull(stepIndex)

    val isLastStep: Boolean
        get() = selectedTool != null && stepIndex >= (selectedTool.steps.size - 1)
}

class ToolkitViewModel(application: Application) : AndroidViewModel(application) {
    private val logRepository = AppGraph.thoughtDumps(application)
    private val futureSelfRepository = AppGraph.futureSelfMessages(application)
    private val refactoringRepository = AppGraph.refactoringEntries(application)
    private val centerOfGravityRepository = AppGraph.centerOfGravityEntries(application)
    private val nvcRepository = AppGraph.nvcEntries(application)
    private val toolkitPreferences = AppGraph.toolkit(application)
    private val settingsPreferences = AppGraph.settings(application)
    private val oneNoteSync = AppGraph.oneNoteSync(application)
    private val appContext = application.applicationContext

    private val _uiState = MutableStateFlow(ToolkitUiState())
    val uiState: StateFlow<ToolkitUiState> = _uiState.asStateFlow()

    init {
        applyToolkitSnapshot(
            toolkitPreferences.load(settingsPreferences.load().onboardingCompleted),
        )
        viewModelScope.launch {
            toolkitPreferences.snapshot.collect { snapshot ->
                applyToolkitSnapshot(snapshot)
            }
        }
        viewModelScope.launch {
            logRepository.observeEntries(ToolkitLogType.THOUGHT_DUMP).collect { entries ->
                _uiState.update { it.copy(thoughtDumpEntries = entries) }
            }
        }
        viewModelScope.launch {
            logRepository.observeEntries(ToolkitLogType.ANXIETY_LOG).collect { entries ->
                _uiState.update { it.copy(anxietyLogEntries = entries) }
            }
        }
        viewModelScope.launch {
            futureSelfRepository.observeAll().collect { entries ->
                _uiState.update { it.copy(futureSelfEntries = entries) }
            }
        }
        viewModelScope.launch {
            refactoringRepository.observeAll().collect { entries ->
                _uiState.update { it.copy(refactoringEntries = entries) }
            }
        }
        viewModelScope.launch {
            centerOfGravityRepository.observeAll().collect { entries ->
                _uiState.update { it.copy(centerOfGravityEntries = entries) }
            }
        }
        viewModelScope.launch {
            nvcRepository.observeAll().collect { entries ->
                _uiState.update { it.copy(nvcEntries = entries) }
            }
        }
    }

    fun toggleToolEnabled(toolId: ToolkitToolId) {
        val current = _uiState.value.enabledToolIds
        val next = if (toolId in current) {
            current - toolId
        } else {
            current + toolId
        }
        _uiState.update { it.copy(enabledToolIds = next) }
    }

    fun saveToolkitConfiguration() {
        toolkitPreferences.saveConfiguration(_uiState.value.enabledToolIds)
    }

    fun reorderProactiveTool(fromIndex: Int, toIndex: Int) {
        val currentOrder = _uiState.value.proactiveTools.map { it.id }
        val reordered = ToolkitLayout.reorder(currentOrder, fromIndex, toIndex)
        toolkitPreferences.saveProactiveOrder(reordered)
    }

    fun reorderReactiveTool(fromIndex: Int, toIndex: Int) {
        val currentOrder = _uiState.value.reactiveTools.map { it.id }
        val reordered = ToolkitLayout.reorder(currentOrder, fromIndex, toIndex)
        toolkitPreferences.saveReactiveOrder(reordered)
    }

    private fun applyToolkitSnapshot(snapshot: com.example.meditationparticles.data.ToolkitPrefsSnapshot) {
        _uiState.update { state ->
            state.copy(
                toolkitConfigured = snapshot.configured,
                enabledToolIds = snapshot.enabledToolIds,
                proactiveTools = ToolkitLayout.orderedTools(
                    category = ToolkitCategory.Proactive,
                    enabledIds = snapshot.enabledToolIds,
                    savedOrder = snapshot.proactiveOrder,
                    usageCounts = snapshot.usageCounts,
                ),
                reactiveTools = ToolkitLayout.orderedTools(
                    category = ToolkitCategory.Reactive,
                    enabledIds = snapshot.enabledToolIds,
                    savedOrder = snapshot.reactiveOrder,
                    usageCounts = snapshot.usageCounts,
                ),
                selectionProactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Proactive),
                selectionReactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Reactive),
            )
        }
    }

    fun openTool(tool: ToolkitTool) {
        toolkitPreferences.incrementUsageCount(tool.id)
        _uiState.update {
            it.copy(
                selectedTool = tool,
                stepIndex = 0,
                randomToolState = RandomToolState.Idle,
                thoughtDumpText = "",
                anxietyLogText = "",
                pendingAudioPath = null,
                openedLogEntry = null,
                futureSelfText = "",
                futureSelfScheduledAtMillis = defaultFutureSelfScheduleTime(),
                editingFutureSelfId = null,
                openedFutureSelfEntry = null,
                refactoringStepIndex = 0,
                refactoringInterpretation = "",
                refactoringActualFacts = "",
                refactoringExplanation1 = "",
                refactoringExplanation2 = "",
                refactoringExplanation3 = "",
                refactoringInterpretationAudio = null,
                refactoringActualFactsAudio = null,
                refactoringExplanation1Audio = null,
                refactoringExplanation2Audio = null,
                refactoringExplanation3Audio = null,
                openedRefactoringEntry = null,
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
                centerOfGravityThoughtsAndFeelingsAudio = null,
                centerOfGravityBodyAndNeedsAudio = null,
                openedCenterOfGravityEntry = null,
                nvcStepIndex = 0,
                nvcObservation = "",
                nvcFeeling = "",
                nvcNeed = "",
                nvcRequest = "",
                nvcObservationAudio = null,
                nvcFeelingAudio = null,
                nvcNeedAudio = null,
                nvcRequestAudio = null,
                openedNvcEntry = null,
            )
        }
    }

    fun openFutureSelfMessage(messageId: Long) {
        viewModelScope.launch {
            val message = futureSelfRepository.getById(messageId) ?: return@launch
            val tool = ToolkitCatalog.byId(ToolkitToolId.FutureSelfMessage) ?: return@launch
            toolkitPreferences.incrementUsageCount(tool.id)
            _uiState.update {
                it.copy(
                    selectedTool = tool,
                    stepIndex = 0,
                    openedFutureSelfEntry = message,
                    futureSelfText = "",
                    futureSelfScheduledAtMillis = defaultFutureSelfScheduleTime(),
                    editingFutureSelfId = null,
                    pendingAudioPath = null,
                )
            }
        }
    }

    fun handlePendingNavigation(toolId: ToolkitToolId?, futureSelfMessageId: Long?) {
        when (toolId) {
            ToolkitToolId.FutureSelfMessage -> {
                if (futureSelfMessageId != null) {
                    openFutureSelfMessage(futureSelfMessageId)
                } else {
                    ToolkitCatalog.byId(ToolkitToolId.FutureSelfMessage)?.let(::openTool)
                }
            }
            else -> Unit
        }
    }

    fun closeTool() {
        _uiState.update {
            it.copy(
                selectedTool = null,
                stepIndex = 0,
                thoughtDumpText = "",
                anxietyLogText = "",
                pendingAudioPath = null,
                openedLogEntry = null,
                futureSelfText = "",
                futureSelfScheduledAtMillis = defaultFutureSelfScheduleTime(),
                editingFutureSelfId = null,
                openedFutureSelfEntry = null,
                refactoringStepIndex = 0,
                refactoringInterpretation = "",
                refactoringActualFacts = "",
                refactoringExplanation1 = "",
                refactoringExplanation2 = "",
                refactoringExplanation3 = "",
                refactoringInterpretationAudio = null,
                refactoringActualFactsAudio = null,
                refactoringExplanation1Audio = null,
                refactoringExplanation2Audio = null,
                refactoringExplanation3Audio = null,
                openedRefactoringEntry = null,
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
                centerOfGravityThoughtsAndFeelingsAudio = null,
                centerOfGravityBodyAndNeedsAudio = null,
                openedCenterOfGravityEntry = null,
                nvcStepIndex = 0,
                nvcObservation = "",
                nvcFeeling = "",
                nvcNeed = "",
                nvcRequest = "",
                nvcObservationAudio = null,
                nvcFeelingAudio = null,
                nvcNeedAudio = null,
                nvcRequestAudio = null,
                openedNvcEntry = null,
            )
        }
    }

    fun nextStep() {
        val tool = _uiState.value.selectedTool ?: return
        if (_uiState.value.stepIndex < tool.steps.lastIndex) {
            _uiState.update { it.copy(stepIndex = it.stepIndex + 1) }
        }
    }

    fun previousStep() {
        if (_uiState.value.stepIndex > 0) {
            _uiState.update { it.copy(stepIndex = it.stepIndex - 1) }
        }
    }

    fun updateThoughtDump(text: String) {
        _uiState.update { it.copy(thoughtDumpText = text) }
    }

    fun updateAnxietyLog(text: String) {
        _uiState.update { it.copy(anxietyLogText = text) }
    }

    fun updateFutureSelfText(text: String) {
        _uiState.update { it.copy(futureSelfText = text) }
    }

    fun updateFutureSelfScheduledAt(millis: Long) {
        _uiState.update { it.copy(futureSelfScheduledAtMillis = millis) }
    }

    fun updateRefactoringInterpretation(text: String) {
        _uiState.update { it.copy(refactoringInterpretation = text) }
    }

    fun updateRefactoringActualFacts(text: String) {
        _uiState.update { it.copy(refactoringActualFacts = text) }
    }

    fun updateRefactoringExplanation1(text: String) {
        _uiState.update { it.copy(refactoringExplanation1 = text) }
    }

    fun updateRefactoringExplanation2(text: String) {
        _uiState.update { it.copy(refactoringExplanation2 = text) }
    }

    fun updateRefactoringExplanation3(text: String) {
        _uiState.update { it.copy(refactoringExplanation3 = text) }
    }

    fun appendToRefactoringField(target: RefactoringSpeechTarget, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            when (target) {
                RefactoringSpeechTarget.Interpretation -> {
                    val current = state.refactoringInterpretation
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(refactoringInterpretation = current + separator + trimmed)
                }
                RefactoringSpeechTarget.ActualFacts -> {
                    val current = state.refactoringActualFacts
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(refactoringActualFacts = current + separator + trimmed)
                }
                RefactoringSpeechTarget.Explanation1 -> {
                    val current = state.refactoringExplanation1
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(refactoringExplanation1 = current + separator + trimmed)
                }
                RefactoringSpeechTarget.Explanation2 -> {
                    val current = state.refactoringExplanation2
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(refactoringExplanation2 = current + separator + trimmed)
                }
                RefactoringSpeechTarget.Explanation3 -> {
                    val current = state.refactoringExplanation3
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(refactoringExplanation3 = current + separator + trimmed)
                }
            }
        }
    }

    fun nextRefactoringStep() {
        commitPendingRefactoringAudio()
        if (_uiState.value.refactoringStepIndex < 2) {
            _uiState.update { it.copy(refactoringStepIndex = it.refactoringStepIndex + 1) }
        }
    }

    fun previousRefactoringStep() {
        commitPendingRefactoringAudio()
        if (_uiState.value.refactoringStepIndex > 0) {
            _uiState.update { it.copy(refactoringStepIndex = it.refactoringStepIndex - 1) }
        }
    }

    fun goToRefactoringStep(index: Int) {
        commitPendingRefactoringAudio()
        _uiState.update { it.copy(refactoringStepIndex = index.coerceIn(0, 2)) }
    }

    private fun commitPendingRefactoringAudio() {
        val state = _uiState.value
        val pending = state.pendingAudioPath ?: return
        _uiState.update {
            when (state.refactoringStepIndex) {
                0 -> it.copy(
                    refactoringActualFactsAudio = pending,
                    pendingAudioPath = null,
                )
                1 -> it.copy(
                    refactoringInterpretationAudio = pending,
                    pendingAudioPath = null,
                )
                else -> {
                    when {
                        state.refactoringExplanation1Audio == null -> it.copy(
                            refactoringExplanation1Audio = pending,
                            pendingAudioPath = null,
                        )
                        state.refactoringExplanation2Audio == null -> it.copy(
                            refactoringExplanation2Audio = pending,
                            pendingAudioPath = null,
                        )
                        else -> it.copy(
                            refactoringExplanation3Audio = pending,
                            pendingAudioPath = null,
                        )
                    }
                }
            }
        }
    }

    fun saveRefactoringEntry() {
        viewModelScope.launch {
            commitPendingRefactoringAudio()
            val state = _uiState.value
            val entryId = refactoringRepository.save(
                RefactoringEntryEntity(
                    interpretation = state.refactoringInterpretation.trim(),
                    interpretationAudioPath = state.refactoringInterpretationAudio,
                    actualFacts = state.refactoringActualFacts.trim(),
                    actualFactsAudioPath = state.refactoringActualFactsAudio,
                    explanation1 = state.refactoringExplanation1.trim(),
                    explanation1AudioPath = state.refactoringExplanation1Audio,
                    explanation2 = state.refactoringExplanation2.trim(),
                    explanation2AudioPath = state.refactoringExplanation2Audio,
                    explanation3 = state.refactoringExplanation3.trim(),
                    explanation3AudioPath = state.refactoringExplanation3Audio,
                ),
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.REFACTORING, it) }
            _uiState.update {
                it.copy(
                    refactoringStepIndex = 0,
                    refactoringInterpretation = "",
                    refactoringActualFacts = "",
                    refactoringExplanation1 = "",
                    refactoringExplanation2 = "",
                    refactoringExplanation3 = "",
                    refactoringInterpretationAudio = null,
                    refactoringActualFactsAudio = null,
                    refactoringExplanation1Audio = null,
                    refactoringExplanation2Audio = null,
                    refactoringExplanation3Audio = null,
                    pendingAudioPath = null,
                )
            }
        }
    }

    fun clearRefactoringDraft() {
        _uiState.update {
            it.copy(
                refactoringStepIndex = 0,
                refactoringInterpretation = "",
                refactoringActualFacts = "",
                refactoringExplanation1 = "",
                refactoringExplanation2 = "",
                refactoringExplanation3 = "",
                refactoringInterpretationAudio = null,
                refactoringActualFactsAudio = null,
                refactoringExplanation1Audio = null,
                refactoringExplanation2Audio = null,
                refactoringExplanation3Audio = null,
                pendingAudioPath = null,
            )
        }
    }

    fun openRefactoringEntry(entry: RefactoringEntryEntity) {
        _uiState.update { it.copy(openedRefactoringEntry = entry) }
    }

    fun closeRefactoringEntry() {
        _uiState.update { it.copy(openedRefactoringEntry = null) }
    }

    fun deleteRefactoringEntry(entry: RefactoringEntryEntity) {
        viewModelScope.launch {
            refactoringRepository.deleteEntry(entry.id)
            if (_uiState.value.openedRefactoringEntry?.id == entry.id) {
                _uiState.update { it.copy(openedRefactoringEntry = null) }
            }
        }
    }

    fun updateCenterOfGravityThoughtsAndFeelings(text: String) {
        _uiState.update { it.copy(centerOfGravityThoughtsAndFeelings = text) }
    }

    fun updateCenterOfGravityBodyAndNeeds(text: String) {
        _uiState.update { it.copy(centerOfGravityBodyAndNeeds = text) }
    }

    fun appendToCenterOfGravityField(target: CenterOfGravitySpeechTarget, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            when (target) {
                CenterOfGravitySpeechTarget.ThoughtsAndFeelings -> {
                    val current = state.centerOfGravityThoughtsAndFeelings
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(centerOfGravityThoughtsAndFeelings = current + separator + trimmed)
                }
                CenterOfGravitySpeechTarget.BodyAndNeeds -> {
                    val current = state.centerOfGravityBodyAndNeeds
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(centerOfGravityBodyAndNeeds = current + separator + trimmed)
                }
            }
        }
    }

    fun nextCenterOfGravityStep() {
        commitPendingCenterOfGravityAudio()
        if (_uiState.value.centerOfGravityStepIndex < 1) {
            _uiState.update { it.copy(centerOfGravityStepIndex = it.centerOfGravityStepIndex + 1) }
        }
    }

    fun previousCenterOfGravityStep() {
        commitPendingCenterOfGravityAudio()
        if (_uiState.value.centerOfGravityStepIndex > 0) {
            _uiState.update { it.copy(centerOfGravityStepIndex = it.centerOfGravityStepIndex - 1) }
        }
    }

    private fun commitPendingCenterOfGravityAudio() {
        val state = _uiState.value
        val pending = state.pendingAudioPath ?: return
        _uiState.update {
            when (state.centerOfGravityStepIndex) {
                0 -> it.copy(
                    centerOfGravityThoughtsAndFeelingsAudio = pending,
                    pendingAudioPath = null,
                )
                else -> it.copy(
                    centerOfGravityBodyAndNeedsAudio = pending,
                    pendingAudioPath = null,
                )
            }
        }
    }

    fun saveCenterOfGravityEntry() {
        viewModelScope.launch {
            commitPendingCenterOfGravityAudio()
            val state = _uiState.value
            val entryId = centerOfGravityRepository.save(
                CenterOfGravityEntryEntity(
                    thoughtsAndFeelings = state.centerOfGravityThoughtsAndFeelings.trim(),
                    thoughtsAndFeelingsAudioPath = state.centerOfGravityThoughtsAndFeelingsAudio,
                    bodyAndNeeds = state.centerOfGravityBodyAndNeeds.trim(),
                    bodyAndNeedsAudioPath = state.centerOfGravityBodyAndNeedsAudio,
                ),
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.CENTER_OF_GRAVITY, it) }
            _uiState.update {
                it.copy(
                    centerOfGravityStepIndex = 0,
                    centerOfGravityThoughtsAndFeelings = "",
                    centerOfGravityBodyAndNeeds = "",
                    centerOfGravityThoughtsAndFeelingsAudio = null,
                    centerOfGravityBodyAndNeedsAudio = null,
                    pendingAudioPath = null,
                )
            }
        }
    }

    fun clearCenterOfGravityDraft() {
        _uiState.update {
            it.copy(
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
                centerOfGravityThoughtsAndFeelingsAudio = null,
                centerOfGravityBodyAndNeedsAudio = null,
                pendingAudioPath = null,
            )
        }
    }

    fun openCenterOfGravityEntry(entry: CenterOfGravityEntryEntity) {
        _uiState.update { it.copy(openedCenterOfGravityEntry = entry) }
    }

    fun closeCenterOfGravityEntry() {
        _uiState.update { it.copy(openedCenterOfGravityEntry = null) }
    }

    fun deleteCenterOfGravityEntry(entry: CenterOfGravityEntryEntity) {
        viewModelScope.launch {
            centerOfGravityRepository.deleteEntry(entry.id)
            if (_uiState.value.openedCenterOfGravityEntry?.id == entry.id) {
                _uiState.update { it.copy(openedCenterOfGravityEntry = null) }
            }
        }
    }

    fun updateNvcObservation(text: String) {
        _uiState.update { it.copy(nvcObservation = text) }
    }

    fun updateNvcFeeling(text: String) {
        _uiState.update { it.copy(nvcFeeling = text) }
    }

    fun updateNvcNeed(text: String) {
        _uiState.update { it.copy(nvcNeed = text) }
    }

    fun updateNvcRequest(text: String) {
        _uiState.update { it.copy(nvcRequest = text) }
    }

    fun appendToNvcField(target: NvcSpeechTarget, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            when (target) {
                NvcSpeechTarget.Observation -> {
                    val current = state.nvcObservation
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(nvcObservation = current + separator + trimmed)
                }
                NvcSpeechTarget.Feeling -> {
                    val current = state.nvcFeeling
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(nvcFeeling = current + separator + trimmed)
                }
                NvcSpeechTarget.Need -> {
                    val current = state.nvcNeed
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(nvcNeed = current + separator + trimmed)
                }
                NvcSpeechTarget.Request -> {
                    val current = state.nvcRequest
                    val separator = if (current.isBlank()) "" else " "
                    state.copy(nvcRequest = current + separator + trimmed)
                }
            }
        }
    }

    fun nextNvcStep() {
        commitPendingNvcAudio()
        if (_uiState.value.nvcStepIndex < 3) {
            _uiState.update { it.copy(nvcStepIndex = it.nvcStepIndex + 1) }
        }
    }

    fun previousNvcStep() {
        commitPendingNvcAudio()
        if (_uiState.value.nvcStepIndex > 0) {
            _uiState.update { it.copy(nvcStepIndex = it.nvcStepIndex - 1) }
        }
    }

    fun goToNvcStep(index: Int) {
        commitPendingNvcAudio()
        _uiState.update { it.copy(nvcStepIndex = index.coerceIn(0, 3)) }
    }

    private fun commitPendingNvcAudio() {
        val state = _uiState.value
        val pending = state.pendingAudioPath ?: return
        _uiState.update {
            when (state.nvcStepIndex) {
                0 -> it.copy(nvcObservationAudio = pending, pendingAudioPath = null)
                1 -> it.copy(nvcFeelingAudio = pending, pendingAudioPath = null)
                2 -> it.copy(nvcNeedAudio = pending, pendingAudioPath = null)
                else -> it.copy(nvcRequestAudio = pending, pendingAudioPath = null)
            }
        }
    }

    fun saveNvcEntry() {
        viewModelScope.launch {
            commitPendingNvcAudio()
            val state = _uiState.value
            val entryId = nvcRepository.save(
                NvcEntryEntity(
                    observation = state.nvcObservation.trim(),
                    observationAudioPath = state.nvcObservationAudio,
                    feeling = state.nvcFeeling.trim(),
                    feelingAudioPath = state.nvcFeelingAudio,
                    need = state.nvcNeed.trim(),
                    needAudioPath = state.nvcNeedAudio,
                    request = state.nvcRequest.trim(),
                    requestAudioPath = state.nvcRequestAudio,
                ),
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.NVC, it) }
            _uiState.update {
                it.copy(
                    nvcStepIndex = 0,
                    nvcObservation = "",
                    nvcFeeling = "",
                    nvcNeed = "",
                    nvcRequest = "",
                    nvcObservationAudio = null,
                    nvcFeelingAudio = null,
                    nvcNeedAudio = null,
                    nvcRequestAudio = null,
                    pendingAudioPath = null,
                )
            }
        }
    }

    fun clearNvcDraft() {
        _uiState.update {
            it.copy(
                nvcStepIndex = 0,
                nvcObservation = "",
                nvcFeeling = "",
                nvcNeed = "",
                nvcRequest = "",
                nvcObservationAudio = null,
                nvcFeelingAudio = null,
                nvcNeedAudio = null,
                nvcRequestAudio = null,
                pendingAudioPath = null,
            )
        }
    }

    fun openNvcEntry(entry: NvcEntryEntity) {
        _uiState.update { it.copy(openedNvcEntry = entry) }
    }

    fun closeNvcEntry() {
        _uiState.update { it.copy(openedNvcEntry = null) }
    }

    fun deleteNvcEntry(entry: NvcEntryEntity) {
        viewModelScope.launch {
            nvcRepository.deleteEntry(entry.id)
            if (_uiState.value.openedNvcEntry?.id == entry.id) {
                _uiState.update { it.copy(openedNvcEntry = null) }
            }
        }
    }

    fun setPendingAudioPath(path: String?) {
        _uiState.update { it.copy(pendingAudioPath = path) }
    }

    fun appendToActiveLog(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        when (_uiState.value.selectedTool?.id) {
            ToolkitToolId.ThoughtDump -> {
                val current = _uiState.value.thoughtDumpText
                val separator = if (current.isBlank()) "" else " "
                _uiState.update { it.copy(thoughtDumpText = current + separator + trimmed) }
            }
            ToolkitToolId.AnxietyLog -> {
                val current = _uiState.value.anxietyLogText
                val separator = if (current.isBlank()) "" else " "
                _uiState.update { it.copy(anxietyLogText = current + separator + trimmed) }
            }
            ToolkitToolId.FutureSelfMessage -> {
                val current = _uiState.value.futureSelfText
                val separator = if (current.isBlank()) "" else " "
                _uiState.update { it.copy(futureSelfText = current + separator + trimmed) }
            }
            else -> Unit
        }
    }

    fun saveThoughtDump() {
        viewModelScope.launch {
            val entryId = logRepository.save(
                type = ToolkitLogType.THOUGHT_DUMP,
                content = _uiState.value.thoughtDumpText,
                audioPath = _uiState.value.pendingAudioPath,
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.THOUGHT_DUMP, it) }
            _uiState.update { it.copy(thoughtDumpText = "", pendingAudioPath = null) }
        }
    }

    fun saveAnxietyLog() {
        viewModelScope.launch {
            val entryId = logRepository.save(
                type = ToolkitLogType.ANXIETY_LOG,
                content = _uiState.value.anxietyLogText,
                audioPath = _uiState.value.pendingAudioPath,
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.ANXIETY_LOG, it) }
            _uiState.update { it.copy(anxietyLogText = "", pendingAudioPath = null) }
        }
    }

    fun saveFutureSelfMessage() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.futureSelfScheduledAtMillis <= System.currentTimeMillis()) return@launch
            val editingId = state.editingFutureSelfId
            if (editingId != null) {
                FutureSelfMessageScheduler.cancel(appContext, editingId)
            }
            val savedId = futureSelfRepository.save(
                id = editingId,
                content = state.futureSelfText,
                audioPath = state.pendingAudioPath,
                scheduledAtMillis = state.futureSelfScheduledAtMillis,
            ) ?: return@launch
            FutureSelfMessageScheduler.schedule(
                appContext,
                savedId,
                state.futureSelfScheduledAtMillis,
            )
            enqueueOneNoteSync(OneNoteEntryType.FUTURE_SELF, savedId)
            closeTool()
        }
    }

    fun clearActiveDraft() {
        _uiState.update {
            it.copy(
                thoughtDumpText = "",
                anxietyLogText = "",
                futureSelfText = "",
                pendingAudioPath = null,
                futureSelfScheduledAtMillis = defaultFutureSelfScheduleTime(),
                editingFutureSelfId = null,
                refactoringStepIndex = 0,
                refactoringInterpretation = "",
                refactoringActualFacts = "",
                refactoringExplanation1 = "",
                refactoringExplanation2 = "",
                refactoringExplanation3 = "",
                refactoringInterpretationAudio = null,
                refactoringActualFactsAudio = null,
                refactoringExplanation1Audio = null,
                refactoringExplanation2Audio = null,
                refactoringExplanation3Audio = null,
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
                centerOfGravityThoughtsAndFeelingsAudio = null,
                centerOfGravityBodyAndNeedsAudio = null,
                nvcStepIndex = 0,
                nvcObservation = "",
                nvcFeeling = "",
                nvcNeed = "",
                nvcRequest = "",
                nvcObservationAudio = null,
                nvcFeelingAudio = null,
                nvcNeedAudio = null,
                nvcRequestAudio = null,
            )
        }
    }

    fun openLogEntry(entry: ThoughtDumpEntity) {
        _uiState.update { it.copy(openedLogEntry = entry) }
    }

    fun closeLogEntry() {
        _uiState.update { it.copy(openedLogEntry = null) }
    }

    fun deleteLogEntry(entry: ThoughtDumpEntity) {
        viewModelScope.launch {
            logRepository.deleteEntry(entry.id)
            if (_uiState.value.openedLogEntry?.id == entry.id) {
                _uiState.update { it.copy(openedLogEntry = null) }
            }
        }
    }

    fun openFutureSelfEntry(entry: FutureSelfMessageEntity) {
        _uiState.update { it.copy(openedFutureSelfEntry = entry) }
    }

    fun closeFutureSelfEntry() {
        _uiState.update { it.copy(openedFutureSelfEntry = null) }
    }

    fun editFutureSelfEntry(entry: FutureSelfMessageEntity) {
        _uiState.update {
            it.copy(
                futureSelfText = entry.content,
                pendingAudioPath = entry.audioPath,
                futureSelfScheduledAtMillis = entry.scheduledAtMillis,
                editingFutureSelfId = entry.id,
                openedFutureSelfEntry = null,
            )
        }
    }

    fun deleteFutureSelfEntry(entry: FutureSelfMessageEntity) {
        viewModelScope.launch {
            FutureSelfMessageScheduler.cancel(appContext, entry.id)
            futureSelfRepository.delete(entry.id)
            if (_uiState.value.openedFutureSelfEntry?.id == entry.id) {
                _uiState.update { it.copy(openedFutureSelfEntry = null) }
            }
            if (_uiState.value.editingFutureSelfId == entry.id) {
                _uiState.update {
                    it.copy(
                        editingFutureSelfId = null,
                        futureSelfText = "",
                        pendingAudioPath = null,
                        futureSelfScheduledAtMillis = defaultFutureSelfScheduleTime(),
                    )
                }
            }
        }
    }

    fun pickRandomTool() {
        viewModelScope.launch {
            _uiState.update { it.copy(randomToolState = RandomToolState.Finding, randomSelectedTool = null) }
            delay(800)
            val tool = ToolkitLayout.randomReactive(_uiState.value.enabledToolIds)
            _uiState.update {
                it.copy(randomToolState = RandomToolState.Selected, randomSelectedTool = tool)
            }
            delay(1_500)
            tool?.let { openTool(it) }
            _uiState.update {
                it.copy(randomToolState = RandomToolState.Idle, randomSelectedTool = null)
            }
        }
    }

    fun logTypeForTool(toolId: ToolkitToolId?): ToolkitLogType? = when (toolId) {
        ToolkitToolId.ThoughtDump -> ToolkitLogType.THOUGHT_DUMP
        ToolkitToolId.AnxietyLog -> ToolkitLogType.ANXIETY_LOG
        else -> null
    }

    fun isLogTool(tool: ToolkitTool?): Boolean =
        tool?.id == ToolkitToolId.ThoughtDump ||
            tool?.id == ToolkitToolId.AnxietyLog ||
            tool?.id == ToolkitToolId.FutureSelfMessage ||
            tool?.id == ToolkitToolId.Refactoring ||
            tool?.id == ToolkitToolId.NonViolentCommunication ||
            tool?.id == ToolkitToolId.RelocateCenterOfGravity

    private fun enqueueOneNoteSync(entryType: OneNoteEntryType, localEntryId: Long) {
        viewModelScope.launch {
            oneNoteSync.enqueueSync(entryType, localEntryId)
        }
    }
}
