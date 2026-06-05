package com.example.meditationparticles.domain.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class MoodGraphAxisFormatterTest {
    private val zoneId = ZoneId.of("America/New_York")
    private val locale = Locale.US

    @Test
    fun yAxisTicks_useMoodScaleLabelsForLevelsOneThroughFour() {
        val ticks = MoodGraphAxisFormatter.yAxisTicks()

        assertEquals(4, ticks.size)
        assertEquals("Red", ticks[0].label)
        assertEquals("Yellow", ticks[1].label)
        assertEquals("Blue", ticks[2].label)
        assertEquals("Green", ticks[3].label)
        assertEquals(0f, ticks[0].positionFraction, 0.001f)
        assertEquals(1f, ticks[3].positionFraction, 0.001f)
    }

    @Test
    fun dayXAxisTicks_useHourLabelsAcrossDay() {
        val startMillis = LocalDate.of(2026, 6, 5)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val endMillis = LocalDate.of(2026, 6, 6)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val ticks = MoodGraphAxisFormatter.xAxisTicks(
            period = MoodGraphPeriod.DAY,
            startMillis = startMillis,
            endMillis = endMillis,
            zoneId = zoneId,
            locale = locale,
        )

        assertEquals(4, ticks.size)
        assertEquals("12 AM", ticks[0].label)
        assertEquals("6 AM", ticks[1].label)
        assertEquals("12 PM", ticks[2].label)
        assertEquals("6 PM", ticks[3].label)
        assertTrue(ticks.all { it.positionFraction in 0f..1f })
    }

    @Test
    fun weekXAxisTicks_useDayOfWeekLabels() {
        val startMillis = LocalDate.of(2026, 6, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val endMillis = LocalDate.of(2026, 6, 8)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val ticks = MoodGraphAxisFormatter.xAxisTicks(
            period = MoodGraphPeriod.WEEK,
            startMillis = startMillis,
            endMillis = endMillis,
            zoneId = zoneId,
            locale = locale,
        )

        assertEquals(7, ticks.size)
        assertEquals("Mon", ticks[0].label)
        assertEquals("Sun", ticks[6].label)
        assertTrue(ticks.all { it.positionFraction in 0f..1f })
    }

    @Test
    fun monthXAxisTicks_useDayOfMonthLabels() {
        val startMillis = LocalDate.of(2026, 6, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val endMillis = LocalDate.of(2026, 7, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val ticks = MoodGraphAxisFormatter.xAxisTicks(
            period = MoodGraphPeriod.MONTH,
            startMillis = startMillis,
            endMillis = endMillis,
            zoneId = zoneId,
            locale = locale,
        )

        assertEquals(listOf("1", "8", "15", "22", "29"), ticks.map { it.label })
        assertTrue(ticks.all { it.positionFraction in 0f..1f })
    }
}
