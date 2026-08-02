package com.safehaven.affirmations.domain.affirmations

import com.safehaven.affirmations.domain.mood.MoodCalendarLogic
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

data class AffirmationCalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val reviewed: Boolean,
)

object AffirmationCalendarLogic {
    fun buildMonthGrid(
        yearMonth: YearMonth,
        reviewedDates: Set<LocalDate>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): List<AffirmationCalendarDay> {
        val weekFields = java.time.temporal.WeekFields.of(locale)
        val firstOfMonth = yearMonth.atDay(1)
        val gridStart = firstOfMonth.with(weekFields.dayOfWeek(), 1)
        val lastOfMonth = yearMonth.atEndOfMonth()
        val gridEnd = lastOfMonth.with(weekFields.dayOfWeek(), 7)

        val days = mutableListOf<AffirmationCalendarDay>()
        var cursor = gridStart
        while (!cursor.isAfter(gridEnd)) {
            days += AffirmationCalendarDay(
                date = cursor,
                inMonth = cursor.month == yearMonth.month && cursor.year == yearMonth.year,
                reviewed = cursor in reviewedDates,
            )
            cursor = cursor.plusDays(1)
        }
        return days
    }

    fun reviewedDatesFromCompletedAt(
        completedAtMillis: List<Long>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Set<LocalDate> = completedAtMillis.map { millis ->
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    }.toSet()

    fun yearMonthFromOffset(
        offset: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): YearMonth = MoodCalendarLogic.yearMonthFromOffset(offset, zoneId)
}
