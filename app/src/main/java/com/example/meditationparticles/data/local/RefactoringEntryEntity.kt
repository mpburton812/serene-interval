package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refactoring_entries")
data class RefactoringEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val interpretation: String,
    val interpretationAudioPath: String? = null,
    val actualFacts: String,
    val actualFactsAudioPath: String? = null,
    val explanation1: String,
    val explanation1AudioPath: String? = null,
    val explanation2: String,
    val explanation2AudioPath: String? = null,
    val explanation3: String,
    val explanation3AudioPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
