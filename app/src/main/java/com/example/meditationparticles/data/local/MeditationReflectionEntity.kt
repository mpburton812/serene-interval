package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meditation_reflections")
data class MeditationReflectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reflection: String,
    val durationSeconds: Int,
    val completedAt: Long,
)

