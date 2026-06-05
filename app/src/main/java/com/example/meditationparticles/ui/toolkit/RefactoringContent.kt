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
import com.example.meditationparticles.data.local.RefactoringEntryEntity
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.components.HistoryGlassCard
import com.example.meditationparticles.ui.components.MoodDisplay
import com.example.meditationparticles.ui.components.MoodPicker
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.util.Date

@Composable
fun RefactoringContent(
    stepIndex: Int,
    selectedMoodLevel: Int?,
    onMoodLevelChange: (Int?) -> Unit,
    interpretation: String,
    actualFacts: String,
    explanation1: String,
    explanation2: String,
    explanation3: String,
    entries: List<RefactoringEntryEntity>,
    openedEntry: RefactoringEntryEntity?,
    onInterpretationChange: (String) -> Unit,
    onActualFactsChange: (String) -> Unit,
    onExplanation1Change: (String) -> Unit,
    onExplanation2Change: (String) -> Unit,
    onExplanation3Change: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepChange: (Int) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpenEntry: (RefactoringEntryEntity) -> Unit,
    onDeleteEntry: (RefactoringEntryEntity) -> Unit,
    onCloseEntry: () -> Unit,
    showOneNoteSync: Boolean = false,
    onSyncEntryToOneNote: (RefactoringEntryEntity) -> Unit = {},
) {
    val stepCount = 3
    val isLastStep = stepIndex >= stepCount - 1
    val stepInstruction = when (stepIndex) {
        0 -> "Write down the actual facts — only what you know for certain."
        1 -> "Write down your interpretation — the story your mind is telling."
        else -> "Write three non-threatening explanations based on logic."
    }

    fun stepHasContent(): Boolean = when (stepIndex) {
        0 -> actualFacts.isNotBlank()
        1 -> interpretation.isNotBlank()
        else -> explanation1.isNotBlank() || explanation2.isNotBlank() || explanation3.isNotBlank()
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
                    0 -> RefactoringFieldEditor(
                        label = "The actual facts",
                        text = actualFacts,
                        onTextChange = onActualFactsChange,
                    )
                    1 -> RefactoringFieldEditor(
                        label = "Interpretation",
                        text = interpretation,
                        onTextChange = onInterpretationChange,
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RefactoringFieldEditor(
                            label = "Explanation 1",
                            text = explanation1,
                            onTextChange = onExplanation1Change,
                            minLines = 4,
                        )
                        RefactoringFieldEditor(
                            label = "Explanation 2",
                            text = explanation2,
                            onTextChange = onExplanation2Change,
                            minLines = 4,
                        )
                        RefactoringFieldEditor(
                            label = "Explanation 3",
                            text = explanation3,
                            onTextChange = onExplanation3Change,
                            minLines = 4,
                        )
                    }
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
                    RefactoringEntryRow(
                        entry = entry,
                        onOpen = { onOpenEntry(entry) },
                        onDelete = { onDeleteEntry(entry) },
                    )
                }
            }
        }
    }

    openedEntry?.let { entry ->
        RefactoringEntryDetailDialog(
            entry = entry,
            showOneNoteSync = showOneNoteSync,
            onSyncToOneNote = { onSyncEntryToOneNote(entry) },
            onDismiss = onCloseEntry,
        )
    }
}

@Composable
private fun RefactoringFieldEditor(
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
private fun RefactoringEntryRow(
    entry: RefactoringEntryEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatRefactoringTimestamp(entry.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val preview = entry.actualFacts.ifBlank {
                entry.interpretation.ifBlank {
                    entry.explanation1.ifBlank { "Refactoring entry" }
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
private fun RefactoringEntryDetailDialog(
    entry: RefactoringEntryEntity,
    showOneNoteSync: Boolean,
    onSyncToOneNote: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatRefactoringTimestamp(entry.createdAt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                entry.moodLevel?.let { mood -> MoodDisplay(moodLevel = mood) }
                RefactoringReadOnlySection(label = "The actual facts", text = entry.actualFacts)
                RefactoringReadOnlySection(label = "Interpretation", text = entry.interpretation)
                RefactoringReadOnlySection(label = "Explanation 1", text = entry.explanation1)
                RefactoringReadOnlySection(label = "Explanation 2", text = entry.explanation2)
                RefactoringReadOnlySection(label = "Explanation 3", text = entry.explanation3)
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
private fun RefactoringReadOnlySection(
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

private fun formatRefactoringTimestamp(createdAt: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.getDefault())
    return formatter.format(Date(createdAt))
}
