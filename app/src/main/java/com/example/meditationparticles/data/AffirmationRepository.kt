package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.AffirmationDao
import com.example.meditationparticles.data.local.AffirmationEntity
import com.example.meditationparticles.domain.affirmations.AffirmationListKind
import com.example.meditationparticles.domain.affirmations.AffirmationReorder
import kotlinx.coroutines.flow.Flow

class AffirmationRepository(
    private val dao: AffirmationDao,
    private val listKind: AffirmationListKind = AffirmationListKind.Affirmations,
) {
    private val kindKey = listKind.name

    val affirmations: Flow<List<AffirmationEntity>> = dao.observeActive(kindKey)

    val archivedAffirmations: Flow<List<AffirmationEntity>> = dao.observeArchived(kindKey)

    suspend fun seedIfEmpty() {
        if (dao.countAll(kindKey) > 0) return
        listKind.defaultTexts.forEachIndexed { index, text ->
            dao.insert(
                AffirmationEntity(
                    text = text,
                    sortOrder = index,
                    createdAt = System.currentTimeMillis() - index * 86_400_000L,
                    listKind = kindKey,
                ),
            )
        }
    }

    suspend fun randomAffirmation(): AffirmationEntity? = dao.randomActive(kindKey)

    suspend fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insert(
            AffirmationEntity(
                text = trimmed,
                sortOrder = dao.countActive(kindKey),
                listKind = kindKey,
            ),
        )
    }

    suspend fun bulkAdd(rawText: String): Int {
        val texts = parseAffirmationLines(rawText)
        if (texts.isEmpty()) return 0
        val baseSortOrder = dao.countActive(kindKey)
        val entities = texts.mapIndexed { index, text ->
            AffirmationEntity(
                text = text,
                sortOrder = baseSortOrder + index,
                listKind = kindKey,
            )
        }
        dao.insertAll(entities)
        return texts.size
    }

    suspend fun update(entity: AffirmationEntity) = dao.update(entity)

    suspend fun delete(entity: AffirmationEntity) = dao.delete(entity)

    suspend fun archive(entity: AffirmationEntity) {
        dao.update(entity.copy(isArchived = true))
    }

    suspend fun unarchive(entity: AffirmationEntity) {
        dao.update(
            entity.copy(
                isArchived = false,
                sortOrder = dao.countActive(kindKey),
            ),
        )
    }

    suspend fun reorder(fromIndex: Int, toIndex: Int) {
        val current = dao.getActive(kindKey)
        val reordered = AffirmationReorder.reorder(current, fromIndex, toIndex)
        val updates = reordered.mapIndexedNotNull { index, entity ->
            if (entity.sortOrder != index) entity.copy(sortOrder = index) else null
        }
        if (updates.isNotEmpty()) {
            dao.updateAll(updates)
        }
    }
}

internal fun parseAffirmationLines(raw: String): List<String> =
    raw.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
