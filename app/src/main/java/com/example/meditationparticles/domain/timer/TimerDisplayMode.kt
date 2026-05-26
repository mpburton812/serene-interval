package com.example.meditationparticles.domain.timer

enum class TimerDisplayMode(val label: String) {
    Digital("Digital"),
    Hourglass("Hourglass"),
    Blank("Blank"),
}

enum class TimerPhase {
    Idle,
    Prepare,
    Running,
    Complete,
}

enum class TimerSoundOption(val label: String) {
    None("None"),
    Rain("Rain"),
    Waves("Waves"),
    Forest("Forest"),
    ;

    companion object {
        private val removedOptionNames = setOf("Wind", "Bell", "Custom")

        fun fromStoredName(name: String?): TimerSoundOption {
            val normalized = when (name) {
                "Ocean" -> Waves.name
                in removedOptionNames -> None.name
                else -> name ?: None.name
            }
            return runCatching { valueOf(normalized) }.getOrDefault(None)
        }
    }
}

object TimerPresets {
    val minutes = listOf(5, 10, 15, 20, 25, 30)
    const val DEFAULT_MINUTES = 10
}
