package com.safehaven.affirmations.data

import com.safehaven.affirmations.data.local.ThermometerDao
import com.safehaven.affirmations.data.local.ThermometerEntity
import com.safehaven.affirmations.data.local.ThermometerEventDao
import com.safehaven.affirmations.data.local.ThermometerEventEntity
import com.safehaven.affirmations.data.local.ThermometerEventType
import com.safehaven.affirmations.domain.thermometer.ThermometerRules
import kotlinx.coroutines.flow.Flow

class ThermometerRepository(
    private val thermometerDao: ThermometerDao,
    private val eventDao: ThermometerEventDao,
) {
    fun observeThermometers(): Flow<List<ThermometerEntity>> = thermometerDao.observeAll()

    fun observeEvents(): Flow<List<ThermometerEventEntity>> = eventDao.observeAll()

    fun observeEvents(thermometerId: Long): Flow<List<ThermometerEventEntity>> =
        eventDao.observeForThermometer(thermometerId)

    suspend fun create(name: String, nowMillis: Long = System.currentTimeMillis()): Long? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        if (!ThermometerRules.canCreate(thermometerDao.activeCount())) return null
        val id = thermometerDao.insert(
            ThermometerEntity(
                name = trimmed,
                sortOrder = thermometerDao.maxSortOrder() + 1,
                isArchived = false,
                createdAt = nowMillis,
                updatedAt = nowMillis,
            ),
        )
        eventDao.insert(
            ThermometerEventEntity(
                thermometerId = id,
                type = ThermometerEventType.RENAME,
                previousName = "",
                newName = trimmed,
                recordedAt = nowMillis,
            ),
        )
        return id
    }

    suspend fun rename(id: Long, name: String, nowMillis: Long = System.currentTimeMillis()) {
        val existing = thermometerDao.getById(id) ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed == existing.name) return
        thermometerDao.update(existing.copy(name = trimmed, updatedAt = nowMillis))
        eventDao.insert(
            ThermometerEventEntity(
                thermometerId = id,
                type = ThermometerEventType.RENAME,
                previousName = existing.name,
                newName = trimmed,
                recordedAt = nowMillis,
            ),
        )
    }

    suspend fun record(
        id: Long,
        score: Int,
        note: String,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        if (thermometerDao.getById(id) == null) return
        eventDao.insert(
            ThermometerEventEntity(
                thermometerId = id,
                type = ThermometerEventType.READING,
                score = ThermometerRules.clampScore(score),
                note = note.trim().ifEmpty { null },
                recordedAt = nowMillis,
            ),
        )
    }

    suspend fun archive(id: Long, nowMillis: Long = System.currentTimeMillis()) {
        val existing = thermometerDao.getById(id) ?: return
        if (existing.isArchived) return
        thermometerDao.update(existing.copy(isArchived = true, updatedAt = nowMillis))
    }

    suspend fun unarchive(id: Long, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val existing = thermometerDao.getById(id) ?: return false
        if (!existing.isArchived) return true
        if (!ThermometerRules.canCreate(thermometerDao.activeCount())) return false
        thermometerDao.update(existing.copy(isArchived = false, updatedAt = nowMillis))
        return true
    }

    suspend fun delete(id: Long) {
        thermometerDao.deleteById(id)
    }

    suspend fun replaceAllFromExport(
        thermometers: List<ThermometerEntity>,
        events: List<ThermometerEventEntity>,
    ) {
        eventDao.deleteAll()
        thermometerDao.deleteAll()
        thermometers.forEach { thermometerDao.insert(it) }
        events.forEach { eventDao.insert(it) }
    }
}
