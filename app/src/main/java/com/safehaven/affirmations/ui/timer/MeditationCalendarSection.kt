package com.safehaven.affirmations.ui.timer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.safehaven.affirmations.R
import com.safehaven.affirmations.domain.timer.MeditationCalendarDay
import com.safehaven.affirmations.ui.components.CalendarDayCell
import com.safehaven.affirmations.ui.components.CalendarLegend
import com.safehaven.affirmations.ui.components.MonthCalendarCard

@Composable
fun MeditationCalendarSection(
    monthTitle: String,
    days: List<MeditationCalendarDay>,
    weekdayHeaders: List<String>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoForward: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val weeks = days.chunked(7)
    MonthCalendarCard(
        title = stringResource(R.string.calendar_meditation_title),
        monthTitle = monthTitle,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        canGoForward = canGoForward,
        weekdayHeaders = weekdayHeaders,
        weekCount = weeks.size,
        dayContent = { weekIndex, dayIndex, cellModifier ->
            val day = weeks.getOrNull(weekIndex)?.getOrNull(dayIndex) ?: return@MonthCalendarCard
            CalendarDayCell(
                dayOfMonth = day.date.dayOfMonth,
                inMonth = day.inMonth,
                highlighted = day.practiced,
                modifier = cellModifier,
            )
        },
        legend = {
            CalendarLegend(
                highlightedLabel = stringResource(R.string.calendar_meditated),
                emptyLabel = stringResource(R.string.calendar_no_session),
            )
        },
        modifier = modifier,
    )
}
