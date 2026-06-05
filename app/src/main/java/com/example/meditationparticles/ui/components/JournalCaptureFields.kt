package com.example.meditationparticles.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.meditationparticles.ui.theme.SereneSpacing

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
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackMd),
    ) {
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
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = minLines,
            enabled = enabled,
        )
    }
}
