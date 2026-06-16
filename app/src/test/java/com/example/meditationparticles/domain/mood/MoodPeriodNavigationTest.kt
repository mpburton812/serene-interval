package com.example.meditationparticles.domain.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class MoodPeriodNavigationTest {
    private val zoneId = ZoneId.of("America/New_York")
    private val locale = Locale.US

    @Test
    fun canShiftPeriodForward_blocksFutureNavigation() {
        assertTrue(canShiftPeriodForward(-1))
        assertFalse(canShiftPeriodForward(0))
        assertFalse(canShiftPeriodForward(1))
    }

    @Test
    fun clampPeriodOffset_preventsFutureOffsets() {
        assertEquals(0, clampPeriodOffset(0))
        assertEquals(0, clampPeriodOffset(2))
        assertEquals(-3, clampPeriodOffset(-3))
    }

    @Test
    fun effectiveGraphEndMillis_clipsCurrentPeriodToNow() {
        val zoneId = ZoneId.of("America/New_York")
        val today = LocalDate.now(zoneId)
        val fullEnd = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val now = today.atTime(15, 0).atZone(zoneId).toInstant().toEpochMilli()

        val clipped = effectiveGraphEndMillis(
            period = MoodGraphPeriod.DAY,
            offset = 0,
            fullEndMillis = fullEnd,
            zoneId = zoneId,
            nowMillis = now,
        )

        assertEquals(now, clipped)
    }

    @Test
    fun effectiveGraphEndMillis_keepsFullRangeForPastPeriods() {
        val zoneId = ZoneId.of("America/New_York")
        val fullEnd = LocalDate.of(2026, 5, 2).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val end = effectiveGraphEndMillis(
            period = MoodGraphPeriod.DAY,
            offset = -1,
            fullEndMillis = fullEnd,
            zoneId = zoneId,
            nowMillis = System.currentTimeMillis(),
        )

        assertEquals(fullEnd, end)
    }

    @Test
    fun moodPeriodTitle_dayOffsetLabels() {
        assertEquals("Today", moodPeriodTitle(MoodGraphPeriod.DAY, 0, zoneId, locale))
        assertEquals("Yesterday", moodPeriodTitle(MoodGraphPeriod.DAY, -1, zoneId, locale))
    }

    @Test
    fun moodPeriodTitle_monthOffsetLabels() {
        assertEquals("This Month", moodPeriodTitle(MoodGraphPeriod.MONTH, 0, zoneId, locale))
        val previous = moodPeriodTitle(MoodGraphPeriod.MONTH, -1, zoneId, locale)
        assertNotEquals("This Month", previous)
    }

    @Test
    fun periodReferenceMillis_shiftsByOffset() {
        val todayMillis = periodReferenceMillis(MoodGraphPeriod.DAY, 0, zoneId, locale)
        val yesterdayMillis = periodReferenceMillis(MoodGraphPeriod.DAY, -1, zoneId, locale)
        val today = LocalDate.now(zoneId)
        val expectedYesterday = today.minusDays(1).atStartOfDay(zoneId).plusHours(12).toInstant().toEpochMilli()

        assertEquals(expectedYesterday, yesterdayMillis)
        assertTrue(todayMillis > yesterdayMillis)
    }
}
