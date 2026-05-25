package com.example.meditationparticles.ui.toolkit

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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.data.parseAffirmationLines
import com.example.meditationparticles.ui.components.GlassCard
import com.example.meditationparticles.ui.theme.SerenePrimary
import com.example.meditationparticles.ui.theme.SereneSpacing
import com.example.meditationparticles.ui.theme.SereneTertiary

private const val TransitionMs = 300

@Composable
fun AffirmationsTab(
    viewModel: AffirmationsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* handled on next toggle */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackLg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.viewMode == AffirmationViewMode.Card,
                onClick = { viewModel.setViewMode(AffirmationViewMode.Card) },
                label = { Text("Card") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
            FilterChip(
                selected = state.viewMode == AffirmationViewMode.List,
                onClick = { viewModel.setViewMode(AffirmationViewMode.List) },
                label = { Text("List") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        if (state.viewMode == AffirmationViewMode.Card) {
            AffirmationHeroCard(
                affirmation = state.currentAffirmation,
                onNext = viewModel::nextAffirmation,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "My Collection",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Your personal echoes of strength",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.importMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = viewModel::showBulkImportDialog) {
                    Text("Bulk import")
                }
                TextButton(onClick = viewModel::showAddDialog) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = SerenePrimary)
                    Text("Add New", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        if (state.affirmations.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No affirmations yet. Add your first one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.gutter)) {
                state.affirmations.forEach { affirmation ->
                    AffirmationCollectionCard(
                        affirmation = affirmation,
                        onFavorite = { viewModel.toggleFavorite(affirmation) },
                        onEdit = { viewModel.showEditDialog(affirmation) },
                        onDelete = { viewModel.deleteAffirmation(affirmation) },
                    )
                }
            }
        }

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
    }

    if (state.showBulkImportDialog) {
        BulkImportDialog(
            onDismiss = viewModel::dismissBulkImportDialog,
            onImport = viewModel::bulkImport,
        )
    }

    if (state.showAddDialog) {
        AffirmationEditorDialog(
            initialText = state.editingAffirmation?.text ?: "",
            title = if (state.editingAffirmation == null) "Add Affirmation" else "Edit Affirmation",
            onDismiss = viewModel::dismissDialog,
            onSave = viewModel::saveAffirmation,
        )
    }
}

@Composable
private fun AffirmationHeroCard(
    affirmation: AffirmationEntity?,
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
                text = "CURRENT AFFIRMATION",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = MaterialTheme.typography.labelMedium.letterSpacing,
            )

            AnimatedContent(
                targetState = affirmation?.text ?: "Add affirmations to begin your collection.",
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
private fun AffirmationCollectionCard(
    affirmation: AffirmationEntity,
    onFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
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
                    IconButton(onClick = onFavorite) {
                        Icon(
                            imageVector = if (affirmation.isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = "Favorite",
                            tint = if (affirmation.isFavorite) SereneTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
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
private fun BulkImportDialog(
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
                    text = "Paste one affirmation per line. Empty lines are ignored.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("I am calm and present…\nI choose peace over worry…") },
                    minLines = 8,
                )
                Text(
                    text = when (parsedCount) {
                        0 -> "No affirmations to import"
                        1 -> "1 affirmation ready to import"
                        else -> "$parsedCount affirmations ready to import"
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
