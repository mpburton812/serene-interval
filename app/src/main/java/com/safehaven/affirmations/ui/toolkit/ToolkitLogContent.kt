package com.safehaven.affirmations.ui.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.data.local.ThoughtDumpEntity
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.HistoryGlassCard
import com.safehaven.affirmations.ui.components.JournalCaptureFields
import com.safehaven.affirmations.ui.components.MoodDisplay
import com.safehaven.affirmations.ui.theme.SereneSpacing
import java.util.Date

@Composable
fun ToolkitLogContent(
    instructionText: String,
    text: String,
    selectedMoodLevel: Int?,
    onMoodLevelChange: (Int?) -> Unit,
    entries: List<ThoughtDumpEntity>,
    openedEntry: ThoughtDumpEntity?,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onOpenEntry: (ThoughtDumpEntity) -> Unit,
    onDeleteEntry: (ThoughtDumpEntity) -> Unit,
    onCloseEntry: () -> Unit,
    showOneNoteSync: Boolean = false,
    onSyncEntryToOneNote: (ThoughtDumpEntity) -> Unit = {},
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            JournalCaptureFields(
                text = text,
                onTextChange = onTextChange,
                selectedMoodLevel = selectedMoodLevel,
                onMoodLevelChange = onMoodLevelChange,
                instructionText = instructionText,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = text.isNotBlank(),
                ) {
                    Text("Save & Close")
                }
            }
        }
    }

    if (entries.isNotEmpty()) {
        HistoryGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Previous entries",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    LogEntryRow(
                        entry = entry,
                        onOpen = { onOpenEntry(entry) },
                        onDelete = { onDeleteEntry(entry) },
                    )
                }
            }
        }
    }

    openedEntry?.let { entry ->
        LogEntryDetailDialog(
            entry = entry,
            showOneNoteSync = showOneNoteSync,
            onSyncToOneNote = { onSyncEntryToOneNote(entry) },
            onDismiss = onCloseEntry,
        )
    }
}

@Composable
private fun LogEntryRow(
    entry: ThoughtDumpEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatLogTimestamp(entry.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (entry.content.isNotBlank()) {
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Default.FolderOpen, contentDescription = "Open entry")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete entry")
        }
    }
}

@Composable
private fun LogEntryDetailDialog(
    entry: ThoughtDumpEntity,
    showOneNoteSync: Boolean,
    onSyncToOneNote: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatLogTimestamp(entry.createdAt)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                entry.moodLevel?.let { mood -> MoodDisplay(moodLevel = mood) }
                if (entry.content.isNotBlank()) {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (showOneNoteSync) {
                    OneNoteEntrySyncButton(onClick = onSyncToOneNote)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private fun formatLogTimestamp(createdAt: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.getDefault())
    return formatter.format(Date(createdAt))
}
