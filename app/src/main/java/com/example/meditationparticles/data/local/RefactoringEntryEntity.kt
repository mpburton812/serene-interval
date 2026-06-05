package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "refactoring_entries")
data class RefactoringEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val interpretation: String,
    val actualFacts: String,
    val explanation1: String,
    val explanation2: String,
    val explanation3: String,
    val moodLevel: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
