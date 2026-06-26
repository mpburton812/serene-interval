package com.example.meditationparticles.domain.backup

enum class AutoBackupFrequency(val label: String, val intervalDays: Long) {
    Daily("Daily", 1),
    Weekly("Weekly", 7),
    ;

    companion object {
        fun fromStored(name: String?): AutoBackupFrequency =
            runCatching {
                if (name.isNullOrBlank()) Weekly else valueOf(name)
            }.getOrDefault(Weekly)
    }
}
