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
import com.example.meditationparticles.data.local.ThoughtDumpEntity
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.util.Date

@Composable
fun ToolkitLogContent(
    instructionText: String,
    text: String,
    entries: List<ThoughtDumpEntity>,
    pendingAudioPath: String?,
    openedEntry: ThoughtDumpEntity?,
    onTextChange: (String) -> Unit,
    onPendingAudioChange: (String?) -> Unit,
    onSpeechResult: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    onOpenEntry: (ThoughtDumpEntity) -> Unit,
    onDeleteEntry: (ThoughtDumpEntity) -> Unit,
    onCloseEntry: () -> Unit,
    showOneNoteSync: Boolean = false,
    onSyncEntryToOneNote: (ThoughtDumpEntity) -> Unit = {},
) {
    val context = LocalContext.current
    val audioRecorder = remember { ToolkitAudioRecorder(context) }
    val audioPlayer = remember { ToolkitAudioPlayer() }
    var isRecording by remember { mutableStateOf(false) }

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
            spoken?.let(onSpeechResult)
        }
    }

    fun launchSpeechToText() {
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

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Text(
                text = instructionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("What's on your mind…") },
                minLines = 8,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = ::launchSpeechToText,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Dictate", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = ::toggleRecording,
                    modifier = Modifier.weight(1f),
                ) {
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

            if (pendingAudioPath != null) {
                Text(
                    text = if (isRecording) "Recording in progress…" else "Audio ready to save",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

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
                    enabled = text.isNotBlank() || pendingAudioPath != null,
                ) {
                    Text("Save & Close")
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
            audioPlayer = audioPlayer,
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
            when {
                entry.content.isNotBlank() -> {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                entry.audioPath != null -> {
                    Text(
                        text = "Audio recording",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
    audioPlayer: ToolkitAudioPlayer,
    showOneNoteSync: Boolean,
    onSyncToOneNote: () -> Unit,
    onDismiss: () -> Unit,
) {
    var isPlaying by remember(entry.id) { mutableStateOf(false) }

    DisposableEffect(entry.id) {
        onDispose {
            audioPlayer.stop()
            isPlaying = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatLogTimestamp(entry.createdAt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (entry.content.isNotBlank()) {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                entry.audioPath?.let { path ->
                    OutlinedButton(
                        onClick = {
                            if (isPlaying) {
                                audioPlayer.stop()
                                isPlaying = false
                            } else {
                                audioPlayer.play(path)
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (isPlaying) "Stop playback" else "Play recording",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
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
