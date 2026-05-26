package com.example.meditationparticles.ui.toolkit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.meditationparticles.audio.ToolkitAudioPlayer
import com.example.meditationparticles.audio.ToolkitAudioRecorder
import com.example.meditationparticles.data.local.CenterOfGravityEntryEntity
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.util.Date

enum class CenterOfGravitySpeechTarget {
    ThoughtsAndFeelings,
    BodyAndNeeds,
}

private const val CENTER_OF_GRAVITY_INTRO =
    "Anxious attachment causes an individual to externalize their center of gravity, constantly monitoring the partner's emotional state, needs, and micro-expressions while completely ignoring their own."

@Composable
fun CenterOfGravityContent(
    stepIndex: Int,
    thoughtsAndFeelings: String,
    bodyAndNeeds: String,
    pendingAudioPath: String?,
    entries: List<CenterOfGravityEntryEntity>,
    openedEntry: CenterOfGravityEntryEntity?,
    onThoughtsAndFeelingsChange: (String) -> Unit,
    onBodyAndNeedsChange: (String) -> Unit,
    onPendingAudioChange: (String?) -> Unit,
    onSpeechResult: (CenterOfGravitySpeechTarget, String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpenEntry: (CenterOfGravityEntryEntity) -> Unit,
    onDeleteEntry: (CenterOfGravityEntryEntity) -> Unit,
    onCloseEntry: () -> Unit,
    showOneNoteSync: Boolean = false,
    onSyncEntryToOneNote: (CenterOfGravityEntryEntity) -> Unit = {},
) {
    val context = LocalContext.current
    val audioRecorder = remember { ToolkitAudioRecorder(context) }
    val audioPlayer = remember { ToolkitAudioPlayer() }
    var isRecording by remember { mutableStateOf(false) }
    var speechTarget by remember { mutableStateOf(CenterOfGravitySpeechTarget.ThoughtsAndFeelings) }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) audioRecorder.stop()
            audioPlayer.release()
        }
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            audioRecorder.start()?.let { path ->
                onPendingAudioChange(path)
                isRecording = true
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            spoken?.let { onSpeechResult(speechTarget, it) }
        }
    }

    fun launchSpeechToText(target: CenterOfGravitySpeechTarget) {
        speechTarget = target
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your thoughts…")
        }
        speechLauncher.launch(intent)
    }

    fun toggleRecording() {
        if (isRecording) {
            val path = audioRecorder.stop()
            isRecording = false
            onPendingAudioChange(path ?: pendingAudioPath)
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                audioRecorder.start()?.let { path ->
                    onPendingAudioChange(path)
                    isRecording = true
                }
            } else {
                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val stepCount = 2
    val isLastStep = stepIndex >= stepCount - 1
    val stepInstruction = when (stepIndex) {
        0 -> "What am I thinking and feeling right now?"
        else -> "What am I feeling in my body right now? What do I need in this moment to feel slightly more grounded?"
    }

    fun stepHasContent(): Boolean = when (stepIndex) {
        0 -> thoughtsAndFeelings.isNotBlank() || pendingAudioPath != null
        else -> bodyAndNeeds.isNotBlank() || pendingAudioPath != null
    }

    Text(
        text = CENTER_OF_GRAVITY_INTRO,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )

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

            when (stepIndex) {
                0 -> CenterOfGravityFieldEditor(
                    label = "Thoughts and feelings",
                    text = thoughtsAndFeelings,
                    onTextChange = onThoughtsAndFeelingsChange,
                    onDictate = { launchSpeechToText(CenterOfGravitySpeechTarget.ThoughtsAndFeelings) },
                    onToggleRecord = ::toggleRecording,
                    isRecording = isRecording,
                    pendingAudioPath = pendingAudioPath,
                )
                else -> CenterOfGravityFieldEditor(
                    label = "Body and needs",
                    text = bodyAndNeeds,
                    onTextChange = onBodyAndNeedsChange,
                    onDictate = { launchSpeechToText(CenterOfGravitySpeechTarget.BodyAndNeeds) },
                    onToggleRecord = ::toggleRecording,
                    isRecording = isRecording,
                    pendingAudioPath = pendingAudioPath,
                )
            }

            if (pendingAudioPath != null) {
                Text(
                    text = if (isRecording) "Recording in progress…" else "Audio ready to save with this step",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
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
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
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
                    CenterOfGravityEntryRow(
                        entry = entry,
                        onOpen = { onOpenEntry(entry) },
                        onDelete = { onDeleteEntry(entry) },
                    )
                }
            }
        }
    }

    openedEntry?.let { entry ->
        CenterOfGravityEntryDetailDialog(
            entry = entry,
            audioPlayer = audioPlayer,
            showOneNoteSync = showOneNoteSync,
            onSyncToOneNote = { onSyncEntryToOneNote(entry) },
            onDismiss = onCloseEntry,
        )
    }
}

