package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LivingTreePersonDao {
    @Transaction
    @Query("SELECT * FROM living_tree_people ORDER BY sortOrder, name COLLATE NOCASE")
    fun observePeopleWithTags(): Flow<List<LivingTreePersonWithTags>>

    @Transaction
    @Query("SELECT * FROM living_tree_people ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getPeopleWithTags(): List<LivingTreePersonWithTags>

    @Query("SELECT COUNT(*) FROM living_tree_people")
    suspend fun count(): Int

    @Query("SELECT * FROM living_tree_people WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LivingTreePersonEntity?

    @Query("SELECT * FROM living_tree_people WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): LivingTreePersonEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LivingTreePersonEntity): Long

    @Update
    suspend fun update(entity: LivingTreePersonEntity)

    @Delete
    suspend fun delete(entity: LivingTreePersonEntity)
}
