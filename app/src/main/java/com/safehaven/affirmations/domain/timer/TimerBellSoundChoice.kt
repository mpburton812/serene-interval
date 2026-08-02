package com.safehaven.affirmations.domain.timer

enum class TimerBellSoundChoice(val label: String) {
    Default("Default"),
    SystemUri("Choose sound…"),
    ;

    companion object {
        fun fromStoredName(name: String?): TimerBellSoundChoice =
            runCatching { valueOf(name ?: Default.name) }.getOrDefault(Default)
    }
}