@Composable
private fun CenterOfGravityFieldEditor(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    onDictate: () -> Unit,
    onToggleRecord: () -> Unit,
    isRecording: Boolean,
    pendingAudioPath: String?,
    minLines: Int = 6,
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
            placeholder = { Text("Write here…") },
            minLines = minLines,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onDictate, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Dictate", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = onToggleRecord, modifier = Modifier.weight(1f)) {
                Icon(
                    if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    if (isRecording) "Stop" else "Record",
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        if (pendingAudioPath != null && isRecording) {
            Text(
                text = "Recording…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CenterOfGravityEntryRow(
    entry: CenterOfGravityEntryEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatCenterOfGravityTimestamp(entry.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val preview = entry.thoughtsAndFeelings.ifBlank {
                entry.bodyAndNeeds.ifBlank { "Center of gravity entry" }
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
private fun CenterOfGravityEntryDetailDialog(
    entry: CenterOfGravityEntryEntity,
    audioPlayer: ToolkitAudioPlayer,
    showOneNoteSync: Boolean,
    onSyncToOneNote: () -> Unit,
    onDismiss: () -> Unit,
) {
    var playingPath by remember(entry.id) { mutableStateOf<String?>(null) }

    DisposableEffect(entry.id) {
        onDispose {
            audioPlayer.stop()
            playingPath = null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatCenterOfGravityTimestamp(entry.createdAt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CenterOfGravityReadOnlySection(
                    label = "Thoughts and feelings",
                    text = entry.thoughtsAndFeelings,
                    audioPath = entry.thoughtsAndFeelingsAudioPath,
                    playingPath = playingPath,
                    onPlayToggle = { path ->
                        if (playingPath == path) {
                            audioPlayer.stop()
                            playingPath = null
                        } else {
                            audioPlayer.play(path)
                            playingPath = path
                        }
                    },
                )
                CenterOfGravityReadOnlySection(
                    label = "Body and needs",
                    text = entry.bodyAndNeeds,
                    audioPath = entry.bodyAndNeedsAudioPath,
                    playingPath = playingPath,
                    onPlayToggle = { path ->
                        if (playingPath == path) {
                            audioPlayer.stop()
                            playingPath = null
                        } else {
                            audioPlayer.play(path)
                            playingPath = path
                        }
                    },
                )
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
private fun CenterOfGravityReadOnlySection(
    label: String,
    text: String,
    audioPath: String?,
    playingPath: String?,
    onPlayToggle: (String) -> Unit,
) {
    if (text.isBlank() && audioPath.isNullOrBlank()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        if (text.isNotBlank()) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
        audioPath?.let { path ->
            OutlinedButton(
                onClick = { onPlayToggle(path) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    if (playingPath == path) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    if (playingPath == path) "Stop playback" else "Play recording",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

private fun formatCenterOfGravityTimestamp(createdAt: Long): String {
    val formatter = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", java.util.Locale.getDefault())
    return formatter.format(Date(createdAt))
}
