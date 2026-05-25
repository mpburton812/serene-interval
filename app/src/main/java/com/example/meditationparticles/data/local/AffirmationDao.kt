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
    @Query("SELECT * FROM affirmations ORDER BY sortOrder ASC, createdAt DESC")
    fun observeAll(): Flow<List<AffirmationEntity>>

    @Query("SELECT * FROM affirmations ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAll(): List<AffirmationEntity>

    @Query("SELECT COUNT(*) FROM affirmations")
    suspend fun count(): Int

    @Query("SELECT * FROM affirmations ORDER BY RANDOM() LIMIT 1")
    suspend fun random(): AffirmationEntity?

    @Query("SELECT * FROM affirmations WHERE isFavorite = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun randomFavorite(): AffirmationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AffirmationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AffirmationEntity>)

    @Update
    suspend fun update(entity: AffirmationEntity)

    @Delete
    suspend fun delete(entity: AffirmationEntity)

    @Query("UPDATE affirmations SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}
