package com.example.meditationparticles.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.BuildConfig
import com.example.meditationparticles.audio.ToolkitAudioPlayer
import com.example.meditationparticles.data.local.MeditationReflectionEntity
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.toolkit.OneNoteEntrySyncButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeditationReflectionHistory(
    entries: List<MeditationReflectionEntity>,
    openedEntry: MeditationReflectionEntity?,
    oneNoteConnected: Boolean,
    onOpenEntry: (MeditationReflectionEntity) -> Unit,
    onDeleteEntry: (MeditationReflectionEntity) -> Unit,
    onCloseEntry: () -> Unit,
    onSyncEntryToOneNote: (MeditationReflectionEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return

    val audioPlayer = remember { ToolkitAudioPlayer() }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Previous reflections",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                ReflectionHistoryRow(
                    entry = entry,
                    onOpen = { onOpenEntry(entry) },
                    onDelete = { onDeleteEntry(entry) },
                )
            }
        }
    }

    openedEntry?.let { entry ->
        ReflectionDetailDialog(
            entry = entry,
            audioPlayer = audioPlayer,
            showOneNoteSync = BuildConfig.ONENOTE_SYNC_AVAILABLE && oneNoteConnected,
            onSyncToOneNote = { onSyncEntryToOneNote(entry) },
            onDismiss = onCloseEntry,
        )
    }
}

@Composable
private fun ReflectionHistoryRow(
    entry: MeditationReflectionEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatReflectionTimestamp(entry.completedAt),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val preview = entry.reflection.trim()
            when {
                preview.isNotBlank() -> {
                    Text(
                        text = preview,
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
            Text(
                text = "${entry.durationSeconds / 60} min session",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Default.FolderOpen, contentDescription = "Open reflection")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete reflection")
        }
    }
}

@Composable
private fun ReflectionDetailDialog(
    entry: MeditationReflectionEntity,
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
        title = { Text(formatReflectionTimestamp(entry.completedAt)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${entry.durationSeconds / 60} min meditation",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.moodLevel?.let { mood ->
                    Text(
                        text = "Mood: ${mood.coerceIn(1, 5)}/5",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (entry.reflection.isNotBlank()) {
                    Text(
                        text = entry.reflection,
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

private fun formatReflectionTimestamp(completedAt: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return formatter.format(Date(completedAt))
}
