package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mood_entries",
    indices = [Index(value = ["legacyTable", "legacyRowId"])],
)
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val moodLevel: Int,
    val recordedAtMillis: Long,
    val source: String,
    val legacyTable: String? = null,
    val legacyRowId: Long? = null,
)
