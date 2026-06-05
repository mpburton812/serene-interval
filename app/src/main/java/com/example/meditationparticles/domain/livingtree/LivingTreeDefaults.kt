package com.example.meditationparticles.domain.livingtree

import androidx.compose.ui.graphics.Color

object LivingTreeDefaults {
    data class DefaultTag(val name: String, val colorArgb: Int)

    val defaultTags: List<DefaultTag> = listOf(
        DefaultTag("Family", 0xFF4A654E.toInt()),
        DefaultTag("Partner", 0xFFE07A5F.toInt()),
        DefaultTag("Friends", 0xFF3D5A80.toInt()),
        DefaultTag("Work", 0xFF6B705C.toInt()),
        DefaultTag("Community", 0xFF81B29A.toInt()),
        DefaultTag("Support", 0xFF5B8C5A.toInt()),
        DefaultTag("Anchor", 0xFF457B9D.toInt()),
        DefaultTag("Trigger", 0xFFC1666B.toInt()),
        DefaultTag("Distance", 0xFF9A8C98.toInt()),
        DefaultTag("Chosen family", 0xFF9B5DE5.toInt()),
        DefaultTag("Trans family", 0xFF55CDFC.toInt()),
        DefaultTag("Queer community", 0xFFF7A8B8.toInt()),
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

object LivingTreeLayout {
    data class NodePosition(
        val id: Long,
        val x: Float,
        val y: Float,
        val radius: Float,
    )

    fun radialPositions(
        personIds: List<Long>,
        centerX: Float,
        centerY: Float,
        orbitRadius: Float,
        nodeRadius: Float,
        storedAngles: Map<Long, Double> = emptyMap(),
    ): List<NodePosition> {
        if (personIds.isEmpty()) return emptyList()
        val count = personIds.size
        return personIds.mapIndexed { index, id ->
            val angle = storedAngles[id] ?: (index * 2.0 * Math.PI / count)
            NodePosition(
                id = id,
                x = centerX + orbitRadius * kotlin.math.cos(angle).toFloat(),
                y = centerY + orbitRadius * kotlin.math.sin(angle).toFloat(),
                radius = nodeRadius,
            )
        }
    }

    fun computeOrbitRadius(minDimension: Float, personCount: Int): Float {
        val base = minDimension * 0.32f
        return if (personCount > 30) base * 0.95f else base
    }

    fun computeNodeRadius(personCount: Int): Float = when {
        personCount > 40 -> 22f
        personCount > 25 -> 26f
        personCount > 12 -> 30f
        else -> 34f
    }

    fun computeCenterRadius(minDimension: Float): Float =
        (minDimension * 0.09f).coerceIn(36f, 52f)

    fun angleForNewPerson(existingCount: Int): Double =
        if (existingCount == 0) 0.0 else existingCount * 2.0 * Math.PI / existingCount
}
