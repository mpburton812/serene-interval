package com.safehaven.affirmations.domain.thermometer

object ThermometerRules {
    const val MAX_ACTIVE = 5
    const val MIN_SCORE = 0
    const val MAX_SCORE = 100

    fun canCreate(activeCount: Int): Boolean = activeCount < MAX_ACTIVE

    fun clampScore(score: Int): Int = score.coerceIn(MIN_SCORE, MAX_SCORE)
}
