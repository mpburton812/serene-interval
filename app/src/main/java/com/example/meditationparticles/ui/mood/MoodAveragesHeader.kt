package com.example.meditationparticles.ui.mood

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.meditationparticles.domain.mood.MoodGraphPeriod
import com.example.meditationparticles.domain.mood.MoodPeriodAverages
import com.example.meditationparticles.domain.mood.MoodScale
import com.example.meditationparticles.ui.components.moodColor
import com.example.meditationparticles.ui.components.moodIcon
import com.example.meditationparticles.ui.theme.SereneSpacing

@Composable
fun MoodAveragesHeader(
    averages: MoodPeriodAverages,
    onPeriodClick: (MoodGraphPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SereneSpacing.containerMargin),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MoodAverageFace(
            label = "Day",
            average = averages.day,
            onClick = { onPeriodClick(MoodGraphPeriod.DAY) },
        )
        MoodAverageFace(
            label = "Week",
            average = averages.week,
            onClick = { onPeriodClick(MoodGraphPeriod.WEEK) },
        )
        MoodAverageFace(
            label = "Month",
            average = averages.month,
            onClick = { onPeriodClick(MoodGraphPeriod.MONTH) },
        )
    }
}

@Composable
private fun MoodAverageFace(
    label: String,
    average: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val moodLevel = MoodScale.averageToLevel(average)
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (moodLevel != null) {
            Icon(
                imageVector = moodIcon(moodLevel),
                contentDescription = "$label average mood",
                tint = moodColor(moodLevel),
                modifier = Modifier.size(36.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Default.SentimentNeutral,
                contentDescription = "$label average mood unavailable",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
