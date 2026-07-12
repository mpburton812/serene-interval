package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.meditationparticles.domain.affirmations.AffirmationListKind

@Entity(tableName = "affirmation_review_sessions")
data class AffirmationReviewSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val notes: String = "",
    val moodLevel: Int? = null,
    val affirmationCount: Int,
    val completedAt: Long,
    val listKind: String = AffirmationListKind.Affirmations.name,
)
