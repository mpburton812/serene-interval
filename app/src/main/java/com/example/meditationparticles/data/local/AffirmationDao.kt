package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AffirmationDao {
    @Query(
        "SELECT * FROM affirmations WHERE listKind = :listKind " +
            "ORDER BY sortOrder ASC, createdAt DESC",
    )
    fun observeAll(listKind: String): Flow<List<AffirmationEntity>>

    @Query(
        "SELECT * FROM affirmations WHERE listKind = :listKind " +
            "ORDER BY sortOrder ASC, createdAt DESC",
    )
    suspend fun getAll(listKind: String): List<AffirmationEntity>

    @Query("SELECT * FROM affirmations ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllKinds(): List<AffirmationEntity>

    @Query("SELECT COUNT(*) FROM affirmations WHERE listKind = :listKind")
    suspend fun count(listKind: String): Int

    @Query(
        "SELECT * FROM affirmations WHERE listKind = :listKind " +
            "ORDER BY RANDOM() LIMIT 1",
    )
    suspend fun random(listKind: String): AffirmationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AffirmationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AffirmationEntity>)

    @Update
    suspend fun update(entity: AffirmationEntity)

    @Update
    suspend fun updateAll(entities: List<AffirmationEntity>)

    @Delete
    suspend fun delete(entity: AffirmationEntity)
}
