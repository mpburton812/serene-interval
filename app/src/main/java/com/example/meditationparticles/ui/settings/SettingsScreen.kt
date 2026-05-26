package com.example.meditationparticles.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.meditationparticles.data.export.AppDataExporter
import com.example.meditationparticles.ui.theme.SereneSpacing
import com.example.meditationparticles.ui.update.UpdateViewModel
import java.io.IOException
import androidx.activity.ComponentActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    updateViewModel: UpdateViewModel,
    onBack: () -> Unit,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val settingsUiState by viewModel.uiState.collectAsState()
    val oneNotePrefs by viewModel.oneNotePrefs.collectAsState()
    val oneNoteUiState by viewModel.oneNoteUiState.collectAsState()
    val updateState by updateViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) {
            viewModel.clearImportStatus()
            return@rememberLauncherForActivityResult
        }

        val readResult = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IOException("Could not open selected file.")
        }
        readResult.onSuccess { json ->
            viewModel.importFromJson(json)
        }.onFailure { error ->
            viewModel.reportImportError(
                error.message ?: "Could not read selected file.",
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        val json = settingsUiState.pendingExportJson
        if (uri == null || json == null) {
            viewModel.clearPendingExport()
            if (uri == null && json != null) {
                viewModel.onExportFinished(success = false, message = "Export cancelled.")
            }
            return@rememberLauncherForActivityResult
        }

        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(json.toByteArray(Charsets.UTF_8))
            } ?: error("Could not open destination file.")
        }
        viewModel.clearPendingExport()
        result.onSuccess {
            viewModel.onExportFinished(success = true)
        }.onFailure { error ->
            viewModel.onExportFinished(
                success = false,
                message = error.message ?: "Could not save export.",
            )
        }
    }

    LaunchedEffect(settingsUiState.pendingExportJson) {
        if (settingsUiState.pendingExportJson != null) {
            exportLauncher.launch(AppDataExporter.DEFAULT_FILENAME)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = settings.sanctuaryTitle,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(SereneSpacing.containerMargin),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
        ) {
            Text(
                text = "Shape a space that feels uniquely yours.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SanctuaryNameField(
                value = settings.sanctuaryName,
                onValueChange = viewModel::setSanctuaryName,
            )

            PreferredNameField(
                value = settings.preferredName,
                onValueChange = viewModel::setPreferredName,
            )

            ThemeSection(
                settings = settings,
                onThemeModeSelected = viewModel::setThemeMode,
            )

            ExperienceSection(
                settings = settings,
                onBreathingChanged = viewModel::setEnableBreathing,
                onTimerChanged = viewModel::setEnableTimer,
                onAffirmationsChanged = viewModel::setEnableAffirmations,
                onToolkitChanged = viewModel::setEnableToolkit,
                onVisualsChanged = viewModel::setEnableVisuals,
            )

            if (settings.enableVisuals) {
                VisualSanctuarySection(
                    enabledScenes = settings.enabledScenes,
                    onToggleScene = viewModel::toggleScene,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
            ) {
                Text(
                    text = "Your data",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Download a JSON backup of your settings and journal entries, or restore " +
                        "from a previous export. Audio recordings are referenced by path only and are " +
                        "not included in the backup file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = viewModel::prepareExport,
                    enabled = !settingsUiState.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (settingsUiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "Download configuration & entries",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        viewModel.clearImportStatus()
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    enabled = !settingsUiState.isImporting && !settingsUiState.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (settingsUiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "Import configuration & entries",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                settingsUiState.exportMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                settingsUiState.exportError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                settingsUiState.importError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (settingsUiState.showImportDialog && settingsUiState.importSummary != null) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissImportDialog,
                    title = { Text("Import complete") },
                    text = {
                        Text(
                            text = settingsUiState.importSummary.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = viewModel::dismissImportDialog) {
                            Text("OK")
                        }
                    },
                )
            }

            OneNoteIntegrationSection(
                prefs = oneNotePrefs,
                uiState = oneNoteUiState,
                onConnect = {
                    activity?.let(viewModel::connectOneNote)
                },
                onDisconnect = viewModel::disconnectOneNote,
                onSyncEnabledChange = viewModel::setOneNoteSyncEnabled,
                onSyncNow = viewModel::syncOneNoteNow,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
            ) {
                Text(
                    text = "App updates",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Installed: ${updateState.installedVersionName}. " +
                        "Compares against the latest build published on main.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { updateViewModel.checkForUpdate(userInitiated = true) },
                    enabled = !updateState.isChecking && !updateState.isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (updateState.isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(vertical = 8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "Check for updates",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                updateState.statusMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                updateState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
            ) {
                Text(
                    text = "Rebuild Your Sanctuary",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Walk through setup again to revisit your name, tools, and scenes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        viewModel.resetOnboarding()
                        onResetOnboarding()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = "Rebuild!",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(SereneSpacing.stackLg))
        }
    }
}
