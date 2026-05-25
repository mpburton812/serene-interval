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
    Wind("Wind"),
    Bell("Soft Bell"),
    Custom("Custom"),
}

object TimerPresets {
    val minutes = listOf(5, 10, 15, 20, 25, 30)
    const val DEFAULT_MINUTES = 10
}
