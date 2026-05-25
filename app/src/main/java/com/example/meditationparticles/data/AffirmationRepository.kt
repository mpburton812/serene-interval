package com.example.meditationparticles.data

import com.example.meditationparticles.data.local.AffirmationDao
import com.example.meditationparticles.data.local.AffirmationEntity
import kotlinx.coroutines.flow.Flow

class AffirmationRepository(
    private val dao: AffirmationDao,
) {
    val affirmations: Flow<List<AffirmationEntity>> = dao.observeAll()

    suspend fun seedIfEmpty() {
        if (dao.count() > 0) return
        DefaultAffirmations.texts.forEachIndexed { index, text ->
            dao.insert(
                AffirmationEntity(
                    text = text,
                    sortOrder = index,
                    createdAt = System.currentTimeMillis() - index * 86_400_000L,
                ),
            )
        }
    }

    suspend fun randomAffirmation(): AffirmationEntity? = dao.random()

    suspend fun randomFavoriteAffirmation(): AffirmationEntity? = dao.randomFavorite()

    suspend fun add(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insert(AffirmationEntity(text = trimmed))
    }

    suspend fun bulkAdd(rawText: String): Int {
        val texts = parseAffirmationLines(rawText)
        if (texts.isEmpty()) return 0
        val baseSortOrder = dao.count()
        val entities = texts.mapIndexed { index, text ->
            AffirmationEntity(text = text, sortOrder = baseSortOrder + index)
        }
        dao.insertAll(entities)
        return texts.size
    }

    suspend fun update(entity: AffirmationEntity) = dao.update(entity)

    suspend fun delete(entity: AffirmationEntity) = dao.delete(entity)

    suspend fun toggleFavorite(entity: AffirmationEntity) {
        dao.setFavorite(entity.id, !entity.isFavorite)
    }
}

internal fun parseAffirmationLines(raw: String): List<String> =
    raw.lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private object DefaultAffirmations {
    val texts = listOf(
        "I am worthy of peace, and I allow myself to breathe deeply through every moment.",
        "My anxiety does not define my future or my value as a human being.",
        "I am releasing the need to control the uncontrollable.",
        "This feeling is temporary. I have survived 100% of my bad days.",
        "I choose to be kind to myself in this moment of struggle.",
    )
}
