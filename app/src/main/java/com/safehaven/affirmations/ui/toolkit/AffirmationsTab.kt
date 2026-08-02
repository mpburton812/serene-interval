package com.safehaven.affirmations.ui.toolkit

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safehaven.affirmations.audio.TimerAudioPlayer
import com.safehaven.affirmations.data.local.AffirmationEntity
import com.safehaven.affirmations.data.parseAffirmationLines
import com.safehaven.affirmations.domain.affirmations.AffirmationListKind
import com.safehaven.affirmations.domain.affirmations.AffirmationReviewLogic
import com.safehaven.affirmations.domain.timer.TimerBellSoundChoice
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.JournalCaptureFields
import com.safehaven.affirmations.ui.components.SereneTabHeader
import com.safehaven.affirmations.ui.theme.SerenePrimary
import com.safehaven.affirmations.ui.theme.SereneSpacing

private const val TransitionMs = 300

@Composable
fun AffirmationsTab(
    modifier: Modifier = Modifier,
    listKind: AffirmationListKind = AffirmationListKind.Affirmations,
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    val viewModel: AffirmationsViewModel = viewModel(
        key = listKind.name,
        factory = AffirmationsViewModel.factory(application, listKind),
    )
    val state by viewModel.uiState.collectAsState()
    val affirmationCalendar by viewModel.affirmationCalendar.collectAsState()
    val audioPlayer = remember { TimerAudioPlayer(context) }

    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }

    LaunchedEffect(state.showReviewAssessment) {
        if (state.showReviewAssessment) {
            audioPlayer.playBell(TimerBellSoundChoice.Default, systemUri = null)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* handled on next toggle */ }

    Box(modifier = modifier.fillMaxSize()) {
        if (!state.showReview) {
            Column(modifier = Modifier.fillMaxSize()) {
            SereneTabHeader(
                title = listKind.displayTitle,
                controls = {
                    TextButton(onClick = viewModel::showAddDialog) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = SerenePrimary)
                        Text("Add New", modifier = Modifier.padding(start = 4.dp), maxLines = 1)
                    }
                    TextButton(onClick = viewModel::showBulkImportDialog) {
                        Text("Bulk Import", maxLines = 1)
                    }
                },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SereneSpacing.containerMargin),
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
            ) {
                AffirmationHeroCard(
                    affirmation = state.currentAffirmation,
                    heroLabel = listKind.heroLabel,
                    emptyMessage = listKind.heroEmptyMessage,
                    onNext = viewModel::nextAffirmation,
                )

                Button(
                    onClick = viewModel::startReview,
                    enabled = state.canStartReview,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(listKind.reviewButtonLabel)
                }

                AffirmationCalendarSection(
                    monthTitle = affirmationCalendar.monthTitle,
                    days = affirmationCalendar.days,
                    weekdayHeaders = affirmationCalendar.weekdayHeaders,
                    onPreviousMonth = { viewModel.shiftCalendarMonth(forward = false) },
                    onNextMonth = { viewModel.shiftCalendarMonth(forward = true) },
                    canGoForward = affirmationCalendar.canGoForward,
                    modifier = Modifier.fillMaxWidth(),
                )

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = if (state.reminderEnabled) {
                                    Icons.Default.Notifications
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column {
                                Text(text = "Daily Reminder", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = if (state.reminderEnabled) {
                                        "%02d:%02d".format(state.reminderHour, state.reminderMinute)
                                    } else {
                                        "Off"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                if (enabled) {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute -> viewModel.setReminder(true, hour, minute) },
                                        state.reminderHour,
                                        state.reminderMinute,
                                        false,
                                    ).show()
                                } else {
                                    viewModel.setReminder(false, state.reminderHour, state.reminderMinute)
                                }
                            },
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "My Collection",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = listKind.collectionSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.affirmations.isNotEmpty()) {
                        Text(
                            text = "Press and hold to reorder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    state.importMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (state.archivedAffirmations.isNotEmpty()) {
                        Text(
                            text = if (state.showArchived) {
                                "Hide archived (${state.archivedAffirmations.size})"
                            } else {
                                "View archived (${state.archivedAffirmations.size})"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable(onClick = viewModel::toggleShowArchived),
                        )
                    }
                }

                if (state.affirmations.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = listKind.emptyCollectionMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    ReorderableAffirmationList(
                        affirmations = state.affirmations,
                        onEdit = viewModel::showEditDialog,
                        onArchive = viewModel::archiveAffirmation,
                        onDelete = viewModel::deleteAffirmation,
                        onReorder = viewModel::reorderAffirmations,
                    )
                }

                if (state.showArchived && state.archivedAffirmations.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
                    ) {
                        Text(
                            text = "Archived",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = SereneSpacing.stackMd),
                        )
                        state.archivedAffirmations.forEach { affirmation ->
                            ArchivedAffirmationCard(
                                affirmation = affirmation,
                                onRestore = { viewModel.unarchiveAffirmation(affirmation) },
                            )
                        }
                    }
                }
            }
            }
        }

        if (state.showReview) {
            AffirmationReviewOverlay(
                affirmations = state.affirmations,
                currentIndex = state.reviewIndex,
                currentAffirmation = state.reviewAffirmation,
                isCompleting = false,
                onPrevious = viewModel::reviewPrevious,
                onNext = viewModel::reviewNext,
                onExit = viewModel::exitReview,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (state.showReviewAssessment) {
        AffirmationReviewAssessmentDialog(
            notes = state.reviewAssessmentNotes,
            moodLevel = state.reviewAssessmentMoodLevel,
            affirmationCount = state.completedReviewAffirmationCount,
            itemNoun = listKind.itemNoun,
            onNotesChange = viewModel::updateReviewAssessmentNotes,
            onMoodChange = viewModel::updateReviewAssessmentMoodLevel,
            onSave = viewModel::saveReviewAssessment,
            onSkip = viewModel::skipReviewAssessment,
        )
    }

    if (state.showBulkImportDialog) {
        BulkImportDialog(
            itemNoun = listKind.itemNoun,
            onDismiss = viewModel::dismissBulkImportDialog,
            onImport = viewModel::bulkImport,
        )
    }

    if (state.showAddDialog) {
        val noun = listKind.itemNoun.replaceFirstChar { it.uppercase() }
        AffirmationEditorDialog(
            initialText = state.editingAffirmation?.text ?: "",
            title = if (state.editingAffirmation == null) "Add $noun" else "Edit $noun",
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::saveAffirmation,
        )
    }
}

@Composable
private fun AffirmationHeroCard(
    affirmation: AffirmationEntity?,
    heroLabel: String,
    emptyMessage: String,
    onNext: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 32.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
        ) {
            Text(
                text = heroLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
            )

            AnimatedContent(
                targetState = affirmation?.text ?: emptyMessage,
                transitionSpec = {
                    (fadeIn(tween(TransitionMs)) + slideInVertically { it / 4 }) togetherWith
                        (fadeOut(tween(TransitionMs)) + slideOutVertically { -it / 4 })
                },
                label = "affirmation_hero",
            ) { text ->
                Text(
                    text = "\"$text\"",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = SereneSpacing.stackMd),
                )
            }

            Button(
                onClick = onNext,
                enabled = affirmation != null,
            ) {
                Text("Next")
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(18.dp),
                )
            }
        }
    }
}

