package com.safehaven.affirmations.domain.mood

import com.safehaven.affirmations.data.local.MoodEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

data class MoodCalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val average: Double?,
)

object MoodCalendarLogic {
    fun buildMonthGrid(
        yearMonth: YearMonth,
        entries: List<MoodEntryEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): List<MoodCalendarDay> {
        val averagesByDay = entries
            .groupBy { entry ->
                Instant.ofEpochMilli(entry.recordedAtMillis).atZone(zoneId).toLocalDate()
            }
            .mapValues { (_, dayEntries) -> dayEntries.map { it.moodLevel.toDouble() }.average() }

        val weekFields = WeekFields.of(locale)
        val firstOfMonth = yearMonth.atDay(1)
        val gridStart = firstOfMonth.with(weekFields.dayOfWeek(), 1)
        val lastOfMonth = yearMonth.atEndOfMonth()
        val gridEnd = lastOfMonth.with(weekFields.dayOfWeek(), 7)

        val days = mutableListOf<MoodCalendarDay>()
        var cursor = gridStart
        while (!cursor.isAfter(gridEnd)) {
            days += MoodCalendarDay(
                date = cursor,
                inMonth = cursor.month == yearMonth.month && cursor.year == yearMonth.year,
                average = averagesByDay[cursor],
            )
            cursor = cursor.plusDays(1)
        }
        return days
    }

    fun yearMonthFromOffset(
        offset: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): YearMonth {
        val today = LocalDate.now(zoneId)
        val reference = today.plusMonths(offset.toLong())
        return YearMonth.of(reference.year, reference.month)
    }

    fun weekdayHeaders(locale: Locale = Locale.getDefault()): List<String> {
        val weekFields = WeekFields.of(locale)
        val firstDay = weekFields.firstDayOfWeek
        return (0 until 7).map { offset ->
            firstDay.plus(offset.toLong())
                .getDisplayName(java.time.format.TextStyle.SHORT, locale)
        }
    }
}
