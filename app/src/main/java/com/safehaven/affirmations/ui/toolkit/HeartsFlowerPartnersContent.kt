package com.safehaven.affirmations.ui.toolkit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.data.local.HeartsEntryEntity
import com.safehaven.affirmations.domain.toolkit.HeartsToolConfig
import com.safehaven.affirmations.domain.toolkit.ToolkitTool
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.HistoryGlassCard
import com.safehaven.affirmations.ui.components.MoodDisplay
import com.safehaven.affirmations.ui.components.MoodPicker
import com.safehaven.affirmations.ui.theme.SereneSpacing
import java.util.Date

@Composable
fun HeartsToolkitContent(
    tool: ToolkitTool,
    stepIndex: Int,
    steps: List<String>,
    personName: String,
    selectedMoodLevel: Int?,
    entries: List<HeartsEntryEntity>,
    openedEntry: HeartsEntryEntity?,
    onPersonNameChange: (String) -> Unit,
    onMoodLevelChange: (Int?) -> Unit,
    onStepChange: (Int, String) -> Unit,
    onGoToStep: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onOpenEntry: (HeartsEntryEntity) -> Unit,
    onDeleteEntry: (HeartsEntryEntity) -> Unit,
    onCloseEntry: () -> Unit,
) {
    val stepDefinitions = HeartsToolConfig.steps(tool.id)
    val stepCount = stepDefinitions.size
    val isLastStep = stepIndex >= stepCount - 1
    val showPersonField = HeartsToolConfig.supportsPersonField(tool.id)
    val currentValue = heartsStepValue(
        stepIndex = stepIndex,
        showPersonField = showPersonField,
        personName = personName,
        steps = steps,
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
                text = stepDefinitions[stepIndex].hint,
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
                onStepChange = onGoToStep,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val definition = stepDefinitions[page]
                if (showPersonField && page == 0) {
                    OutlinedTextField(
                        value = personName,
                        onValueChange = onPersonNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(definition.label) },
                        placeholder = { Text("Name or connection") },
                        minLines = 1,
                    )
                } else {
                    val storageIndex = if (showPersonField) page - 1 else page
                    OutlinedTextField(
                        value = steps.getOrElse(storageIndex) { "" },
                        onValueChange = { onStepChange(storageIndex, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(definition.label) },
                        minLines = definition.minLines,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter),
            ) {
                OutlinedButton(
                    onClick = onPrevious,
                    enabled = stepIndex > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Previous")
                }
                if (isLastStep) {
                    Button(
                        onClick = onSave,
                        enabled = steps.any { it.isNotBlank() } || personName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save & Close")
                    }
                } else {
                    Button(
                        onClick = onNext,
                        enabled = currentValue.isNotBlank() || personName.isNotBlank(),
                        modifier = Modifier.weight(1f),
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
            TextButton(onClick = onClear) {
                Text("Clear draft")
            }
        }
    }

    if (entries.isNotEmpty()) {
        HistoryGlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(text = "Previous entries", style = MaterialTheme.typography.titleMedium)
                }
                entries.take(12).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEntry(entry) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entrySummaryPreview(entry),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = Date(entry.createdAt).toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        entry.moodLevel?.let { moodLevel ->
                            MoodDisplay(moodLevel = moodLevel, showLabel = false)
                        }
                    }
                }
            }
        }
    }

    openedEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = onCloseEntry,
            title = { Text(tool.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (entry.personName.isNotBlank()) {
                        Text("Connection", style = MaterialTheme.typography.labelMedium)
                        Text(entry.personName, style = MaterialTheme.typography.bodyMedium)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    stepDefinitions.forEachIndexed { index, definition ->
                        val value = if (showPersonField && index == 0) {
                            entry.personName
                        } else {
                            val storageIndex = if (showPersonField) index - 1 else index
                            entry.stepValues().getOrElse(storageIndex) { "" }
                        }
                        if (value.isNotBlank()) {
                            Text(definition.label, style = MaterialTheme.typography.labelMedium)
                            Text(value, style = MaterialTheme.typography.bodyMedium)
                            if (index < stepDefinitions.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                    entry.moodLevel?.let { moodLevel ->
                        MoodDisplay(moodLevel = moodLevel)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onCloseEntry) {
                    Text("Close")
                }
            },
            dismissButton = {
                IconButton(onClick = { onDeleteEntry(entry) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            },
        )
    }
}

@Composable
fun HeartsFlowerPartnersContent(
    partners: List<HeartsPartnerSummary>,
    onOpenDelightForPartner: (HeartsPartnerSummary) -> Unit,
    onOpenAttunementForPartner: (HeartsPartnerSummary) -> Unit,
    onOpenRepairForPartner: (HeartsPartnerSummary) -> Unit,
) {
    if (partners.isEmpty()) {
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
            ) {
                Text(
                    text = "No Flower partners yet",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Add people with the Partner tag in your Flower to track HEARTS touchpoints here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd)) {
        Text(
            text = "Partners from your Flower",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        partners.forEach { partner ->
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(partner.tagColorArgb)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = partner.personName,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = formatTouchpoint(partner),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        partner.lastHeartsLetter?.let { letter ->
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = { onOpenDelightForPartner(partner) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Delight", modifier = Modifier.padding(start = 4.dp))
                        }
                        TextButton(
                            onClick = { onOpenAttunementForPartner(partner) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Attune")
                        }
                        TextButton(
                            onClick = { onOpenRepairForPartner(partner) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Repair")
                        }
                    }
                }
            }
        }
    }
}

data class HeartsPartnerSummary(
    val personId: Long,
    val personName: String,
    val tagColorArgb: Int,
    val lastToolTitle: String?,
    val lastHeartsLetter: Char?,
    val lastSummary: String?,
    val lastTouchpointAt: Long?,
)

private fun heartsStepValue(
    stepIndex: Int,
    showPersonField: Boolean,
    personName: String,
    steps: List<String>,
): String {
    if (showPersonField && stepIndex == 0) return personName
    val storageIndex = if (showPersonField) stepIndex - 1 else stepIndex
    return steps.getOrElse(storageIndex) { "" }
}

private fun entrySummaryPreview(entry: HeartsEntryEntity): String {
    val body = entry.stepValues().firstOrNull { it.isNotBlank() }
    return when {
        entry.personName.isNotBlank() && !body.isNullOrBlank() -> "${entry.personName}: $body"
        entry.personName.isNotBlank() -> entry.personName
        !body.isNullOrBlank() -> body
        else -> "Entry"
    }
}

private fun formatTouchpoint(partner: HeartsPartnerSummary): String {
    if (partner.lastTouchpointAt == null) {
        return "No HEARTS touchpoints yet"
    }
    val date = java.text.DateFormat.getDateTimeInstance(
        java.text.DateFormat.MEDIUM,
        java.text.DateFormat.SHORT,
    ).format(Date(partner.lastTouchpointAt))
    val tool = partner.lastToolTitle ?: "Practice"
    val snippet = partner.lastSummary?.take(60)?.let { if (it.length == 60) "$it…" else it }
    return if (snippet.isNullOrBlank()) {
        "$tool · $date"
    } else {
        "$tool · $snippet · $date"
    }
}
