package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RefactoringEntryDao {
    @Query("SELECT * FROM refactoring_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<RefactoringEntryEntity>

    @Query("SELECT * FROM refactoring_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RefactoringEntryEntity>>

    @Insert
    suspend fun insert(entity: RefactoringEntryEntity): Long

    @Query("SELECT * FROM refactoring_entries WHERE id = :id")
    suspend fun getById(id: Long): RefactoringEntryEntity?

    @Query("DELETE FROM refactoring_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}
