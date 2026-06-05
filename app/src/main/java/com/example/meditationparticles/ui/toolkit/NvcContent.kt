package com.example.meditationparticles.ui.toolkit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.data.local.NvcEntryEntity
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.components.HistoryGlassCard
import com.example.meditationparticles.ui.components.MoodDisplay
import com.example.meditationparticles.ui.components.MoodPicker
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.util.Date

@Composable
fun NvcContent(
    stepIndex: Int,
    selectedMoodLevel: Int?,
    onMoodLevelChange: (Int?) -> Unit,
    observation: String,
    feeling: String,
    need: String,
    request: String,
    entries: List<NvcEntryEntity>,
    openedEntry: NvcEntryEntity?,
    onObservationChange: (String) -> Unit,
    onFeelingChange: (String) -> Unit,
    onNeedChange: (String) -> Unit,
    onRequestChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepChange: (Int) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpenEntry: (NvcEntryEntity) -> Unit,
    onDeleteEntry: (NvcEntryEntity) -> Unit,
    onCloseEntry: () -> Unit,
    showOneNoteSync: Boolean = false,
    onSyncEntryToOneNote: (NvcEntryEntity) -> Unit = {},
) {
    val stepCount = 4
    val isLastStep = stepIndex >= stepCount - 1
    val stepInstruction = when (stepIndex) {
        0 -> "Describe what happened — just the facts, without judgment."
        1 -> "Name the feeling this brings up in you."
        2 -> "What need or value of yours is connected to this feeling?"
        else -> "What clear, specific request could help meet that need?"
    }

    fun stepHasContent(): Boolean = when (stepIndex) {
        0 -> observation.isNotBlank()
        1 -> feeling.isNotBlank()
        2 -> need.isNotBlank()
        else -> request.isNotBlank()
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Text(
                text = "Step ${stepIndex + 1} of $stepCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stepInstruction,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MoodPicker(
                selectedLevel = selectedMoodLevel,
                onLevelChange = onMoodLevelChange,
            )

            ToolkitStepPager(
                stepIndex = stepIndex,
                pageCount = stepCount,
                onStepChange = onStepChange,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> NvcFieldEditor(
                        label = "Observation",
                        text = observation,
                        onTextChange = onObservationChange,
                    )
                    1 -> NvcFieldEditor(
                        label = "Feeling",
                        text = feeling,
                        onTextChange = onFeelingChange,
                    )
                    2 -> NvcFieldEditor(
                        label = "Need",
                        text = need,
                        onTextChange = onNeedChange,
                    )
                    else -> NvcFieldEditor(
                        label = "Request",
                        text = request,
                        onTextChange = onRequestChange,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = stepIndex > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Previous")
                }
                if (isLastStep) {
                    OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                        Text("Clear")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = stepHasContent(),
                    ) {
                        Text("Save")
                    }
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        enabled = stepHasContent(),
                    ) {
                        Text("Next")
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(18.dp),
                        )
                    }
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
                    NvcEntryRow(
                        entry = entry,
                        onOpen = { onOpenEntry(entry) },
                        onDelete = { onDeleteEntry(entry) },
                    )
                }
            }
        }
    }

    openedEntry?.let { entry ->
        NvcEntryDetailDialog(
            entry = entry,
            showOneNoteSync = showOneNoteSync,
            onSyncToOneNote = { onSyncEntryToOneNote(entry) },
            onDismiss = onCloseEntry,
        )
    }
}

@Composable
private fun NvcFieldEditor(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    minLines: Int = 8,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("What's on your mind…") },
            minLines = minLines,
        )
    }
}

@Composable
private fun NvcEntryRow(
    entry: NvcEntryEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatNvcTimestamp(entry.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val preview = entry.observation.ifBlank {
                entry.feeling.ifBlank {
                    entry.need.ifBlank {
                        entry.request.ifBlank { "NVC entry" }
                    }
                }
            }
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
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
private fun NvcEntryDetailDialog(
    entry: NvcEntryEntity,
    showOneNoteSync: Boolean,
    onSyncToOneNote: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatNvcTimestamp(entry.createdAt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                entry.moodLevel?.let { mood -> MoodDisplay(moodLevel = mood) }
                NvcReadOnlySection(label = "Observation", text = entry.observation)
                NvcReadOnlySection(label = "Feeling", text = entry.feeling)
                NvcReadOnlySection(label = "Need", text = entry.need)
                NvcReadOnlySection(label = "Request", text = entry.request)
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

@Composable
private fun NvcReadOnlySection(
    label: String,
    text: String,
) {
    if (text.isBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatNvcTimestamp(createdAt: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.getDefault())
    return formatter.format(Date(createdAt))
}
