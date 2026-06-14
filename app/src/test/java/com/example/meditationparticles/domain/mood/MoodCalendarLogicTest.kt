package com.example.meditationparticles.domain.mood

import com.example.meditationparticles.data.local.MoodEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class MoodCalendarLogicTest {
    private val zoneId = ZoneId.of("America/New_York")
    private val locale = Locale.US

    @Test
    fun buildMonthGrid_colorsDaysWithDailyAverage() {
        val day = LocalDate.of(2026, 6, 10)
        val entries = listOf(
            MoodEntryEntity(
                id = 1L,
                moodLevel = 2,
                recordedAtMillis = day.atTime(9, 0).atZone(zoneId).toInstant().toEpochMilli(),
                source = "test",
                legacyTable = null,
                legacyRowId = null,
            ),
            MoodEntryEntity(
                id = 2L,
                moodLevel = 4,
                recordedAtMillis = day.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli(),
                source = "test",
                legacyTable = null,
                legacyRowId = null,
            ),
        )

        val grid = MoodCalendarLogic.buildMonthGrid(
            yearMonth = YearMonth.of(2026, 6),
            entries = entries,
            zoneId = zoneId,
            locale = locale,
        )

        val targetDay = grid.first { it.date == day }
        assertEquals(3.0, targetDay.average!!, 0.001)
        assertTrue(grid.any { !it.inMonth })
    }

    @Test
    fun buildMonthGrid_leavesDaysWithoutEntriesBlank() {
        val grid = MoodCalendarLogic.buildMonthGrid(
            yearMonth = YearMonth.of(2026, 6),
            entries = emptyList(),
            zoneId = zoneId,
            locale = locale,
        )

        assertTrue(grid.any { it.inMonth && it.average == null })
    }

    @Test
    fun weekdayHeaders_returnsSevenLabels() {
        assertEquals(7, MoodCalendarLogic.weekdayHeaders(locale).size)
    }
}
