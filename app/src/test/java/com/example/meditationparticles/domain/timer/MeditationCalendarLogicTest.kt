package com.example.meditationparticles.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MeditationCalendarLogicTest {
    private val zoneId = ZoneId.of("America/New_York")

    @Test
    fun buildMonthGrid_marksPracticedDaysGreenEligible() {
        val practiced = setOf(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 12))
        val grid = MeditationCalendarLogic.buildMonthGrid(
            yearMonth = YearMonth.of(2026, 6),
            practicedDates = practiced,
            zoneId = zoneId,
        )

        assertTrue(grid.first { it.date == LocalDate.of(2026, 6, 5) }.practiced)
        assertTrue(grid.first { it.date == LocalDate.of(2026, 6, 12) }.practiced)
        assertFalse(grid.first { it.date == LocalDate.of(2026, 6, 6) }.practiced)
    }

    @Test
    fun practicedDatesFromCompletedAt_groupsByLocalDate() {
        val day = LocalDate.of(2026, 6, 8)
        val millis = listOf(
            day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli(),
        )

        val dates = MeditationCalendarLogic.practicedDatesFromCompletedAt(millis, zoneId)

        assertEquals(setOf(day), dates)
    }

    @Test
    fun buildMonthGrid_includesLeadingAndTrailingPaddingDays() {
        val grid = MeditationCalendarLogic.buildMonthGrid(
            yearMonth = YearMonth.of(2026, 6),
            practicedDates = emptySet(),
            zoneId = zoneId,
        )

        assertTrue(grid.any { !it.inMonth })
        assertTrue(grid.size % 7 == 0)
        assertTrue(grid.size >= 28)
    }
}
