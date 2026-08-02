package com.safehaven.affirmations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LivingTreePersonTagDao {
    @Query("SELECT tagId FROM living_tree_person_tags WHERE personId = :personId")
    suspend fun getTagIdsForPerson(personId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(refs: List<LivingTreePersonTagCrossRef>)

    @Query("DELETE FROM living_tree_person_tags WHERE personId = :personId")
    suspend fun deleteForPerson(personId: Long)
}
