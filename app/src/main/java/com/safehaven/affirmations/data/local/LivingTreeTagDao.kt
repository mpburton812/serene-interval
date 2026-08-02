package com.safehaven.affirmations.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LivingTreeTagDao {
    @Query("SELECT * FROM living_tree_tags ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<LivingTreeTagEntity>>

    @Query("SELECT * FROM living_tree_tags ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getAll(): List<LivingTreeTagEntity>

    @Query("SELECT COUNT(*) FROM living_tree_tags")
    suspend fun count(): Int

    @Query("SELECT * FROM living_tree_tags WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LivingTreeTagEntity?

    @Query("SELECT * FROM living_tree_tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): LivingTreeTagEntity?

    @Query(
        """
        SELECT COUNT(DISTINCT pt.personId)
        FROM living_tree_person_tags pt
        WHERE pt.tagId = :tagId
        """,
    )
    suspend fun countPeopleForTag(tagId: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: LivingTreeTagEntity): Long

    @Update
    suspend fun update(entity: LivingTreeTagEntity)

    @Delete
    suspend fun delete(entity: LivingTreeTagEntity)
}
