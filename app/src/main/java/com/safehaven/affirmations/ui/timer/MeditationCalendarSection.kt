package com.safehaven.affirmations.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safehaven.affirmations.domain.timer.MeditationCalendarDay
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.mood.MoodPeriodNavigator
import com.safehaven.affirmations.ui.theme.SerenePrimary
import com.safehaven.affirmations.ui.theme.SereneSpacing

private val PracticedDayColor = SerenePrimary.copy(alpha = 0.85f)

@Composable
fun MeditationCalendarSection(
    monthTitle: String,
    days: List<MeditationCalendarDay>,
    weekdayHeaders: List<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
        ) {
            Text(
                text = "Meditation calendar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            MoodPeriodNavigator(
                title = monthTitle,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
            )
            MeditationCalendarGrid(
                days = days,
                weekdayHeaders = weekdayHeaders,
            )
            MeditationCalendarLegend()
        }
    }
}

@Composable
private fun MeditationCalendarGrid(
    days: List<MeditationCalendarDay>,
    weekdayHeaders: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayHeaders.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { day ->
                    MeditationCalendarDayCell(
                        day = day,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MeditationCalendarDayCell(
    day: MeditationCalendarDay,
    modifier: Modifier = Modifier,
) {
    val background = when {
        !day.inMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        day.practiced -> PracticedDayColor
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val textColor = when {
        !day.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        day.practiced -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
private fun MeditationCalendarLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendSwatch(color = PracticedDayColor, label = "Meditated")
        LegendSwatch(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            label = "No session",
        )
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
