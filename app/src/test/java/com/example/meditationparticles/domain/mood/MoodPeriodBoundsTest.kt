package com.example.meditationparticles.domain.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

class MoodPeriodBoundsTest {
    private val zoneId = ZoneId.of("America/New_York")
    private val locale = Locale.US

    @Test
    fun dayBounds_useHalfOpenLocalDayRange() {
        val referenceMillis = LocalDate.of(2026, 6, 5)
            .atTime(15, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val bounds = moodPeriodBounds(
            period = MoodGraphPeriod.DAY,
            referenceMillis = referenceMillis,
            zoneId = zoneId,
            locale = locale,
        )

        val dayStart = LocalDate.of(2026, 6, 5).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextDayStart = LocalDate.of(2026, 6, 6).atStartOfDay(zoneId).toInstant().toEpochMilli()

        assertEquals(dayStart, bounds.startMillis)
        assertEquals(nextDayStart, bounds.endMillis)
        assertTrue(bounds.contains(referenceMillis))
        assertFalse(bounds.contains(nextDayStart))
    }

    @Test
    fun weekBounds_startOnLocaleWeekStart() {
        val referenceMillis = LocalDate.of(2026, 6, 5)
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val weekFields = WeekFields.of(locale)
        val weekStart = LocalDate.of(2026, 6, 5)
            .with(weekFields.dayOfWeek(), 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val nextWeekStart = LocalDate.of(2026, 6, 5)
            .with(weekFields.dayOfWeek(), 1)
            .plusWeeks(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val bounds = moodPeriodBounds(
            period = MoodGraphPeriod.WEEK,
            referenceMillis = referenceMillis,
            zoneId = zoneId,
            locale = locale,
        )

        assertEquals(weekStart, bounds.startMillis)
        assertEquals(nextWeekStart, bounds.endMillis)
        assertTrue(bounds.contains(referenceMillis))
        assertFalse(bounds.contains(nextWeekStart))
    }

    @Test
    fun monthBounds_useHalfOpenCalendarMonth() {
        val referenceMillis = LocalDate.of(2026, 6, 15)
            .atTime(9, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val monthStart = LocalDate.of(2026, 6, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextMonthStart = LocalDate.of(2026, 7, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val bounds = moodPeriodBounds(
            period = MoodGraphPeriod.MONTH,
            referenceMillis = referenceMillis,
            zoneId = zoneId,
            locale = locale,
        )

        assertEquals(monthStart, bounds.startMillis)
        assertEquals(nextMonthStart, bounds.endMillis)
        assertTrue(bounds.contains(referenceMillis))
        assertFalse(bounds.contains(nextMonthStart))
    }
}
