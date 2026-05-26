package com.example.meditationparticles.ui.settings

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.onenote.OneNoteAuthResult
import com.example.meditationparticles.data.onenote.OneNoteNotebook
import com.example.meditationparticles.data.onenote.OneNotePrefsSnapshot
import com.example.meditationparticles.data.onenote.OneNoteSection
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
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

    init {
        viewModelScope.launch {
            oneNotePrefs.collect { prefs ->
                if (!prefs.accountEmail.isNullOrBlank() && _oneNoteUiState.value.notebooks.isEmpty()) {
                    refreshOneNoteTargets(showLoading = true)
                }
            }
        }
    }

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
            oneNoteAuth.reconcileConnectionState(oneNotePreferences)
            val authResult = runCatching { oneNoteAuth.signIn(activity) }.getOrElse { error ->
                OneNoteAuthResult.failure(error.message ?: "Could not start Microsoft sign-in.")
            }
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
            val backfilled = if (sectionResult.isSuccess) {
                oneNoteSync.backfillExistingEntries()
            } else {
                0
            }
            refreshOneNoteTargets(showLoading = true)
            _oneNoteUiState.value = OneNoteSettingsUiState(
                statusMessage = when {
                    sectionResult.isFailure -> "Connected, but could not prepare OneNote section."
                    backfilled > 0 -> "Connected. Queued $backfilled existing ${if (backfilled == 1) "entry" else "entries"}."
                    else -> "Connected to OneNote."
                },
                notebooks = _oneNoteUiState.value.notebooks,
                sections = _oneNoteUiState.value.sections,
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

    fun setOneNoteEntryTypeSyncEnabled(entryType: OneNoteEntryType, enabled: Boolean) {
        oneNotePreferences.setEntryTypeEnabled(entryType, enabled)
    }

    fun selectOneNoteNotebook(notebook: OneNoteNotebook) {
        if (_oneNoteUiState.value.isBusy) return
        viewModelScope.launch {
            _oneNoteUiState.value = _oneNoteUiState.value.copy(isBusy = true, errorMessage = null)
            runCatching {
                val sections = oneNoteSync.fetchSections(notebook.id)
                val preferredSection = sections.firstOrNull {
                    it.displayName.equals(
                        com.example.meditationparticles.data.onenote.OneNoteGraphClient.SECTION_NAME,
                        ignoreCase = true,
                    )
                } ?: sections.firstOrNull()
                if (preferredSection == null) {
                    error("No sections found in this notebook.")
                }
                oneNoteSync.applySyncTarget(
                    notebookId = notebook.id,
                    notebookName = notebook.displayName,
                    sectionId = preferredSection.id,
                    sectionName = preferredSection.displayName,
                )
                preferredSection to sections
            }.onSuccess { (preferredSection, sections) ->
                _oneNoteUiState.value = _oneNoteUiState.value.copy(
                    isBusy = false,
                    sections = sections,
                    statusMessage = "Sync target updated to ${notebook.displayName} → ${preferredSection.displayName}.",
                )
            }.onFailure { error ->
                _oneNoteUiState.value = _oneNoteUiState.value.copy(
                    isBusy = false,
                    errorMessage = error.message ?: "Could not update notebook.",
                )
            }
        }
    }

    fun selectOneNoteSection(section: OneNoteSection) {
        val prefs = oneNotePreferences.load()
        val notebookId = prefs.notebookId ?: return
        val notebookName = prefs.notebookName ?: return
        if (_oneNoteUiState.value.isBusy) return
        viewModelScope.launch {
            _oneNoteUiState.value = _oneNoteUiState.value.copy(isBusy = true, errorMessage = null)
            runCatching {
                oneNoteSync.applySyncTarget(
                    notebookId = notebookId,
                    notebookName = notebookName,
                    sectionId = section.id,
                    sectionName = section.displayName,
                )
            }.onSuccess {
                _oneNoteUiState.value = _oneNoteUiState.value.copy(
                    isBusy = false,
                    statusMessage = "Sync target updated to $notebookName → ${section.displayName}.",
                )
            }.onFailure { error ->
                _oneNoteUiState.value = _oneNoteUiState.value.copy(
                    isBusy = false,
                    errorMessage = error.message ?: "Could not update section.",
                )
            }
        }
    }

    fun backfillOneNoteExistingEntries() {
        if (_oneNoteUiState.value.isBusy) return
        viewModelScope.launch {
            _oneNoteUiState.value = _oneNoteUiState.value.copy(isBusy = true, errorMessage = null)
            val queued = runCatching { oneNoteSync.backfillExistingEntries() }
            val syncResult = runCatching { oneNoteSync.syncNow() }
            _oneNoteUiState.value = OneNoteSettingsUiState(
                statusMessage = when {
                    queued.isFailure -> queued.exceptionOrNull()?.message ?: "Could not queue entries."
                    syncResult.isSuccess && syncResult.getOrNull()?.syncedCount?.let { it > 0 } == true ->
                        syncResult.getOrNull()?.message
                    queued.getOrDefault(0) > 0 ->
                        "Queued ${queued.getOrDefault(0)} ${if (queued.getOrDefault(0) == 1) "entry" else "entries"} for sync."
                    else -> "No new entries to sync."
                },
                errorMessage = syncResult.exceptionOrNull()?.message,
                notebooks = _oneNoteUiState.value.notebooks,
                sections = _oneNoteUiState.value.sections,
            )
        }
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
                notebooks = _oneNoteUiState.value.notebooks,
                sections = _oneNoteUiState.value.sections,
            )
        }
    }

    fun clearOneNoteStatus() {
        _oneNoteUiState.value = _oneNoteUiState.value.copy(
            statusMessage = null,
            errorMessage = null,
        )
    }

    private suspend fun refreshOneNoteTargets(showLoading: Boolean) {
        if (!oneNoteAuth.isAvailable) return
        val prefs = oneNotePreferences.load()
        if (prefs.accountEmail.isNullOrBlank()) return
        if (showLoading) {
            _oneNoteUiState.value = _oneNoteUiState.value.copy(isLoadingTargets = true)
        }
        runCatching {
            val notebooks = oneNoteSync.fetchNotebooks()
            val sections = prefs.notebookId?.let { notebookId ->
                oneNoteSync.fetchSections(notebookId)
            }.orEmpty()
            _oneNoteUiState.value = _oneNoteUiState.value.copy(
                notebooks = notebooks,
                sections = sections,
                isLoadingTargets = false,
            )
        }.onFailure { error ->
            _oneNoteUiState.value = _oneNoteUiState.value.copy(
                isLoadingTargets = false,
                errorMessage = error.message,
            )
        }
    }
}
