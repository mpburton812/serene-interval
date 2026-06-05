package com.example.meditationparticles.domain.livingtree

import androidx.compose.ui.graphics.Color

object LivingTreeDefaults {
    data class DefaultTag(val name: String, val colorArgb: Int)

    val defaultTags: List<DefaultTag> = listOf(
        DefaultTag("Family", 0xFF4A654E.toInt()),
        DefaultTag("Partner", 0xFFE07A5F.toInt()),
        DefaultTag("Lover", 0xFFC1666B.toInt()),
        DefaultTag("Friend", 0xFF3D5A80.toInt()),
        DefaultTag("Chosen Family", 0xFF9B5DE5.toInt()),
        DefaultTag("Work", 0xFF6B705C.toInt()),
        DefaultTag("Support", 0xFF5B8C5A.toInt()),
        DefaultTag("Poly", 0xFFE9B44C.toInt()),
        DefaultTag("Queer", 0xFFF7A8B8.toInt()),
        DefaultTag("Trans", 0xFF55CDFC.toInt()),
        DefaultTag("Long Distance", 0xFF9A8C98.toInt()),
        DefaultTag("Local", 0xFF81B29A.toInt()),
    )

    val presetColors: List<Int> = listOf(
        0xFF4A654E.toInt(),
        0xFF3D5A80.toInt(),
        0xFF457B9D.toInt(),
        0xFF81B29A.toInt(),
        0xFF5B8C5A.toInt(),
        0xFFE07A5F.toInt(),
        0xFFC1666B.toInt(),
        0xFF9B5DE5.toInt(),
        0xFF55CDFC.toInt(),
        0xFFF7A8B8.toInt(),
        0xFFE9B44C.toInt(),
        0xFF6B705C.toInt(),
        0xFF9A8C98.toInt(),
    )

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
