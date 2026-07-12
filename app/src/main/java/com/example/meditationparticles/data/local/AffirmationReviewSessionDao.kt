package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AffirmationReviewSessionDao {
    @Query(
        "SELECT * FROM affirmation_review_sessions WHERE listKind = :listKind " +
            "ORDER BY completedAt DESC",
    )
    fun observeAll(listKind: String): Flow<List<AffirmationReviewSessionEntity>>

    @Query("SELECT * FROM affirmation_review_sessions ORDER BY completedAt DESC")
    fun observeAllKinds(): Flow<List<AffirmationReviewSessionEntity>>

    @Query(
        "SELECT * FROM affirmation_review_sessions " +
            "WHERE listKind = :listKind AND completedAt >= :startMillis AND completedAt < :endMillis " +
            "ORDER BY completedAt DESC",
    )
    fun observeInRange(
        listKind: String,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<AffirmationReviewSessionEntity>>

    @Query(
        "SELECT * FROM affirmation_review_sessions WHERE listKind = :listKind " +
            "ORDER BY completedAt DESC",
    )
    suspend fun getAll(listKind: String): List<AffirmationReviewSessionEntity>

    @Query("SELECT * FROM affirmation_review_sessions ORDER BY completedAt DESC")
    suspend fun getAllKinds(): List<AffirmationReviewSessionEntity>

    @Insert
    suspend fun insert(entity: AffirmationReviewSessionEntity): Long

    @Query("SELECT * FROM affirmation_review_sessions WHERE id = :id")
    suspend fun getById(id: Long): AffirmationReviewSessionEntity?

    @Query("DELETE FROM affirmation_review_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
