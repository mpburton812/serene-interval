package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThoughtDumpDao {
    @Query("SELECT * FROM thought_dumps ORDER BY createdAt DESC")
    suspend fun getAll(): List<ThoughtDumpEntity>

    @Query("SELECT * FROM thought_dumps WHERE logType = :logType ORDER BY createdAt DESC")
    fun observeByType(logType: String): Flow<List<ThoughtDumpEntity>>

    @Query("SELECT * FROM thought_dumps ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<ThoughtDumpEntity?>

    @Insert
    suspend fun insert(entity: ThoughtDumpEntity): Long

    @Query("SELECT * FROM thought_dumps WHERE id = :id")
    suspend fun getById(id: Long): ThoughtDumpEntity?

    @Query("DELETE FROM thought_dumps WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM thought_dumps")
    suspend fun clearAll()
}
