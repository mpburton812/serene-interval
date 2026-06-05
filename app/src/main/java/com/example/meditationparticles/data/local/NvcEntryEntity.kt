package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nvc_entries")
data class NvcEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val observation: String,
    val feeling: String,
    val need: String,
    val request: String,
    val moodLevel: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
