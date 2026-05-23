package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThoughtDumpDao {
    @Query("SELECT * FROM thought_dumps ORDER BY createdAt DESC LIMIT 1")
    fun observeLatest(): Flow<ThoughtDumpEntity?>

    @Insert
    suspend fun insert(entity: ThoughtDumpEntity): Long

    @Query("DELETE FROM thought_dumps")
    suspend fun clearAll()
}
