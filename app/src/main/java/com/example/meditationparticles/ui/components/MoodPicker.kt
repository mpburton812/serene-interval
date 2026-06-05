package com.example.meditationparticles.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.mood.MoodScale

fun moodColor(level: Int): Color = Color(MoodScale.colorArgb(level))

fun moodIcon(level: Int): ImageVector = when (MoodScale.migrateFromLegacy(level)) {
    1 -> Icons.Default.SentimentVeryDissatisfied
    2 -> Icons.Default.SentimentDissatisfied
    3 -> Icons.Default.SentimentNeutral
    4 -> Icons.Default.SentimentVerySatisfied
    else -> Icons.Default.SentimentVerySatisfied
}

@Composable
fun MoodPicker(
    selectedLevel: Int?,
    onLevelChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Mood (optional)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            (MoodScale.MIN..MoodScale.MAX).forEach { value ->
                val selected = value == selectedLevel
                Icon(
                    imageVector = moodIcon(value),
                    contentDescription = "Mood $value of ${MoodScale.MAX}",
                    tint = if (selected) moodColor(value) else MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .size(28.dp)
                        .selectable(
                            selected = selected,
                            onClick = {
                                onLevelChange(if (selected) null else value)
                            },
                            role = Role.RadioButton,
                        ),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Sad",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Happy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
