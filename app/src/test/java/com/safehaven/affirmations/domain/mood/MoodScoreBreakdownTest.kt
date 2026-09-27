package com.safehaven.affirmations.domain.mood

import com.safehaven.affirmations.data.local.MoodEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class MoodScoreBreakdownTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun week_averagesEachDayThatHasData() {
        val monday = LocalDateTime.of(2026, 9, 21, 9, 0)
        val buckets = MoodScoreBreakdown.buckets(
            period = MoodGraphPeriod.WEEK,
            entries = listOf(
                entry(1, monday),
                entry(3, monday.plusHours(2)),
                entry(4, monday.plusDays(1).withHour(15)),
            ),
            zoneId = zone,
            locale = Locale.US,
        )
        assertEquals(listOf("Mon 21", "Tue 22"), buckets.map { it.label })
        assertEquals(2.0, buckets[0].average, 0.001)
        assertEquals(4.0, buckets[1].average, 0.001)
        assertEquals("Score by Week", MoodScoreBreakdown.heading(MoodGraphPeriod.WEEK))
    }

    @Test
    fun day_averagesEachHour() {
        val morning = LocalDateTime.of(2026, 9, 21, 9, 15)
        val buckets = MoodScoreBreakdown.buckets(
            period = MoodGraphPeriod.DAY,
            entries = listOf(
                entry(1, morning),
                entry(3, morning.plusMinutes(20)),
                entry(2, morning.withHour(14)),
            ),
            zoneId = zone,
            locale = Locale.US,
        )
        assertEquals(2, buckets.size)
        assertEquals(2.0, buckets[0].average, 0.001)
        assertEquals(2.0, buckets[1].average, 0.001)
    }

    @Test
    fun year_averagesEachMonth() {
        val january = LocalDateTime.of(2026, 1, 4, 12, 0)
        val buckets = MoodScoreBreakdown.buckets(
            period = MoodGraphPeriod.YEAR,
            entries = listOf(
                entry(1, january),
                entry(3, january.plusDays(10)),
                entry(4, january.withMonth(6)),
            ),
            zoneId = zone,
            locale = Locale.US,
        )
        assertEquals(listOf("January", "June"), buckets.map { it.label })
        assertEquals(2.0, buckets[0].average, 0.001)
        assertEquals(4.0, buckets[1].average, 0.001)
    }

    @Test
    fun yearBounds_coverCalendarYear() {
        val millis = LocalDateTime.of(2026, 9, 21, 12, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val bounds = moodPeriodBounds(MoodGraphPeriod.YEAR, millis, zone)
        val start = java.time.Instant.ofEpochMilli(bounds.startMillis).atZone(zone).toLocalDate()
        val end = java.time.Instant.ofEpochMilli(bounds.endMillis).atZone(zone).toLocalDate()
        assertEquals(java.time.LocalDate.of(2026, 1, 1), start)
        assertEquals(java.time.LocalDate.of(2027, 1, 1), end)
    }

    private fun entry(level: Int, time: LocalDateTime) = MoodEntryEntity(
        moodLevel = level,
        recordedAtMillis = time.atZone(zone).toInstant().toEpochMilli(),
        source = "HOME_SCREEN",
    )
}
