package com.example.meditationparticles.domain.timer

import com.example.meditationparticles.domain.mood.MoodCalendarLogic
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

data class MeditationCalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val practiced: Boolean,
)

object MeditationCalendarLogic {
    fun buildMonthGrid(
        yearMonth: YearMonth,
        practicedDates: Set<LocalDate>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): List<MeditationCalendarDay> {
        val weekFields = java.time.temporal.WeekFields.of(locale)
        val firstOfMonth = yearMonth.atDay(1)
        val gridStart = firstOfMonth.with(weekFields.dayOfWeek(), 1)
        val lastOfMonth = yearMonth.atEndOfMonth()
        val gridEnd = lastOfMonth.with(weekFields.dayOfWeek(), 7)

        val days = mutableListOf<MeditationCalendarDay>()
        var cursor = gridStart
        while (!cursor.isAfter(gridEnd)) {
            days += MeditationCalendarDay(
                date = cursor,
                inMonth = cursor.month == yearMonth.month && cursor.year == yearMonth.year,
                practiced = cursor in practicedDates,
            )
            cursor = cursor.plusDays(1)
        }
        return days
    }

    fun practicedDatesFromCompletedAt(
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
