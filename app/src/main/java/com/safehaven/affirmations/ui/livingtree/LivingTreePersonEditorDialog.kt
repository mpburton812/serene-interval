package com.safehaven.affirmations.ui.livingtree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.data.local.LivingTreePersonWithTags
import com.safehaven.affirmations.data.local.LivingTreeTagEntity
import com.safehaven.affirmations.domain.livingtree.LivingTreePersonNames

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LivingTreePersonEditorDialog(
    existing: LivingTreePersonWithTags?,
    tags: List<LivingTreeTagEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, Set<Long>) -> Unit,
) {
    val isNew = existing == null
    var name by remember(existing) { mutableStateOf(existing?.person?.name ?: "") }
    var notes by remember(existing) { mutableStateOf(existing?.person?.notes ?: "") }
    var selectedTagIds by remember(existing) {
        mutableStateOf(existing?.tags?.map { it.id }?.toSet() ?: emptySet())
    }
    val parsedNames = remember(name) { LivingTreePersonNames.parse(name) }
    val isBulkAdd = isNew && parsedNames.size > 1
    val notesEnabled = !isBulkAdd

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isNew && parsedNames.size > 1 -> "New people"
                    isNew -> "New person or people"
                    else -> "Edit person"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isNew) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Names") },
                        placeholder = { Text("Alex, Jordan, Sam") },
                        supportingText = {
                            Text("Separate names with commas. Each name becomes a person with the selected tags.")
                        },
                        minLines = 3,
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (notesEnabled) notes = it },
                    label = { Text("Notes (optional)") },
                    enabled = notesEnabled,
                    supportingText = if (isBulkAdd) {
                        { Text("Notes apply to single-person add only.") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(text = "Tags", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.forEach { tag ->
                        val selected = tag.id in selectedTagIds
                        Text(
                            text = tag.name,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (selected) Color(tag.colorArgb).copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                                )
                                .clickable {
                                    selectedTagIds = selectedTagIds.toMutableSet().apply {
                                        if (selected) remove(tag.id) else add(tag.id)
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            color = if (selected) Color(tag.colorArgb) else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, notes, selectedTagIds) },
                enabled = parsedNames.isNotEmpty(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
