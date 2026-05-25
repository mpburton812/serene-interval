package com.example.meditationparticles.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.audio.ToolkitAudioPlayer
import com.example.meditationparticles.data.AppGraph
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.reminder.FutureSelfMessageScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun FutureSelfNotificationOverlay(
    messageId: Long,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { AppGraph.futureSelfMessages(context) }
    val audioPlayer = remember { ToolkitAudioPlayer() }
    var message by remember(messageId) { mutableStateOf<FutureSelfMessageEntity?>(null) }
    var isPlaying by remember(messageId) { mutableStateOf(false) }

    LaunchedEffect(messageId) {
        val loaded = repository.getById(messageId)
        if (loaded == null) {
            onDismiss()
        } else {
            message = loaded
        }
    }

    DisposableEffect(messageId) {
        onDispose {
            audioPlayer.stop()
            isPlaying = false
        }
    }

    val entry = message ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 340.dp),
        title = {
            Text(
                text = "Message from your past self",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = formatFutureSelfTimestamp(entry.scheduledAtMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.content.isNotBlank()) {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
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
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        FutureSelfMessageScheduler.cancel(context, entry.id)
                        repository.delete(entry.id)
                        audioPlayer.stop()
                        onDeleted()
                    }
                },
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

private fun formatFutureSelfTimestamp(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}
