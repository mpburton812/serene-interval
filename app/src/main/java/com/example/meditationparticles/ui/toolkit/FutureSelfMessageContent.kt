package com.example.meditationparticles.ui.toolkit

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.meditationparticles.R
import com.example.meditationparticles.audio.ToolkitAudioPlayer
import com.example.meditationparticles.audio.ToolkitAudioRecorder
import com.example.meditationparticles.data.local.FutureSelfMessageEntity
import com.example.meditationparticles.permissions.SchedulingPermissions
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.components.JournalCaptureFields
import com.example.meditationparticles.ui.theme.SereneSpacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FutureSelfMessageContent(
    text: String,
    selectedMoodLevel: Int?,
    onMoodLevelChange: (Int?) -> Unit,
    scheduledAtMillis: Long,
    pendingAudioPath: String?,
    entries: List<FutureSelfMessageEntity>,
    editingEntryId: Long?,
    openedEntry: FutureSelfMessageEntity?,
    onTextChange: (String) -> Unit,
    onScheduledAtChange: (Long) -> Unit,
    onPendingAudioChange: (String?) -> Unit,
    onSpeechResult: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onEditEntry: (FutureSelfMessageEntity) -> Unit,
    onDeleteEntry: (FutureSelfMessageEntity) -> Unit,
    onOpenEntry: (FutureSelfMessageEntity) -> Unit,
    onCloseEntry: () -> Unit,
    schedulingAvailable: Boolean = true,
    showOneNoteSync: Boolean = false,
    onSyncEntryToOneNote: (FutureSelfMessageEntity) -> Unit = {},
) {
    val context = LocalContext.current
    val appName = stringResource(R.string.app_name)
    if (!schedulingAvailable) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
            ) {
                Text(
                    text = "Future Self Message is unavailable",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Enable Alarms & reminders in system settings to schedule messages to your future self.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { SchedulingPermissions.openExactAlarmSettings(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open settings")
                }
            }
        }
        return
    }

    val audioPlayer = remember { ToolkitAudioPlayer() }
    var pendingSaveAfterNotification by remember { mutableStateOf(false) }
    var showNotificationDeniedDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingSaveAfterNotification) {
            pendingSaveAfterNotification = false
            onSave()
            maybeShowExactAlarmGuidance(context) { showExactAlarmDialog = true }
        } else {
            pendingSaveAfterNotification = false
            if (!granted) {
                showNotificationDeniedDialog = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { audioPlayer.release() }
    }

    fun showDatePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = scheduledAtMillis
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                onScheduledAtChange(updated.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH),
        ).show()
    }

    fun showTimePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = scheduledAtMillis }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val updated = Calendar.getInstance().apply {
                    timeInMillis = scheduledAtMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onScheduledAtChange(updated.timeInMillis)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false,
        ).show()
    }

    fun trySchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                pendingSaveAfterNotification = true
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onSave()
        maybeShowExactAlarmGuidance(context) { showExactAlarmDialog = true }
    }

    val canSave = (text.isNotBlank() || pendingAudioPath != null) &&
        scheduledAtMillis > System.currentTimeMillis()

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
                pendingAudioPath = pendingAudioPath,
                onPendingAudioChange = onPendingAudioChange,
                onSpeechResult = onSpeechResult,
                instructionText = "Write, dictate, or record a message for your future self. Choose when you'd like to receive it.",
                placeholder = "Dear future me…",
            )

            Text(
                text = "Deliver on",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = ::showDatePicker,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(formatFutureSelfDate(scheduledAtMillis))
                }
                OutlinedButton(
                    onClick = ::showTimePicker,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(formatFutureSelfTime(scheduledAtMillis))
                }
            }

            if (scheduledAtMillis <= System.currentTimeMillis()) {
                Text(
                    text = "Choose a date and time in the future.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Text(if (editingEntryId != null) "Cancel edit" else "Clear")
                }
                Button(
                    onClick = ::trySchedule,
                    modifier = Modifier.weight(1f),
                    enabled = canSave,
                ) {
                    Text(if (editingEntryId != null) "Update" else "Schedule")
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
                    text = "Scheduled messages",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    FutureSelfEntryRow(
                        entry = entry,
                        onOpen = { onOpenEntry(entry) },
                        onEdit = { onEditEntry(entry) },
                        onDelete = { onDeleteEntry(entry) },
                    )
                }
            }
        }
    }

    openedEntry?.let { entry ->
        FutureSelfEntryDetailDialog(
            entry = entry,
            audioPlayer = audioPlayer,
            showOneNoteSync = showOneNoteSync,
            onSyncToOneNote = { onSyncEntryToOneNote(entry) },
            onDismiss = onCloseEntry,
        )
    }

    if (showNotificationDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDeniedDialog = false },
            title = { Text("Notifications required") },
            text = {
                Text(
                    "Allow notifications so $appName can deliver your message at the scheduled time. " +
                        "You can enable them in system settings.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationDeniedDialog = false
                        SchedulingPermissions.openNotificationSettings(context)
                    },
                ) {
                    Text("Open settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationDeniedDialog = false }) {
                    Text("Not now")
                }
            },
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text("Enable precise delivery") },
            text = {
                Text(
                    "For on-time delivery, allow Alarms & reminders for $appName in system settings. " +
                        "Without this, your message may arrive late when the device is idle.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExactAlarmDialog = false
                        SchedulingPermissions.openExactAlarmSettings(context)
                    },
                ) {
                    Text("Open settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text("Continue anyway")
                }
            },
        )
    }
}

@Composable
private fun FutureSelfEntryRow(
    entry: FutureSelfMessageEntity,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val isPast = entry.scheduledAtMillis <= System.currentTimeMillis()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatFutureSelfTimestamp(entry.scheduledAtMillis),
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
                        text = "Audio message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = when {
                    entry.delivered -> "Delivered"
                    isPast -> "Ready to open"
                    else -> "Scheduled"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onOpen) {
            Icon(Icons.Default.FolderOpen, contentDescription = "Open message")
        }
        if (!entry.delivered || !isPast) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit message")
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete message")
        }
    }
}

@Composable
private fun FutureSelfEntryDetailDialog(
    entry: FutureSelfMessageEntity,
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
        title = { Text(formatFutureSelfTimestamp(entry.scheduledAtMillis)) },
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

private fun formatFutureSelfTimestamp(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatFutureSelfDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun formatFutureSelfTime(millis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}

fun defaultFutureSelfScheduleTime(): Long {
    return Calendar.getInstance().apply {
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun maybeShowExactAlarmGuidance(context: Context, onShow: () -> Unit) {
    if (!SchedulingPermissions.canScheduleExactAlarms(context)) {
        onShow()
    }
}
