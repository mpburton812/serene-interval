package com.safehaven.affirmations.domain.mood

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MoodGraphAxisTick(
    val positionFraction: Float,
    val label: String,
    val level: Int = 0,
)

object MoodGraphAxisFormatter {
    fun yAxisTicks(): List<MoodGraphAxisTick> =
        (MoodScale.MIN..MoodScale.MAX).map { level ->
            val fraction = (level - MoodScale.MIN).toFloat() / (MoodScale.MAX - MoodScale.MIN).toFloat()
            MoodGraphAxisTick(
                positionFraction = fraction,
                label = MoodScale.label(level),
                level = level,
            )
        }

    fun xAxisTicks(
        period: MoodGraphPeriod,
        startMillis: Long,
        endMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale.getDefault(),
    ): List<MoodGraphAxisTick> {
        val rangeMillis = (endMillis - startMillis).coerceAtLeast(1L)
        return when (period) {
            MoodGraphPeriod.DAY -> dayTicks(startMillis, rangeMillis, zoneId, locale)
            MoodGraphPeriod.WEEK -> weekTicks(startMillis, rangeMillis, zoneId, locale)
            MoodGraphPeriod.MONTH,
            MoodGraphPeriod.CALENDAR,
            -> monthTicks(startMillis, rangeMillis, zoneId, locale)
        }
    }

    private fun dayTicks(
        startMillis: Long,
        rangeMillis: Long,
        zoneId: ZoneId,
        locale: Locale,
    ): List<MoodGraphAxisTick> {
        val formatter = DateTimeFormatter.ofPattern("h a", locale)
        val dayStart = Instant.ofEpochMilli(startMillis).atZone(zoneId)
        // Include end-of-day midnight so Today always spans 12 AM → 12 AM.
        return listOf(0, 6, 12, 18, 24).map { hour ->
            val tickTime = if (hour == 24) {
                dayStart.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            } else {
                dayStart.withHour(hour).withMinute(0).withSecond(0).withNano(0)
            }
            val tickMillis = tickTime.toInstant().toEpochMilli()
            MoodGraphAxisTick(
                positionFraction = ((tickMillis - startMillis).toFloat() / rangeMillis).coerceIn(0f, 1f),
                label = formatter.format(tickTime),
            )
        }
    }

    private fun weekTicks(
        startMillis: Long,
        rangeMillis: Long,
        zoneId: ZoneId,
        locale: Locale,
    ): List<MoodGraphAxisTick> {
        val formatter = DateTimeFormatter.ofPattern("EEE", locale)
        val weekStart = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val dayMillis = 24L * 60 * 60 * 1000
        return (0 until 7).map { dayOffset ->
            val day = weekStart.plusDays(dayOffset.toLong())
            val dayCenterMillis = startMillis + dayOffset * dayMillis + dayMillis / 2
            MoodGraphAxisTick(
                positionFraction = ((dayCenterMillis - startMillis).toFloat() / rangeMillis).coerceIn(0f, 1f),
                label = formatter.format(day),
            )
        }
    }

    private fun monthTicks(
        startMillis: Long,
        rangeMillis: Long,
        zoneId: ZoneId,
        locale: Locale,
    ): List<MoodGraphAxisTick> {
        val formatter = DateTimeFormatter.ofPattern("d", locale)
        val monthStart = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val monthLength = monthStart.lengthOfMonth()
        val tickDays = listOf(1, 8, 15, 22, 29).filter { it <= monthLength }
        return tickDays.map { dayOfMonth ->
            val tickDate = monthStart.withDayOfMonth(dayOfMonth)
            val tickMillis = tickDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            MoodGraphAxisTick(
                positionFraction = ((tickMillis - startMillis).toFloat() / rangeMillis).coerceIn(0f, 1f),
                label = formatter.format(tickDate),
            )
        }
    }
}
