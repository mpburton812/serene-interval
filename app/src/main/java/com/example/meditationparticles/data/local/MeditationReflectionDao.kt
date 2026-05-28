package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MeditationReflectionDao {
    @Insert
    suspend fun insert(entity: MeditationReflectionEntity): Long

    @Query("SELECT * FROM meditation_reflections WHERE id = :id")
    suspend fun getById(id: Long): MeditationReflectionEntity?
}

