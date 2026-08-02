package com.safehaven.affirmations.domain.mood

import com.safehaven.affirmations.data.local.MoodEntryEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class MoodMonthGraphMode {
    TOTAL_AVERAGE,
    ROLLING_7_DAY,
}

data class MoodGraphPoint(
    val xMillis: Long,
    val yLevel: Double,
)

object MoodGraphSeriesBuilder {
    fun entryPoints(entries: List<MoodEntryEntity>): List<MoodGraphPoint> =
        entries
            .sortedBy { it.recordedAtMillis }
            .map { MoodGraphPoint(it.recordedAtMillis, it.moodLevel.toDouble()) }

    /** One point per calendar day (noon) using that day's average mood — used for month total mode. */
    fun dailyAverageSeries(
        entries: List<MoodEntryEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<MoodGraphPoint> =
        entries
            .groupBy { entry ->
                Instant.ofEpochMilli(entry.recordedAtMillis).atZone(zoneId).toLocalDate()
            }
            .map { (day, dayEntries) ->
                MoodGraphPoint(
                    xMillis = day.atStartOfDay(zoneId).plusHours(12).toInstant().toEpochMilli(),
                    yLevel = dayEntries.map { it.moodLevel.toDouble() }.average(),
                )
            }
            .sortedBy { it.xMillis }

    fun dataSpanMillis(points: List<MoodGraphPoint>): Pair<Long, Long>? {
        if (points.isEmpty()) return null
        if (points.size == 1) {
            val only = points.first().xMillis
            return only to only
        }
        return points.first().xMillis to points.last().xMillis
    }

    fun xFraction(
        xMillis: Long,
        domainStartMillis: Long,
        domainEndMillis: Long,
        singlePointCentered: Boolean = false,
    ): Float {
        if (singlePointCentered && domainStartMillis == domainEndMillis) return 0.5f
        val range = (domainEndMillis - domainStartMillis).coerceAtLeast(1L)
        return ((xMillis - domainStartMillis).toFloat() / range).coerceIn(0f, 1f)
    }

    fun monthRollingAverageSeries(
        entries: List<MoodEntryEntity>,
        startMillis: Long,
        endMillis: Long,
        graphEndMillis: Long = endMillis,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<MoodGraphPoint> {
        val monthStart = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val scheduledMonthEnd = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDate().minusDays(1)
        val graphLastDay = Instant.ofEpochMilli(graphEndMillis - 1).atZone(zoneId).toLocalDate()
        val monthEnd = minOf(scheduledMonthEnd, graphLastDay)
        if (monthEnd.isBefore(monthStart)) return emptyList()

        val entriesByDay = entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.recordedAtMillis).atZone(zoneId).toLocalDate()
        }

        val points = mutableListOf<MoodGraphPoint>()
        var day = monthStart
        while (!day.isAfter(monthEnd)) {
            val windowStart = day.minusDays(6)
            val windowLevels = entriesByDay
                .filterKeys { date -> !date.isBefore(windowStart) && !date.isAfter(day) }
                .values
                .flatten()
                .map { it.moodLevel.toDouble() }
            if (windowLevels.isNotEmpty()) {
                val average = windowLevels.average()
                val xMillis = day.atStartOfDay(zoneId).plusHours(12).toInstant().toEpochMilli()
                points += MoodGraphPoint(xMillis, average)
            }
            day = day.plusDays(1)
        }
        return points
    }

    /** Full calendar period domain (inclusive end for plotting). Prefer [periodDomainMillis]. */
    fun monthDomainMillis(
        startMillis: Long,
        endMillis: Long,
    ): Pair<Long, Long> = periodDomainMillis(startMillis, endMillis)

    /** Inclusive plot domain covering [startMillis, endMillis). */
    fun periodDomainMillis(
        startMillis: Long,
        endMillis: Long,
    ): Pair<Long, Long> = startMillis to (endMillis - 1).coerceAtLeast(startMillis)

    /**
     * Maps a timestamp onto the full month width (day 1 … last day), regardless of "now".
     * Future days stay empty because callers filter points with [entriesWithinGraph].
     */
    fun monthXFraction(
        xMillis: Long,
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Float {
        val monthStart = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val daysInMonth = monthStart.lengthOfMonth().coerceAtLeast(1)
        val day = Instant.ofEpochMilli(xMillis).atZone(zoneId).toLocalDate()
        val dayOffset = (day.dayOfMonth - 1).coerceIn(0, daysInMonth - 1)
        return dayOffset.toFloat() / (daysInMonth - 1).coerceAtLeast(1).toFloat()
    }

    fun entriesWithinGraph(
        entries: List<MoodEntryEntity>,
        graphEndMillis: Long,
    ): List<MoodEntryEntity> = entries.filter { it.recordedAtMillis < graphEndMillis }
}
