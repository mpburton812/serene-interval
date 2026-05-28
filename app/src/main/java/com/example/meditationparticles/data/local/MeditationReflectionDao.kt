package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationReflectionDao {
    @Query("SELECT * FROM meditation_reflections ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<MeditationReflectionEntity>>

    @Query("SELECT * FROM meditation_reflections ORDER BY completedAt DESC")
    suspend fun getAll(): List<MeditationReflectionEntity>

    @Insert
    suspend fun insert(entity: MeditationReflectionEntity): Long

    @Query("SELECT * FROM meditation_reflections WHERE id = :id")
    suspend fun getById(id: Long): MeditationReflectionEntity?

    @Query("DELETE FROM meditation_reflections WHERE id = :id")
    suspend fun deleteById(id: Long)
}

