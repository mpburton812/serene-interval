package com.example.meditationparticles.ui.settings

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.onenote.OneNotePrefsSnapshot
import com.example.meditationparticles.data.export.AppDataExporter
import com.example.meditationparticles.data.export.AppDataImporter
import com.example.meditationparticles.data.export.ImportParseException
import com.example.meditationparticles.domain.settings.ExperienceSettings
import com.example.meditationparticles.domain.settings.ThemeMode
import com.example.meditationparticles.domain.visualizations.CalmingVisualizationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val exportError: String? = null,
    val pendingExportJson: String? = null,
    val isImporting: Boolean = false,
    val importSummary: String? = null,
    val importError: String? = null,
    val showImportDialog: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppGraph.settings(application)
    private val oneNotePreferences = AppGraph.oneNotePreferences(application)
    private val oneNoteAuth = AppGraph.oneNoteAuth(application)
    private val oneNoteSync = AppGraph.oneNoteSync(application)
    private val exporter = AppDataExporter(application)
    private val importer = AppDataImporter(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _oneNoteUiState = MutableStateFlow(OneNoteSettingsUiState())
    val oneNoteUiState: StateFlow<OneNoteSettingsUiState> = _oneNoteUiState.asStateFlow()

    val oneNotePrefs: StateFlow<OneNotePrefsSnapshot> = oneNotePreferences.snapshot
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OneNotePrefsSnapshot())

    val settings: StateFlow<ExperienceSettings> = preferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExperienceSettings())

    fun setPreferredName(name: String) {
        preferences.update { it.copy(preferredName = name) }
    }

    fun setSanctuaryName(name: String) {
        preferences.update { it.copy(sanctuaryName = name) }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.update { it.copy(themeMode = mode) }
    }

    fun setEnableBreathing(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableBreathing = enabled) }
        }
    }

    fun setEnableTimer(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableTimer = enabled) }
        }
    }

    fun setEnableAffirmations(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableAffirmations = enabled) }
        }
    }

    fun setEnableToolkit(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableToolkit = enabled) }
        }
    }

    fun setEnableVisuals(enabled: Boolean) {
        preferences.update { current ->
            current.withToolToggle { it.copy(enableVisuals = enabled) }
        }
    }

    fun toggleScene(id: CalmingVisualizationId) {
        preferences.update { current ->
            val scenes = current.enabledScenes.toMutableSet()
            if (scenes.contains(id.name)) {
                if (scenes.size > 1) scenes.remove(id.name)
            } else {
                scenes.add(id.name)
            }
            current.copy(enabledScenes = scenes)
        }
    }

    fun resetOnboarding() {
        preferences.update { it.copy(onboardingCompleted = false) }
    }

    fun prepareExport() {
        if (_uiState.value.isExporting) return
        viewModelScope.launch {
            _uiState.value = SettingsUiState(isExporting = true)
            runCatching {
                exporter.buildExportJson()
            }.onSuccess { json ->
                _uiState.value = SettingsUiState(pendingExportJson = json)
            }.onFailure { error ->
                _uiState.value = SettingsUiState(
                    exportError = error.message ?: "Could not prepare export.",
                )
            }
        }
    }

    fun clearPendingExport() {
        _uiState.value = _uiState.value.copy(pendingExportJson = null)
    }

    fun onExportFinished(success: Boolean, message: String? = null) {
        _uiState.value = SettingsUiState(
            exportMessage = if (success) {
                message ?: "Export saved."
            } else {
                null
            },
            exportError = if (success) {
                null
            } else {
                message ?: "Could not save export."
            },
        )
    }

    fun clearExportStatus() {
        _uiState.value = _uiState.value.copy(exportMessage = null, exportError = null)
    }

    fun importFromJson(json: String) {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isImporting = true,
                importSummary = null,
                importError = null,
                showImportDialog = false,
            )
            runCatching {
                importer.importFromJson(json)
            }.onSuccess { result ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importSummary = result.buildSummary(),
                    showImportDialog = true,
                )
            }.onFailure { error ->
                val message = when (error) {
                    is ImportParseException -> error.message
                    else -> error.message ?: "Could not import backup."
                }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importError = message,
                )
            }
        }
    }

    fun reportImportError(message: String) {
        _uiState.value = _uiState.value.copy(
            isImporting = false,
            importError = message,
        )
    }

    fun dismissImportDialog() {
        _uiState.value = _uiState.value.copy(
            showImportDialog = false,
            importSummary = null,
        )
    }

    fun clearImportStatus() {
        _uiState.value = _uiState.value.copy(importError = null)
    }

    fun connectOneNote(activity: Activity) {
        if (_oneNoteUiState.value.isBusy || !oneNoteAuth.isAvailable) return
        viewModelScope.launch {
            _oneNoteUiState.value = OneNoteSettingsUiState(isBusy = true)
            val authResult = oneNoteAuth.signIn(activity)
            if (authResult.cancelled) {
                _oneNoteUiState.value = OneNoteSettingsUiState()
                return@launch
            }
            if (!authResult.success) {
                _oneNoteUiState.value = OneNoteSettingsUiState(
                    errorMessage = authResult.errorMessage ?: "Could not connect OneNote.",
                )
                return@launch
            }
            oneNotePreferences.setAccountEmail(authResult.email ?: oneNoteAuth.currentAccountEmail())
            oneNotePreferences.setSyncEnabled(true)
            val sectionResult = oneNoteSync.ensureSection()
            sectionResult.onFailure { error ->
                oneNotePreferences.setLastError(error.message)
            }
            _oneNoteUiState.value = OneNoteSettingsUiState(
                statusMessage = "Connected to OneNote.",
            )
        }
    }

    fun disconnectOneNote() {
        if (_oneNoteUiState.value.isBusy) return
        viewModelScope.launch {
            _oneNoteUiState.value = OneNoteSettingsUiState(isBusy = true)
            runCatching { oneNoteSync.disconnect() }
            _oneNoteUiState.value = OneNoteSettingsUiState(
                statusMessage = "Disconnected from OneNote.",
            )
        }
    }

    fun setOneNoteSyncEnabled(enabled: Boolean) {
        oneNotePreferences.setSyncEnabled(enabled)
    }

    fun syncOneNoteNow() {
        if (_oneNoteUiState.value.isBusy) return
        viewModelScope.launch {
            _oneNoteUiState.value = OneNoteSettingsUiState(isBusy = true)
            val result = oneNoteSync.syncNow()
            _oneNoteUiState.value = OneNoteSettingsUiState(
                statusMessage = result.message,
                errorMessage = if (result.syncedCount == 0 && result.failedCount > 0) {
                    result.message
                } else {
                    null
                },
            )
        }
    }

    fun clearOneNoteStatus() {
        _oneNoteUiState.value = OneNoteSettingsUiState()
    }
}
