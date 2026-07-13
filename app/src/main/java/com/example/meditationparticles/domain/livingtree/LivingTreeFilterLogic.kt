package com.example.meditationparticles.domain.livingtree

import androidx.compose.ui.graphics.Color

object LivingTreeFilterLogic {
    fun visiblePersonIds(
        peopleTagIds: Map<Long, Set<Long>>,
        selectedTagIds: Set<Long>,
    ): Set<Long> {
        if (selectedTagIds.isEmpty()) return peopleTagIds.keys
        // Multi-tag selection is AND: person must have every selected tag.
        return peopleTagIds.filterValues { tagIds ->
            selectedTagIds.all { it in tagIds }
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
