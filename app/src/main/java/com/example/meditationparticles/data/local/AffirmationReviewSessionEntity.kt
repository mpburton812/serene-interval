package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "affirmation_review_sessions")
data class AffirmationReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val notes: String = "",
    val moodLevel: Int? = null,
    val affirmationCount: Int,
    val completedAt: Long,
)
