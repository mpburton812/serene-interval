package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thought_dumps")
data class ThoughtDumpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val content: String,
    val logType: String,
    val moodLevel: Int = 3,
    val audioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
