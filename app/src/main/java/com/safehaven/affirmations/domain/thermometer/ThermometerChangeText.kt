package com.safehaven.affirmations.domain.thermometer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ThermometerReadingRef(
    val id: Long,
    val thermometerId: Long,
    val score: Int,
    val recordedAt: Long,
)

object ThermometerChangeText {
    const val ACTIVITY_TITLE = "Thermometer:"

    fun previousScores(readings: List<ThermometerReadingRef>): Map<Long, Int> {
        val previous = mutableMapOf<Long, Int>()
        readings.groupBy { it.thermometerId }.values.forEach { group ->
            val ordered = group.sortedWith(
                compareBy<ThermometerReadingRef> { it.recordedAt }.thenBy { it.id },
            )
            var prior: Int? = null
            ordered.forEach { reading ->
                prior?.let { previous[reading.id] = it }
                prior = reading.score
            }
        }
        return previous
    }

    fun activitySubtitle(
        name: String,
        fromScore: Int?,
        toScore: Int,
        reason: String?,
        lowerPlan: String?,
    ): String {
        val reasonText = reason?.trim().orEmpty()
        val lowerText = lowerPlan?.trim().orEmpty()
        return buildString {
            append(name)
            append(" - stress from ")
            append(fromScore?.toString() ?: "—")
            append(" to ")
            append(toScore)
            if (reasonText.isNotEmpty()) {
                append(", ")
                append(reasonText)
            }
            if (lowerText.isNotEmpty()) {
                append(". How to lower: ")
                append(lowerText)
            }
        }
    }

    fun entryText(reason: String?, lowerPlan: String?): String {
        val reasonText = reason?.trim().orEmpty()
        val lowerText = lowerPlan?.trim().orEmpty()
        return buildList {
            if (reasonText.isNotEmpty()) add(reasonText)
            if (lowerText.isNotEmpty()) add("How to lower: $lowerText")
        }.joinToString("\n")
    }

    fun shouldSaveOnClose(
        baselineScore: Int?,
        score: Int,
        reason: String,
        lowerPlan: String,
        scoreTouched: Boolean,
    ): Boolean {
        if (reason.isNotBlank() || lowerPlan.isNotBlank()) return true
        if (!scoreTouched) return false
        val clamped = ThermometerRules.clampScore(score)
        return baselineScore == null || baselineScore != clamped
    }
}

object ThermometerGraphStamps {
    fun labels(
        recordedAt: List<Long>,
        locale: Locale = Locale.getDefault(),
        zone: java.util.TimeZone = java.util.TimeZone.getDefault(),
    ): List<String> {
        val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }
        val counts = recordedAt.groupingBy { dayKey.format(Date(it)) }.eachCount()
        val dateOnly = SimpleDateFormat("M/d", locale).apply { timeZone = zone }
        val dateTime = SimpleDateFormat("M/d h:mma", locale).apply { timeZone = zone }
        return recordedAt.map { at ->
            val showTime = counts.getValue(dayKey.format(Date(at))) > 1
            (if (showTime) dateTime else dateOnly).format(Date(at))
        }
    }
}

enum class ThermometerLiquid {
    RED,
    BLUE,
    ;

    companion object {
        fun fromStored(value: String?): ThermometerLiquid =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: RED
    }
}
