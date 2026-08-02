package com.safehaven.affirmations.domain.mood

import com.safehaven.affirmations.data.local.MoodEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MoodGraphSeriesBuilderTest {
    private val zoneId = ZoneId.of("America/New_York")

    @Test
    fun dataSpanMillis_usesFirstAndLastEntryTimes() {
        val points = listOf(
            MoodGraphPoint(xMillis = 1000L, yLevel = 2.0),
            MoodGraphPoint(xMillis = 5000L, yLevel = 3.0),
        )

        val span = MoodGraphSeriesBuilder.dataSpanMillis(points)

        assertEquals(1000L to 5000L, span)
    }

    @Test
    fun xFraction_spansSinglePointAtCenter() {
        val fraction = MoodGraphSeriesBuilder.xFraction(
            xMillis = 500L,
            domainStartMillis = 500L,
            domainEndMillis = 500L,
            singlePointCentered = true,
        )

        assertEquals(0.5f, fraction, 0.001f)
    }

    @Test
    fun xFraction_spansMultiplePointsEdgeToEdge() {
        assertEquals(
            0f,
            MoodGraphSeriesBuilder.xFraction(1000L, 1000L, 4000L),
            0.001f,
        )
        assertEquals(
            1f,
            MoodGraphSeriesBuilder.xFraction(4000L, 1000L, 4000L),
            0.001f,
        )
    }

    @Test
    fun monthRollingAverageSeries_computesSevenDayWindow() {
        val day = LocalDate.of(2026, 6, 10)
        val startMillis = day.withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = day.withDayOfMonth(1).plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val entries = listOf(
            entry(day.minusDays(2), 2),
            entry(day.minusDays(1), 4),
            entry(day, 4),
        )

        val series = MoodGraphSeriesBuilder.monthRollingAverageSeries(
            entries = entries,
            startMillis = startMillis,
            endMillis = endMillis,
            zoneId = zoneId,
        )

        assertTrue(series.isNotEmpty())
        val juneTenth = day.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
        val tenthPoint = series.first { it.xMillis == juneTenth }
        assertEquals((2.0 + 4.0 + 4.0) / 3.0, tenthPoint.yLevel, 0.01)
    }

    @Test
    fun monthRollingAverageSeries_stopsAtGraphEndForCurrentMonth() {
        val zoneId = ZoneId.of("America/New_York")
        val monthStart = LocalDate.of(2026, 6, 1)
        val startMillis = monthStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = monthStart.plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val graphEndMillis = LocalDate.of(2026, 6, 5)
            .atTime(23, 59)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val entries = listOf(
            entry(LocalDate.of(2026, 6, 1), 2),
            entry(LocalDate.of(2026, 6, 5), 4),
            entry(LocalDate.of(2026, 6, 20), 1),
        )

        val series = MoodGraphSeriesBuilder.monthRollingAverageSeries(
            entries = entries,
            startMillis = startMillis,
            endMillis = endMillis,
            graphEndMillis = graphEndMillis,
            zoneId = zoneId,
        )

        assertTrue(series.none { point ->
            val day = java.time.Instant.ofEpochMilli(point.xMillis).atZone(zoneId).toLocalDate()
            day.isAfter(LocalDate.of(2026, 6, 5))
        })
    }

    @Test
    fun entriesWithinGraph_excludesFutureEntries() {
        val entries = listOf(
            MoodEntryEntity(1, 2, 1000L, "test", null, null),
            MoodEntryEntity(2, 3, 5000L, "test", null, null),
        )

        val visible = MoodGraphSeriesBuilder.entriesWithinGraph(entries, graphEndMillis = 3000L)

        assertEquals(listOf(1L), visible.map { it.id })
    }

    private fun entry(date: LocalDate, level: Int) = MoodEntryEntity(
        id = date.toEpochDay(),
        moodLevel = level,
        recordedAtMillis = date.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(),
        source = "test",
        legacyTable = null,
        legacyRowId = null,
    )
}