@Composable
internal fun AffirmationCollectionCard(
    affirmation: AffirmationEntity,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "\"${affirmation.text}\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSavedAgo(affirmation.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onArchive) {
                        Icon(Icons.Default.Archive, contentDescription = "Archive")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
internal fun ArchivedAffirmationCard(
    affirmation: AffirmationEntity,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "\"${affirmation.text}\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSavedAgo(affirmation.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.Unarchive,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp),
                    )
                    Text("Restore")
                }
            }
        }
    }
}

@Composable
private fun AffirmationReviewAssessmentDialog(
    notes: String,
    moodLevel: Int?,
    affirmationCount: Int,
    itemNoun: String,
    onNotesChange: (String) -> Unit,
    onMoodChange: (Int?) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
) {
    val canSave = AffirmationReviewLogic.canSaveAssessment(moodLevel, notes)

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Session assessment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "You reviewed $affirmationCount $itemNoun${if (affirmationCount == 1) "" else "s"}. " +
                        "How did that feel?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                JournalCaptureFields(
                    text = notes,
                    onTextChange = onNotesChange,
                    selectedMoodLevel = moodLevel,
                    onMoodLevelChange = onMoodChange,
                    placeholder = "What stood out from this review?",
                    minLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        },
    )
}

@Composable
private fun BulkImportDialog(
    itemNoun: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    val parsedCount = remember(text) { parseAffirmationLines(text).size }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Import") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Paste one $itemNoun per line. Empty lines are ignored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("One item per line…") },
                    minLines = 8,
                )
                Text(
                    text = when (parsedCount) {
                        0 -> "No ${itemNoun}s to import"
                        1 -> "1 $itemNoun ready to import"
                        else -> "$parsedCount ${itemNoun}s ready to import"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (parsedCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text) },
                enabled = parsedCount > 0,
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun AffirmationEditorDialog(
    initialText: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("I am calm and present…") },
                minLines = 3,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.trim().isNotEmpty(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun formatSavedAgo(createdAt: Long): String {
    val relative = DateUtils.getRelativeTimeSpanString(
        createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    )
    return "Saved $relative"
}
