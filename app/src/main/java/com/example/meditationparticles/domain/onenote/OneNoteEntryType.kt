package com.example.meditationparticles.domain.onenote

enum class OneNoteEntryType(val displayName: String) {
    NVC("Non-Violent Communication"),
    REFACTORING("Refactoring"),
    CENTER_OF_GRAVITY("Center of Gravity"),
    THOUGHT_DUMP("Thought Dump"),
    ANXIETY_LOG("Anxiety Log"),
    FUTURE_SELF("Future Self"),
    MEDITATION_REFLECTION("Meditation Reflection"),
}

enum class OneNoteSyncStatus {
    PENDING,
    SYNCED,
    FAILED,
}
