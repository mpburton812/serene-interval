package com.safehaven.affirmations.ui.mood

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.domain.mood.MoodGraphPeriod
import com.safehaven.affirmations.domain.mood.MoodPeriodAverages
import com.safehaven.affirmations.domain.mood.MoodScale
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.components.moodColor
import com.safehaven.affirmations.ui.components.moodIcon
import com.safehaven.affirmations.ui.theme.SereneSpacing

@Composable
fun MoodSummariesSection(
    averages: MoodPeriodAverages,
    onPeriodClick: (MoodGraphPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
    ) {
        Text(
            text = "Summaries",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MoodSummaryFace(
                    label = "Day",
                    average = averages.day,
                    onClick = { onPeriodClick(MoodGraphPeriod.DAY) },
                )
                MoodSummaryFace(
                    label = "Week",
                    average = averages.week,
                    onClick = { onPeriodClick(MoodGraphPeriod.WEEK) },
                )
                MoodSummaryFace(
                    label = "Month",
                    average = averages.month,
                    onClick = { onPeriodClick(MoodGraphPeriod.MONTH) },
                )
                MoodSummaryCalendar(
                    onClick = { onPeriodClick(MoodGraphPeriod.CALENDAR) },
                )
            }
        }
    }
}

@Composable
private fun MoodSummaryFace(
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

@Composable
private fun MoodSummaryCalendar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = "Calendar mood summary",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = "Calendar",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
