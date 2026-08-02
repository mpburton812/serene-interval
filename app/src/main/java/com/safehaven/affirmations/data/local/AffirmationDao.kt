package com.safehaven.affirmations.data.local

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
        "SELECT * FROM affirmations WHERE listKind = :listKind AND isArchived = 0 " +
            "ORDER BY sortOrder ASC, createdAt DESC",
    )
    fun observeActive(listKind: String): Flow<List<AffirmationEntity>>

    @Query(
        "SELECT * FROM affirmations WHERE listKind = :listKind AND isArchived = 1 " +
            "ORDER BY sortOrder ASC, createdAt DESC",
    )
    fun observeArchived(listKind: String): Flow<List<AffirmationEntity>>

    @Query(
        "SELECT * FROM affirmations WHERE listKind = :listKind AND isArchived = 0 " +
            "ORDER BY sortOrder ASC, createdAt DESC",
    )
    suspend fun getActive(listKind: String): List<AffirmationEntity>

    @Query("SELECT * FROM affirmations ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllKinds(): List<AffirmationEntity>

    @Query("SELECT COUNT(*) FROM affirmations WHERE listKind = :listKind")
    suspend fun countAll(listKind: String): Int

    @Query("SELECT COUNT(*) FROM affirmations WHERE listKind = :listKind AND isArchived = 0")
    suspend fun countActive(listKind: String): Int

    @Query(
        "SELECT * FROM affirmations WHERE listKind = :listKind AND isArchived = 0 " +
            "ORDER BY RANDOM() LIMIT 1",
    )
    suspend fun randomActive(listKind: String): AffirmationEntity?

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
