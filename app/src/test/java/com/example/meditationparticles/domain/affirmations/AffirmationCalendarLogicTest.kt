package com.example.meditationparticles.domain.affirmations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class AffirmationCalendarLogicTest {
    private val zoneId = ZoneId.of("America/New_York")

    @Test
    fun buildMonthGrid_marksReviewedDays() {
        val reviewed = setOf(LocalDate.of(2026, 6, 5), LocalDate.of(2026, 6, 12))
        val grid = AffirmationCalendarLogic.buildMonthGrid(
            yearMonth = YearMonth.of(2026, 6),
            reviewedDates = reviewed,
            zoneId = zoneId,
        )

        assertTrue(grid.first { it.date == LocalDate.of(2026, 6, 5) }.reviewed)
        assertTrue(grid.first { it.date == LocalDate.of(2026, 6, 12) }.reviewed)
        assertFalse(grid.first { it.date == LocalDate.of(2026, 6, 6) }.reviewed)
    }

    @Test
    fun reviewedDatesFromCompletedAt_groupsByLocalDate() {
        val day = LocalDate.of(2026, 6, 8)
        val millis = listOf(
            day.atTime(8, 0).atZone(zoneId).toInstant().toEpochMilli(),
            day.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli(),
        )

        val dates = AffirmationCalendarLogic.reviewedDatesFromCompletedAt(millis, zoneId)

        assertEquals(setOf(day), dates)
    }

    @Test
    fun buildMonthGrid_includesLeadingAndTrailingPaddingDays() {
        val grid = AffirmationCalendarLogic.buildMonthGrid(
            yearMonth = YearMonth.of(2026, 6),
            reviewedDates = emptySet(),
            zoneId = zoneId,
        )

        assertTrue(grid.any { !it.inMonth })
        assertTrue(grid.size % 7 == 0)
        assertTrue(grid.size >= 28)
    }
}
