package com.safehaven.affirmations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.safehaven.affirmations.domain.toolkit.ToolkitToolId
import kotlinx.coroutines.flow.Flow

@Dao
interface HeartsEntryDao {
    @Query("SELECT * FROM hearts_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HeartsEntryEntity>>

    @Query("SELECT * FROM hearts_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<HeartsEntryEntity>

    @Query("SELECT * FROM hearts_entries WHERE toolId = :toolId ORDER BY createdAt DESC")
    fun observeByTool(toolId: String): Flow<List<HeartsEntryEntity>>

    @Query("SELECT * FROM hearts_entries WHERE personId = :personId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForPerson(personId: Long): HeartsEntryEntity?

    @Insert
    suspend fun insert(entity: HeartsEntryEntity): Long

    @Query("DELETE FROM hearts_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

fun ToolkitToolId.toHeartsDbValue(): String = name
