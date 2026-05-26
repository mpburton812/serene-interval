package com.example.meditationparticles.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.BuildConfig
import com.example.meditationparticles.data.onenote.OneNoteGraphClient
import com.example.meditationparticles.data.onenote.OneNoteNotebook
import com.example.meditationparticles.data.onenote.OneNotePrefsSnapshot
import com.example.meditationparticles.data.onenote.OneNoteSection
import com.example.meditationparticles.domain.onenote.OneNoteEntryType
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun OneNoteIntegrationSection(
    prefs: OneNotePrefsSnapshot,
    uiState: OneNoteSettingsUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSyncEnabledChange: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onSyncExistingEntries: () -> Unit,
    onEntryTypeSyncChange: (OneNoteEntryType, Boolean) -> Unit,
    onSelectNotebook: (OneNoteNotebook) -> Unit,
    onSelectSection: (OneNoteSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNotebookPicker by remember { mutableStateOf(false) }
    var showSectionPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = "Integrations",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "OneNote",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (!BuildConfig.ONENOTE_SYNC_AVAILABLE) {
            Text(
                text = "OneNote sync is not configured for this build. Add onenote.clientId to " +
                    "local.properties and register the app in Azure — see release/ONENOTE.md.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        Text(
            text = "When connected, saved toolkit journal text is copied to your Microsoft OneNote " +
                "account. Audio recordings stay on this device only. You can disconnect or turn off " +
                "auto-sync at any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val connected = !prefs.accountEmail.isNullOrBlank()
        Text(
            text = if (connected) {
                "Connected as ${prefs.accountEmail}"
            } else {
                "Not connected"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (connected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )

        if (connected) {
            val notebookLabel = prefs.notebookName ?: "Default notebook"
            val sectionLabel = prefs.sectionName ?: OneNoteGraphClient.SECTION_NAME
            Text(
                text = "Syncing to: $notebookLabel → $sectionLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedButton(
                onClick = { showNotebookPicker = true },
                enabled = !uiState.isBusy && !uiState.isLoadingTargets,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Choose notebook",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            OutlinedButton(
                onClick = { showSectionPicker = true },
                enabled = !uiState.isBusy &&
                    !uiState.isLoadingTargets &&
                    !prefs.notebookId.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Choose section",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Text(
                text = "Sync entry types",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OneNoteEntryType.entries.forEach { entryType ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entryType.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = entryType in prefs.enabledEntryTypes,
                        onCheckedChange = { enabled -> onEntryTypeSyncChange(entryType, enabled) },
                        enabled = !uiState.isBusy,
                    )
                }
            }
        }

        prefs.lastSyncAtMillis?.let { millis ->
            Text(
                text = "Last sync: ${formatSyncTime(millis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        uiState.statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        (uiState.errorMessage ?: prefs.lastError)?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (connected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Auto-sync on save",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = prefs.syncEnabled,
                    onCheckedChange = onSyncEnabledChange,
                    enabled = !uiState.isBusy,
                )
            }
        }

        OutlinedButton(
            onClick = if (connected) onDisconnect else onConnect,
            enabled = !uiState.isBusy,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            if (uiState.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 8.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = if (connected) "Disconnect" else "Connect Microsoft account",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }

        if (connected) {
            OutlinedButton(
                onClick = onSyncExistingEntries,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Sync existing entries",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            OutlinedButton(
                onClick = onSyncNow,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = "Sync now",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }

    if (showNotebookPicker) {
        OneNoteTargetPickerDialog(
            title = "Choose notebook",
            items = uiState.notebooks.map { it.displayName.ifBlank { "Untitled notebook" } },
            isLoading = uiState.isLoadingTargets,
            onDismiss = { showNotebookPicker = false },
            onSelect = { index ->
                uiState.notebooks.getOrNull(index)?.let(onSelectNotebook)
                showNotebookPicker = false
            },
        )
    }

    if (showSectionPicker) {
        OneNoteTargetPickerDialog(
            title = "Choose section",
            items = uiState.sections.map { it.displayName.ifBlank { "Untitled section" } },
            isLoading = uiState.isLoadingTargets,
            onDismiss = { showSectionPicker = false },
            onSelect = { index ->
                uiState.sections.getOrNull(index)?.let(onSelectSection)
                showSectionPicker = false
            },
        )
    }
}

@Composable
private fun OneNoteTargetPickerDialog(
    title: String,
    items: List<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }
                items.isEmpty() -> {
                    Text(
                        text = "No items found.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(items.size) { index ->
                            Text(
                                text = items[index],
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(index) }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun formatSyncTime(epochMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
    return formatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
}

data class OneNoteSettingsUiState(
    val isBusy: Boolean = false,
    val isLoadingTargets: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val notebooks: List<OneNoteNotebook> = emptyList(),
    val sections: List<OneNoteSection> = emptyList(),
)
