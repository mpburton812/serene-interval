package com.example.meditationparticles.domain.mood

import org.junit.Assert.assertEquals
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
