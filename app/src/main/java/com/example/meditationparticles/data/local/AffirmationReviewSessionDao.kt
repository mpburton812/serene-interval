package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AffirmationReviewSessionDao {
    @Query("SELECT * FROM affirmation_review_sessions ORDER BY completedAt DESC")
    suspend fun getAll(): List<AffirmationReviewSessionEntity>

    @Insert
    suspend fun insert(entity: AffirmationReviewSessionEntity): Long

    @Query("SELECT * FROM affirmation_review_sessions WHERE id = :id")
    suspend fun getById(id: Long): AffirmationReviewSessionEntity?

    @Query("DELETE FROM affirmation_review_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
