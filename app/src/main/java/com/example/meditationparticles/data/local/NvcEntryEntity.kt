package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nvc_entries")
data class NvcEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val observation: String,
    val observationAudioPath: String? = null,
    val feeling: String,
    val feelingAudioPath: String? = null,
    val need: String,
    val needAudioPath: String? = null,
    val request: String,
    val requestAudioPath: String? = null,
    val moodLevel: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
