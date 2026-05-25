package com.example.meditationparticles.ui.toolkit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.domain.toolkit.ToolkitCatalog
import com.example.meditationparticles.domain.toolkit.ToolkitCategory
import com.example.meditationparticles.domain.toolkit.ToolkitLogType
import com.example.meditationparticles.domain.toolkit.ToolkitTool
import com.example.meditationparticles.domain.toolkit.ToolkitToolId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RandomToolState { Idle, Finding, Selected }

data class ToolkitUiState(
    val proactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Proactive),
    val reactiveTools: List<ToolkitTool> = ToolkitCatalog.byCategory(ToolkitCategory.Reactive),
    val selectedTool: ToolkitTool? = null,
    val stepIndex: Int = 0,
    val thoughtDumpText: String = "",
    val anxietyLogText: String = "",
    val pendingAudioPath: String? = null,
    val thoughtDumpEntries: List<ThoughtDumpEntity> = emptyList(),
    val anxietyLogEntries: List<ThoughtDumpEntity> = emptyList(),
    val openedLogEntry: ThoughtDumpEntity? = null,
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

    private val _uiState = MutableStateFlow(ToolkitUiState())
    val uiState: StateFlow<ToolkitUiState> = _uiState.asStateFlow()

    init {
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
            )
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

    fun clearActiveDraft() {
        _uiState.update {
            it.copy(
                thoughtDumpText = "",
                anxietyLogText = "",
                pendingAudioPath = null,
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

    fun pickRandomTool() {
        viewModelScope.launch {
            _uiState.update { it.copy(randomToolState = RandomToolState.Finding, randomSelectedTool = null) }
            delay(800)
            val tool = ToolkitCatalog.randomReactive()
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
        tool?.id == ToolkitToolId.ThoughtDump || tool?.id == ToolkitToolId.AnxietyLog
}
