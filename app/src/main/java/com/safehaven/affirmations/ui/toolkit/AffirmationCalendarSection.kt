package com.safehaven.affirmations.ui.toolkit

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
import com.safehaven.affirmations.domain.affirmations.AffirmationCalendarDay
import com.safehaven.affirmations.ui.components.GlassCard
import com.safehaven.affirmations.ui.mood.MoodPeriodNavigator
import com.safehaven.affirmations.ui.theme.SerenePrimary
import com.safehaven.affirmations.ui.theme.SereneSpacing

private val ReviewedDayColor = SerenePrimary.copy(alpha = 0.85f)

@Composable
fun AffirmationCalendarSection(
    monthTitle: String,
    days: List<AffirmationCalendarDay>,
    weekdayHeaders: List<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoForward: Boolean = true,
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
                text = "Affirmation calendar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            MoodPeriodNavigator(
                title = monthTitle,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                canGoForward = canGoForward,
            )
            AffirmationCalendarGrid(
                days = days,
                weekdayHeaders = weekdayHeaders,
            )
            AffirmationCalendarLegend()
        }
    }
}

@Composable
private fun AffirmationCalendarGrid(
    days: List<AffirmationCalendarDay>,
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
                    AffirmationCalendarDayCell(
                        day = day,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AffirmationCalendarDayCell(
    day: AffirmationCalendarDay,
    modifier: Modifier = Modifier,
) {
    val background = when {
        !day.inMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        day.reviewed -> ReviewedDayColor
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val textColor = when {
        !day.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        day.reviewed -> Color.White
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
private fun AffirmationCalendarLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendSwatch(color = ReviewedDayColor, label = "Reviewed")
        LegendSwatch(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            label = "No review",
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
