package com.example.meditationparticles.ui.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.HeartsEntryEntity
import com.example.meditationparticles.data.local.NvcEntryEntity
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.toolkit.HeartsToolConfig
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitLane
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import com.example.meditationparticles.domain.toolkit.ToolkitTool
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.reminder.FutureSelfMessageScheduler
import com.example.meditationparticles.domain.livingtree.LivingTreeDefaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RandomToolState { Idle, Finding, Selected }

data class ToolkitUiState(
    val toolkitConfigured: Boolean = true,
    val enabledToolIds: Set<ToolkitToolId> = ToolkitLayout.defaultEnabledTools(),
    val proactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Core),
    val reactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Core),
    val heartsProactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Hearts),
    val heartsReactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Hearts),
    val selectionProactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Core) +
        ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Hearts),
    val selectionReactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Core) +
        ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Hearts),
    val selectedTool: ToolkitTool? = null,
    val stepIndex: Int = 0,
    val thoughtDumpText: String = "",
    val draftMoodLevel: Int? = null,
    val anxietyLogText: String = "",
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
    val refactoringEntries: List<RefactoringEntryEntity> = emptyList(),
    val openedRefactoringEntry: RefactoringEntryEntity? = null,
    val centerOfGravityStepIndex: Int = 0,
    val centerOfGravityThoughtsAndFeelings: String = "",
    val centerOfGravityBodyAndNeeds: String = "",
    val centerOfGravityEntries: List<CenterOfGravityEntryEntity> = emptyList(),
    val openedCenterOfGravityEntry: CenterOfGravityEntryEntity? = null,
    val nvcStepIndex: Int = 0,
    val nvcObservation: String = "",
    val nvcFeeling: String = "",
    val nvcNeed: String = "",
    val nvcRequest: String = "",
    val nvcEntries: List<NvcEntryEntity> = emptyList(),
    val openedNvcEntry: NvcEntryEntity? = null,
    val heartsStepIndex: Int = 0,
    val heartsSteps: List<String> = List(5) { "" },
    val heartsPersonId: Long? = null,
    val heartsPersonName: String = "",
    val heartsEntries: List<HeartsEntryEntity> = emptyList(),
    val openedHeartsEntry: HeartsEntryEntity? = null,
    val heartsPartnerSummaries: List<HeartsPartnerSummary> = emptyList(),
    val randomToolState: RandomToolState = RandomToolState.Idle,
    val randomSelectedTool: ToolkitTool? = null,
    val oneNoteConnected: Boolean = false,
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
    private val heartsRepository = AppGraph.heartsEntries(application)
    private val livingTreeRepository = AppGraph.livingTree(application)
    private val toolkitPreferences = AppGraph.toolkit(application)
    private val settingsPreferences = AppGraph.settings(application)
    private val oneNotePreferences by lazy { AppGraph.oneNotePreferences(application) }
    private val oneNoteSync by lazy { AppGraph.oneNoteSync(application) }
    private val appContext = application.applicationContext
    private var heartsEntriesJob: Job? = null
    private var heartsPartnersJob: Job? = null

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
            logRepository.observeEntries(ToolkitLogType.THOUGHT_DUMP)
                .catch { }
                .collect { entries ->
                _uiState.update { it.copy(thoughtDumpEntries = entries) }
            }
        }
        viewModelScope.launch {
            logRepository.observeEntries(ToolkitLogType.ANXIETY_LOG)
                .catch { }
                .collect { entries ->
                _uiState.update { it.copy(anxietyLogEntries = entries) }
            }
        }
        viewModelScope.launch {
            futureSelfRepository.observeAll()
                .catch { }
                .collect { entries ->
                _uiState.update { it.copy(futureSelfEntries = entries) }
            }
        }
        viewModelScope.launch {
            refactoringRepository.observeAll()
                .catch { }
                .collect { entries ->
                _uiState.update { it.copy(refactoringEntries = entries) }
            }
        }
        viewModelScope.launch {
            centerOfGravityRepository.observeAll()
                .catch { }
                .collect { entries ->
                _uiState.update { it.copy(centerOfGravityEntries = entries) }
            }
        }
        viewModelScope.launch {
            nvcRepository.observeAll()
                .catch { }
                .collect { entries ->
                _uiState.update { it.copy(nvcEntries = entries) }
            }
        }
        viewModelScope.launch {
            oneNotePreferences.snapshot
                .catch { }
                .collect { prefs ->
                _uiState.update {
                    it.copy(oneNoteConnected = !prefs.accountEmail.isNullOrBlank())
                }
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

    fun reorderHeartsProactiveTool(fromIndex: Int, toIndex: Int) {
        val currentOrder = _uiState.value.heartsProactiveTools.map { it.id }
        val reordered = ToolkitLayout.reorder(currentOrder, fromIndex, toIndex)
        toolkitPreferences.saveHeartsProactiveOrder(reordered)
    }

    fun reorderHeartsReactiveTool(fromIndex: Int, toIndex: Int) {
        val currentOrder = _uiState.value.heartsReactiveTools.map { it.id }
        val reordered = ToolkitLayout.reorder(currentOrder, fromIndex, toIndex)
        toolkitPreferences.saveHeartsReactiveOrder(reordered)
    }

    private fun applyToolkitSnapshot(snapshot: com.example.meditationparticles.data.ToolkitPrefsSnapshot) {
        _uiState.update { state ->
            state.copy(
                toolkitConfigured = snapshot.configured,
                enabledToolIds = snapshot.enabledToolIds,
                proactiveTools = ToolkitLayout.orderedTools(
                    category = ToolkitCategory.Proactive,
                    lane = ToolkitLane.Core,
                    enabledIds = snapshot.enabledToolIds,
                    savedOrder = snapshot.proactiveOrder,
                    usageCounts = snapshot.usageCounts,
                ),
                reactiveTools = ToolkitLayout.orderedTools(
                    category = ToolkitCategory.Reactive,
                    lane = ToolkitLane.Core,
                    enabledIds = snapshot.enabledToolIds,
                    savedOrder = snapshot.reactiveOrder,
                    usageCounts = snapshot.usageCounts,
                ),
                heartsProactiveTools = ToolkitLayout.orderedTools(
                    category = ToolkitCategory.Proactive,
                    lane = ToolkitLane.Hearts,
                    enabledIds = snapshot.enabledToolIds,
                    savedOrder = snapshot.heartsProactiveOrder,
                    usageCounts = snapshot.usageCounts,
                ),
                heartsReactiveTools = ToolkitLayout.orderedTools(
                    category = ToolkitCategory.Reactive,
                    lane = ToolkitLane.Hearts,
                    enabledIds = snapshot.enabledToolIds,
                    savedOrder = snapshot.heartsReactiveOrder,
                    usageCounts = snapshot.usageCounts,
                ),
                selectionProactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Core) +
                    ToolkitCatalog.byCategory(ToolkitCategory.Proactive, ToolkitLane.Hearts),
                selectionReactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Core) +
                    ToolkitCatalog.byCategory(ToolkitCategory.Reactive, ToolkitLane.Hearts),
            )
        }
    }

    fun openTool(tool: ToolkitTool) {
        toolkitPreferences.incrementUsageCount(tool.id)
        heartsEntriesJob?.cancel()
        heartsPartnersJob?.cancel()
        _uiState.update {
            it.copy(
                selectedTool = tool,
                stepIndex = 0,
                randomToolState = RandomToolState.Idle,
                thoughtDumpText = "",
                draftMoodLevel = null,
                anxietyLogText = "",
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
                openedRefactoringEntry = null,
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
                openedCenterOfGravityEntry = null,
                nvcStepIndex = 0,
                nvcObservation = "",
                nvcFeeling = "",
                nvcNeed = "",
                nvcRequest = "",
                openedNvcEntry = null,
                heartsStepIndex = 0,
                heartsSteps = List(5) { "" },
                heartsPersonId = null,
                heartsPersonName = "",
                heartsEntries = emptyList(),
                openedHeartsEntry = null,
            )
        }
        if (HeartsToolConfig.isJournalTool(tool.id)) {
            heartsEntriesJob = viewModelScope.launch {
                heartsRepository.observeByTool(tool.id).collect { entries ->
                    _uiState.update { state -> state.copy(heartsEntries = entries) }
                }
            }
        }
        if (tool.id == ToolkitToolId.HeartsFlowerPartners) {
            heartsPartnersJob = viewModelScope.launch {
                combine(
                    livingTreeRepository.snapshot,
                    heartsRepository.observeAll(),
                ) { tree, entries ->
                    buildPartnerSummaries(tree.people, tree.tagById, entries)
                }.collect { partners ->
                    _uiState.update { state -> state.copy(heartsPartnerSummaries = partners) }
                }
            }
        }
    }

    fun openHeartsToolForPartner(toolId: ToolkitToolId, partner: HeartsPartnerSummary) {
        ToolkitCatalog.byId(toolId)?.let { tool ->
            openTool(tool)
            _uiState.update {
                it.copy(
                    heartsPersonId = partner.personId,
                    heartsPersonName = partner.personName,
                )
            }
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
            null -> Unit
            else -> ToolkitCatalog.byId(toolId)?.let(::openTool)
        }
    }

    fun closeTool() {
        heartsEntriesJob?.cancel()
        heartsPartnersJob?.cancel()
        _uiState.update {
            it.copy(
                selectedTool = null,
                stepIndex = 0,
                thoughtDumpText = "",
                draftMoodLevel = null,
                anxietyLogText = "",
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
                openedRefactoringEntry = null,
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
                openedCenterOfGravityEntry = null,
                nvcStepIndex = 0,
                nvcObservation = "",
                nvcFeeling = "",
                nvcNeed = "",
                nvcRequest = "",
                openedNvcEntry = null,
                heartsStepIndex = 0,
                heartsSteps = List(5) { "" },
                heartsPersonId = null,
                heartsPersonName = "",
                heartsEntries = emptyList(),
                openedHeartsEntry = null,
                heartsPartnerSummaries = emptyList(),
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

    fun updateDraftMoodLevel(level: Int?) {
        _uiState.update { it.copy(draftMoodLevel = MoodScale.normalize(level)) }
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

    fun nextRefactoringStep() {
        if (_uiState.value.refactoringStepIndex < 2) {
            _uiState.update { it.copy(refactoringStepIndex = it.refactoringStepIndex + 1) }
        }
    }

    fun previousRefactoringStep() {
        if (_uiState.value.refactoringStepIndex > 0) {
            _uiState.update { it.copy(refactoringStepIndex = it.refactoringStepIndex - 1) }
        }
    }

    fun goToRefactoringStep(index: Int) {
        _uiState.update { it.copy(refactoringStepIndex = index.coerceIn(0, 2)) }
    }

    fun saveRefactoringEntry(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val entryId = refactoringRepository.save(
                RefactoringEntryEntity(
                    interpretation = state.refactoringInterpretation.trim(),
                    actualFacts = state.refactoringActualFacts.trim(),
                    explanation1 = state.refactoringExplanation1.trim(),
                    explanation2 = state.refactoringExplanation2.trim(),
                    explanation3 = state.refactoringExplanation3.trim(),
                    moodLevel = state.draftMoodLevel,
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
                )
            }
            onComplete()
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
            oneNoteSync.deleteForEntry(OneNoteEntryType.REFACTORING, entry.id)
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

    fun nextCenterOfGravityStep() {
        if (_uiState.value.centerOfGravityStepIndex < 1) {
            _uiState.update { it.copy(centerOfGravityStepIndex = it.centerOfGravityStepIndex + 1) }
        }
    }

    fun previousCenterOfGravityStep() {
        if (_uiState.value.centerOfGravityStepIndex > 0) {
            _uiState.update { it.copy(centerOfGravityStepIndex = it.centerOfGravityStepIndex - 1) }
        }
    }

    fun saveCenterOfGravityEntry(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val entryId = centerOfGravityRepository.save(
                CenterOfGravityEntryEntity(
                    thoughtsAndFeelings = state.centerOfGravityThoughtsAndFeelings.trim(),
                    bodyAndNeeds = state.centerOfGravityBodyAndNeeds.trim(),
                    moodLevel = state.draftMoodLevel,
                ),
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.CENTER_OF_GRAVITY, it) }
            _uiState.update {
                it.copy(
                    centerOfGravityStepIndex = 0,
                    centerOfGravityThoughtsAndFeelings = "",
                    centerOfGravityBodyAndNeeds = "",
                )
            }
            onComplete()
        }
    }

    fun clearCenterOfGravityDraft() {
        _uiState.update {
            it.copy(
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
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
            oneNoteSync.deleteForEntry(OneNoteEntryType.CENTER_OF_GRAVITY, entry.id)
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

    fun nextNvcStep() {
        if (_uiState.value.nvcStepIndex < 3) {
            _uiState.update { it.copy(nvcStepIndex = it.nvcStepIndex + 1) }
        }
    }

    fun previousNvcStep() {
        if (_uiState.value.nvcStepIndex > 0) {
            _uiState.update { it.copy(nvcStepIndex = it.nvcStepIndex - 1) }
        }
    }

    fun goToNvcStep(index: Int) {
        _uiState.update { it.copy(nvcStepIndex = index.coerceIn(0, 3)) }
    }

    fun saveNvcEntry(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val entryId = nvcRepository.save(
                NvcEntryEntity(
                    observation = state.nvcObservation.trim(),
                    feeling = state.nvcFeeling.trim(),
                    need = state.nvcNeed.trim(),
                    request = state.nvcRequest.trim(),
                    moodLevel = state.draftMoodLevel,
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
                )
            }
            onComplete()
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
            oneNoteSync.deleteForEntry(OneNoteEntryType.NVC, entry.id)
            nvcRepository.deleteEntry(entry.id)
            if (_uiState.value.openedNvcEntry?.id == entry.id) {
                _uiState.update { it.copy(openedNvcEntry = null) }
            }
        }
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

    fun saveThoughtDump(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val entryId = logRepository.save(
                type = ToolkitLogType.THOUGHT_DUMP,
                content = _uiState.value.thoughtDumpText,
                moodLevel = _uiState.value.draftMoodLevel,
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.THOUGHT_DUMP, it) }
            _uiState.update { it.copy(thoughtDumpText = "", draftMoodLevel = null) }
            onComplete()
        }
    }

    fun saveAnxietyLog(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val entryId = logRepository.save(
                type = ToolkitLogType.ANXIETY_LOG,
                content = _uiState.value.anxietyLogText,
                moodLevel = _uiState.value.draftMoodLevel,
            )
            entryId?.let { enqueueOneNoteSync(OneNoteEntryType.ANXIETY_LOG, it) }
            _uiState.update { it.copy(anxietyLogText = "", draftMoodLevel = null) }
            onComplete()
        }
    }

    fun saveFutureSelfMessage(onComplete: () -> Unit = {}) {
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
                scheduledAtMillis = state.futureSelfScheduledAtMillis,
                moodLevel = state.draftMoodLevel,
            ) ?: return@launch
            FutureSelfMessageScheduler.schedule(
                appContext,
                savedId,
                state.futureSelfScheduledAtMillis,
            )
            enqueueOneNoteSync(OneNoteEntryType.FUTURE_SELF, savedId)
            closeTool()
            onComplete()
        }
    }

    fun clearActiveDraft() {
        _uiState.update {
            it.copy(
                thoughtDumpText = "",
                draftMoodLevel = null,
                anxietyLogText = "",
                futureSelfText = "",
                futureSelfScheduledAtMillis = defaultFutureSelfScheduleTime(),
                editingFutureSelfId = null,
                refactoringStepIndex = 0,
                refactoringInterpretation = "",
                refactoringActualFacts = "",
                refactoringExplanation1 = "",
                refactoringExplanation2 = "",
                refactoringExplanation3 = "",
                centerOfGravityStepIndex = 0,
                centerOfGravityThoughtsAndFeelings = "",
                centerOfGravityBodyAndNeeds = "",
                nvcStepIndex = 0,
                nvcObservation = "",
                nvcFeeling = "",
                nvcNeed = "",
                nvcRequest = "",
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
            val entryType = when (entry.logType) {
                ToolkitLogType.ANXIETY_LOG.name -> OneNoteEntryType.ANXIETY_LOG
                else -> OneNoteEntryType.THOUGHT_DUMP
            }
            oneNoteSync.deleteForEntry(entryType, entry.id)
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
                draftMoodLevel = entry.moodLevel,
                futureSelfScheduledAtMillis = entry.scheduledAtMillis,
                editingFutureSelfId = entry.id,
                openedFutureSelfEntry = null,
            )
        }
    }

    fun deleteFutureSelfEntry(entry: FutureSelfMessageEntity) {
        viewModelScope.launch {
            FutureSelfMessageScheduler.cancel(appContext, entry.id)
            oneNoteSync.deleteForEntry(OneNoteEntryType.FUTURE_SELF, entry.id)
            futureSelfRepository.delete(entry.id)
            if (_uiState.value.openedFutureSelfEntry?.id == entry.id) {
                _uiState.update { it.copy(openedFutureSelfEntry = null) }
            }
            if (_uiState.value.editingFutureSelfId == entry.id) {
                _uiState.update {
                    it.copy(
                        editingFutureSelfId = null,
                        futureSelfText = "",
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
            tool?.id == ToolkitToolId.RelocateCenterOfGravity ||
            (tool != null && HeartsToolConfig.isJournalTool(tool.id))

    fun updateHeartsPersonName(name: String) {
        _uiState.update { it.copy(heartsPersonName = name) }
    }

    fun updateHeartsStep(index: Int, value: String) {
        _uiState.update { state ->
            val next = state.heartsSteps.toMutableList()
            while (next.size < 5) next.add("")
            next[index] = value
            state.copy(heartsSteps = next)
        }
    }

    fun nextHeartsStep() {
        val tool = _uiState.value.selectedTool ?: return
        val max = HeartsToolConfig.stepCount(tool.id) - 1
        if (_uiState.value.heartsStepIndex < max) {
            _uiState.update { it.copy(heartsStepIndex = it.heartsStepIndex + 1) }
        }
    }

    fun previousHeartsStep() {
        if (_uiState.value.heartsStepIndex > 0) {
            _uiState.update { it.copy(heartsStepIndex = it.heartsStepIndex - 1) }
        }
    }

    fun goToHeartsStep(index: Int) {
        val tool = _uiState.value.selectedTool ?: return
        val max = HeartsToolConfig.stepCount(tool.id) - 1
        _uiState.update { it.copy(heartsStepIndex = index.coerceIn(0, max)) }
    }

    fun saveHeartsEntry(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val tool = state.selectedTool ?: return@launch
            if (!HeartsToolConfig.isJournalTool(tool.id)) return@launch
            heartsRepository.save(
                toolId = tool.id,
                personId = state.heartsPersonId,
                personName = state.heartsPersonName,
                steps = state.heartsSteps,
                moodLevel = state.draftMoodLevel,
            )
            _uiState.update {
                it.copy(
                    heartsStepIndex = 0,
                    heartsSteps = List(5) { "" },
                    heartsPersonName = if (tool.id == ToolkitToolId.HeartsFlowerPartners) {
                        it.heartsPersonName
                    } else {
                        ""
                    },
                    draftMoodLevel = null,
                )
            }
            onComplete()
        }
    }

    fun clearHeartsDraft() {
        _uiState.update {
            it.copy(
                heartsStepIndex = 0,
                heartsSteps = List(5) { "" },
                heartsPersonName = "",
            )
        }
    }

    fun openHeartsEntry(entry: HeartsEntryEntity) {
        _uiState.update { it.copy(openedHeartsEntry = entry) }
    }

    fun closeHeartsEntry() {
        _uiState.update { it.copy(openedHeartsEntry = null) }
    }

    fun deleteHeartsEntry(entry: HeartsEntryEntity) {
        viewModelScope.launch {
            heartsRepository.deleteEntry(entry.id)
            if (_uiState.value.openedHeartsEntry?.id == entry.id) {
                _uiState.update { it.copy(openedHeartsEntry = null) }
            }
        }
    }

    private fun buildPartnerSummaries(
        people: List<com.example.meditationparticles.data.local.LivingTreePersonWithTags>,
        tagById: Map<Long, com.example.meditationparticles.data.local.LivingTreeTagEntity>,
        entries: List<HeartsEntryEntity>,
    ): List<HeartsPartnerSummary> {
        val partnerTag = tagById.values.find { tag ->
            tag.name.equals(LivingTreeDefaults.defaultTags.first { it.name == "Partner" }.name, ignoreCase = true)
        } ?: tagById.values.find { it.name.equals("Partner", ignoreCase = true) }
            ?: return emptyList()
        return people
            .filter { person -> person.tags.any { it.id == partnerTag.id } }
            .map { personWithTags ->
                val person = personWithTags.person
                val latest = entries.filter { it.personId == person.id }.maxByOrNull { it.createdAt }
                val toolId = latest?.toolId?.let { name ->
                    runCatching { ToolkitToolId.valueOf(name) }.getOrNull()
                }
                HeartsPartnerSummary(
                    personId = person.id,
                    personName = person.name,
                    tagColorArgb = partnerTag.colorArgb,
                    lastToolTitle = toolId?.let { heartsRepository.toolTitle(it) },
                    lastHeartsLetter = toolId?.let { heartsRepository.heartsLetter(it) },
                    lastSummary = latest?.let { heartsRepository.entrySummary(it) },
                    lastTouchpointAt = latest?.createdAt,
                )
            }
            .sortedBy { it.personName.lowercase() }
    }

    fun syncEntryToOneNote(entryType: OneNoteEntryType, localEntryId: Long) {
        viewModelScope.launch {
            oneNoteSync.enqueueSync(entryType, localEntryId, manual = true)
        }
    }

    private fun enqueueOneNoteSync(entryType: OneNoteEntryType, localEntryId: Long) {
        viewModelScope.launch {
            oneNoteSync.enqueueSync(entryType, localEntryId)
        }
    }
}
