package com.example.meditationparticles.domain.mood

enum class MoodSource(val dbValue: String) {
    HOME_SCREEN("HOME_SCREEN"),
    WIDGET("WIDGET"),
    MEDITATION_REFLECTION("MEDITATION_REFLECTION"),
    THOUGHT_DUMP("THOUGHT_DUMP"),
    ANXIETY_LOG("ANXIETY_LOG"),
    FUTURE_SELF("FUTURE_SELF"),
    REFACTORING("REFACTORING"),
    CENTER_OF_GRAVITY("CENTER_OF_GRAVITY"),
    NVC("NVC"),
    ;

    companion object {
        fun fromDbValue(value: String): MoodSource? =
            entries.find { it.dbValue == value }
    }
}
