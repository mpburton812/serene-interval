package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AffirmationReviewSessionDao {
    @Query("SELECT * FROM affirmation_review_sessions ORDER BY completedAt DESC")
    fun observeAll(): Flow<List<AffirmationReviewSessionEntity>>

    @Query(
        "SELECT * FROM affirmation_review_sessions " +
            "WHERE completedAt >= :startMillis AND completedAt < :endMillis " +
            "ORDER BY completedAt DESC",
    )
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<AffirmationReviewSessionEntity>>

    @Query("SELECT * FROM affirmation_review_sessions ORDER BY completedAt DESC")
    suspend fun getAll(): List<AffirmationReviewSessionEntity>

    @Insert
    suspend fun insert(entity: AffirmationReviewSessionEntity): Long

    @Query("SELECT * FROM affirmation_review_sessions WHERE id = :id")
    suspend fun getById(id: Long): AffirmationReviewSessionEntity?

    @Query("DELETE FROM affirmation_review_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
