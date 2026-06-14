package com.example.meditationparticles.domain.livingtree

object LivingTreeDefaults {
    data class DefaultTag(val name: String, val colorArgb: Int)

    private val defaultTagNames = listOf(
        "Family",
        "Partner",
        "Lover",
        "Friend",
        "Chosen Family",
        "Work",
        "Support",
        "Poly",
        "Queer",
        "Trans",
        "Long Distance",
        "Local",
    )

    // Twelve tags at 30° hue steps (360/12); S=68% L=48% for legibility on light/dark UI.
    private val defaultTagColors: List<Int> =
        LivingTreeColor.distinctDefaultTagColors(defaultTagNames.size)

    val defaultTags: List<DefaultTag> = defaultTagNames.mapIndexed { index, name ->
        DefaultTag(name = name, colorArgb = defaultTagColors[index])
    }

    val presetColors: List<Int> = defaultTagColors

    const val MAX_FILTER_TAGS = 4
    const val MAX_NAME_LENGTH = 48
    const val MAX_NOTES_LENGTH = 2_000

    fun centerLabel(preferredName: String): String =
        preferredName.trim().ifBlank { "You" }

    fun truncateName(name: String, maxLength: Int = 14): String =
        if (name.length <= maxLength) name else name.take(maxLength - 1) + "…"
}
