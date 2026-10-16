package com.safehaven.affirmations.ui.components

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
import com.safehaven.affirmations.ui.mood.MoodPeriodNavigator
import com.safehaven.affirmations.ui.theme.SerenePrimary
import com.safehaven.affirmations.ui.theme.SereneSpacing

val CalendarHighlightColor: Color = SerenePrimary.copy(alpha = 0.85f)

@Composable
fun MonthCalendarCard(
    title: String,
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoForward: Boolean,
    weekdayHeaders: List<String>,
    weekCount: Int,
    dayContent: @Composable (weekIndex: Int, dayIndex: Int, modifier: Modifier) -> Unit,
    legend: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 20.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SereneSpacing.gutter),
            verticalArrangement = Arrangement.spacedBy(SereneSpacing.stackSm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            MoodPeriodNavigator(
                title = monthTitle,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
                canGoForward = canGoForward,
            )
            MonthCalendarGrid(
                weekdayHeaders = weekdayHeaders,
                weekCount = weekCount,
                dayContent = dayContent,
            )
            legend()
        }
    }
}

@Composable
fun MonthCalendarGrid(
    weekdayHeaders: List<String>,
    weekCount: Int,
    dayContent: @Composable (weekIndex: Int, dayIndex: Int, modifier: Modifier) -> Unit,
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
        repeat(weekCount) { weekIndex ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(7) { dayIndex ->
                    dayContent(weekIndex, dayIndex, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayOfMonth: Int,
    inMonth: Boolean,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = when {
        !inMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        highlighted -> CalendarHighlightColor
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
    }
    val textColor = when {
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        highlighted -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(SereneSpacing.stackSm))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

@Composable
fun CalendarLegend(
    highlightedLabel: String,
    emptyLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SereneSpacing.gutter, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarLegendSwatch(color = CalendarHighlightColor, label = highlightedLabel)
        CalendarLegendSwatch(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            label = emptyLabel,
        )
    }
}

@Composable
private fun CalendarLegendSwatch(color: Color, label: String) {
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
