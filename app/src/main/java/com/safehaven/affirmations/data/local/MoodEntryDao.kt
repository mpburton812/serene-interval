package com.safehaven.affirmations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodEntryDao {
    @Insert
    suspend fun insert(entity: MoodEntryEntity): Long

    @Query(
        """
        SELECT * FROM mood_entries
        WHERE recordedAtMillis >= :startMillis AND recordedAtMillis < :endMillis
        ORDER BY recordedAtMillis DESC
        """,
    )
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<MoodEntryEntity>>

    @Query(
        """
        SELECT AVG(moodLevel) FROM mood_entries
        WHERE recordedAtMillis >= :startMillis AND recordedAtMillis < :endMillis
        """,
    )
    fun averageInRange(startMillis: Long, endMillis: Long): Flow<Double?>

    @Query(
        """
        SELECT COUNT(*) FROM mood_entries
        WHERE legacyTable = :legacyTable AND legacyRowId = :legacyRowId
        """,
    )
    suspend fun countLegacy(legacyTable: String, legacyRowId: Long): Int

    @Query("SELECT * FROM mood_entries ORDER BY recordedAtMillis DESC")
    suspend fun getAll(): List<MoodEntryEntity>

    @Query("DELETE FROM mood_entries")
    suspend fun clearAll()
}
