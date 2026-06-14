package com.example.meditationparticles.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MoodEntryIcon(
    moodLevel: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String = "Entry mood",
) {
    val level = moodLevel ?: return
    Icon(
        imageVector = moodIcon(level),
        contentDescription = contentDescription,
        tint = moodColor(level),
        modifier = modifier.size(size),
    )
}
