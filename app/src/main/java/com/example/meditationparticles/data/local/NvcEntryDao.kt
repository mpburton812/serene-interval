package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NvcEntryDao {
    @Query("SELECT * FROM nvc_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<NvcEntryEntity>

    @Query("SELECT * FROM nvc_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NvcEntryEntity>>

    @Insert
    suspend fun insert(entity: NvcEntryEntity): Long

    @Query("SELECT * FROM nvc_entries WHERE id = :id")
    suspend fun getById(id: Long): NvcEntryEntity?

    @Query("DELETE FROM nvc_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
