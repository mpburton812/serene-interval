package com.example.meditationparticles.domain.mood

import com.example.meditationparticles.data.local.MoodEntryEntity
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
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<MoodGraphPoint> {
        val monthStart = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val monthEnd = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDate().minusDays(1)
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

    fun monthDomainMillis(
        startMillis: Long,
        endMillis: Long,
    ): Pair<Long, Long> = startMillis to (endMillis - 1).coerceAtLeast(startMillis)

    fun monthXFraction(
        xMillis: Long,
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Float {
        val monthStart = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val monthEndExclusive = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDate()
        val dayCount = monthStart.until(monthEndExclusive).days.coerceAtLeast(1)
        val day = Instant.ofEpochMilli(xMillis).atZone(zoneId).toLocalDate()
        val dayOffset = monthStart.until(day).days.coerceIn(0, dayCount - 1)
        return dayOffset.toFloat() / (dayCount - 1).coerceAtLeast(1).toFloat()
    }
}
