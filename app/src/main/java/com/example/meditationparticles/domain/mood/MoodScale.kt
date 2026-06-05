package com.example.meditationparticles.domain.mood

object MoodScale {
    const val MIN = 1
    const val MAX = 4
    const val LEGACY_MAX = 5

    const val COLOR_RED: Long = 0xFFE53935
    const val COLOR_YELLOW: Long = 0xFFF9A825
    const val COLOR_BLUE: Long = 0xFF1E88E5
    const val COLOR_GREEN: Long = 0xFF2E7D32

    fun migrateFromLegacy(level: Int): Int = when {
        level >= 5 -> MAX
        level < MIN -> MIN
        else -> level.coerceAtMost(MAX)
    }

    fun normalize(level: Int?): Int? = level?.let(::migrateFromLegacy)

    fun label(level: Int): String = when (migrateFromLegacy(level)) {
        1 -> "Red"
        2 -> "Yellow"
        3 -> "Blue"
        4 -> "Green"
        else -> "Green"
    }

    fun colorArgb(level: Int): Long = when (migrateFromLegacy(level)) {
        1 -> COLOR_RED
        2 -> COLOR_YELLOW
        3 -> COLOR_BLUE
        4 -> COLOR_GREEN
        else -> COLOR_GREEN
    }
}
