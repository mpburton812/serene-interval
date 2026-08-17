package com.safehaven.affirmations.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.ui.theme.SereneSpacing

@Composable
fun JournalCaptureFields(
    text: String,
    onTextChange: (String) -> Unit,
    selectedMoodLevel: Int?,
    onMoodLevelChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    instructionText: String? = null,
    fieldLabel: String? = null,
    placeholder: String = "What's on your mind…",
    minLines: Int = 8,
    enabled: Boolean = true,
    showMood: Boolean = true,
    fillAvailableHeight: Boolean = false,
    compactChrome: Boolean = false,
) {
    Column(
        modifier = if (fillAvailableHeight) modifier.fillMaxSize() else modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
        if (!compactChrome) {
            instructionText?.let { instruction ->
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            fieldLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (showMood) {
                MoodPicker(
                    selectedLevel = selectedMoodLevel,
                    onLevelChange = onMoodLevelChange,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (fillAvailableHeight) {
                        Modifier
                            .weight(1f)
                            .heightIn(min = 128.dp)
                    } else {
                        Modifier.heightIn(min = 160.dp, max = 280.dp)
                    }
                ),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (fillAvailableHeight) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.verticalScroll(rememberScrollState())
                        }
                    ),
                placeholder = { Text(placeholder) },
                minLines = if (fillAvailableHeight) 1 else minLines,
                maxLines = 20,
                enabled = enabled,
            )
        }
    }
}
