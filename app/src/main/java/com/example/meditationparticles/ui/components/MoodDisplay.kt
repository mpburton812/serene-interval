package com.example.meditationparticles.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import com.example.meditationparticles.domain.mood.MoodScale

private fun moodDisplayIcon(level: Int): ImageVector = when (MoodScale.migrateFromLegacy(level)) {
    1 -> Icons.Default.SentimentVeryDissatisfied
    2 -> Icons.Default.SentimentDissatisfied
    3 -> Icons.Default.SentimentNeutral
    4 -> Icons.Default.SentimentVerySatisfied
    else -> Icons.Default.SentimentVerySatisfied
}

@Composable
fun MoodDisplay(
    moodLevel: Int,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
) {
    val normalized = MoodScale.migrateFromLegacy(moodLevel)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = moodDisplayIcon(normalized),
            contentDescription = "Mood $normalized of ${MoodScale.MAX}",
            tint = moodColor(normalized),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = if (showLabel) {
                "Mood: $normalized/${MoodScale.MAX} (${MoodScale.label(normalized)})"
            } else {
                "$normalized/${MoodScale.MAX}"
            },
            style = MaterialTheme.typography.labelLarge,
            color = moodColor(normalized),
        )
    }
}
