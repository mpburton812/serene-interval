package com.example.meditationparticles.domain.settings

enum class SanctuaryLandscapeThemeId(
    val label: String,
    val subtitle: String,
    val showInPicker: Boolean = true,
) {
    Beach("Beach", "Coastal calm"),
    Cabin("Cabin", "Woodland retreat"),
    Desert("Desert", "Warm sands"),
    Snowscape("Snowscape", "Quiet winter"),
    DeepWoods("Deep Woods", "Forest depth"),
    Moon("Moon", "Lunar stillness"),
    Space("Space", "Cosmic expanse"),
    Classic("Classic", "Original backgrounds", showInPicker = false),
    ;

    companion object {
        val pickerOptions: List<SanctuaryLandscapeThemeId> =
            entries.filter { it.showInPicker }

        fun fromStored(name: String?): SanctuaryLandscapeThemeId =
            runCatching {
                if (name.isNullOrBlank()) Classic else valueOf(name)
            }.getOrDefault(Classic)
    }
}
