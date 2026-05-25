package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CenterOfGravityEntryDao {
    @Query("SELECT * FROM center_of_gravity_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<CenterOfGravityEntryEntity>

    @Query("SELECT * FROM center_of_gravity_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CenterOfGravityEntryEntity>>

    @Insert
    suspend fun insert(entity: CenterOfGravityEntryEntity): Long

    @Query("SELECT * FROM center_of_gravity_entries WHERE id = :id")
    suspend fun getById(id: Long): CenterOfGravityEntryEntity?

    @Query("DELETE FROM center_of_gravity_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
