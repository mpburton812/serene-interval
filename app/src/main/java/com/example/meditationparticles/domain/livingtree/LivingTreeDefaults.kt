package com.example.meditationparticles.domain.livingtree

import androidx.compose.ui.graphics.Color

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

object LivingTreeFilterLogic {
    fun visiblePersonIds(
        peopleTagIds: Map<Long, Set<Long>>,
        selectedTagIds: Set<Long>,
    ): Set<Long> {
        if (selectedTagIds.isEmpty()) return peopleTagIds.keys
        return peopleTagIds.filterValues { tagIds ->
            tagIds.any { it in selectedTagIds }
        }.keys
    }

    fun matchedTagIds(
        personTagIds: Set<Long>,
        selectedTagIds: List<Long>,
    ): List<Long> =
        selectedTagIds.filter { it in personTagIds }

    fun bubbleColors(
        personTagIds: Set<Long>,
        selectedTagIds: List<Long>,
        tagColors: Map<Long, Int>,
    ): List<Color> =
        matchedTagIds(personTagIds, selectedTagIds)
            .mapNotNull { tagId -> tagColors[tagId]?.let(::Color) }

    fun tagPersonCounts(
        peopleTagIds: Map<Long, Set<Long>>,
        tagIds: List<Long>,
    ): Map<Long, Int> =
        tagIds.associateWith { tagId ->
            peopleTagIds.values.count { tagId in it }
        }
}
