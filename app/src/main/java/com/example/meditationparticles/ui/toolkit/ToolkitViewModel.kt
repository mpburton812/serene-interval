package com.example.meditationparticles.ui.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitLayout
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import com.example.meditationparticles.domain.toolkit.ToolkitTool
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
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
    private val toolkitPreferences = AppGraph.toolkit(application)
    private val settingsPreferences = AppGraph.settings(application)
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
                ),
                reactiveTools = ToolkitLayout.orderedTools(
                    category = ToolkitCategory.Reactive,
                    enabledIds = snapshot.enabledToolIds,
                    savedOrder = snapshot.reactiveOrder,
                ),
                selectionProactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Proactive),
                selectionReactiveTools = ToolkitCatalog.byCategory(ToolkitCategory.Reactive),
            )
        }
    }

    fun openTool(tool: ToolkitTool) {
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
            )
        }
    }

    fun openFutureSelfMessage(messageId: Long) {
        viewModelScope.launch {
            val message = futureSelfRepository.getById(messageId) ?: return@launch
            val tool = ToolkitCatalog.byId(ToolkitToolId.FutureSelfMessage) ?: return@launch
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
            logRepository.save(
                type = ToolkitLogType.THOUGHT_DUMP,
                content = _uiState.value.thoughtDumpText,
                audioPath = _uiState.value.pendingAudioPath,
            )
            _uiState.update { it.copy(thoughtDumpText = "", pendingAudioPath = null) }
        }
    }

    fun saveAnxietyLog() {
        viewModelScope.launch {
            logRepository.save(
                type = ToolkitLogType.ANXIETY_LOG,
                content = _uiState.value.anxietyLogText,
                audioPath = _uiState.value.pendingAudioPath,
            )
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
            _uiState.update {
                it.copy(
                    futureSelfText = "",
                    pendingAudioPath = null,
                    futureSelfScheduledAtMillis = defaultFutureSelfScheduleTime(),
                    editingFutureSelfId = null,
                )
            }
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
            tool?.id == ToolkitToolId.FutureSelfMessage
}
