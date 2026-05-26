package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface OneNoteSyncDao {
    @Upsert
    suspend fun upsertMapping(mapping: OneNoteSyncMappingEntity)

    @Query("SELECT * FROM one_note_sync_mappings WHERE localEntryId = :localEntryId AND entryType = :entryType")
    suspend fun getMapping(localEntryId: Long, entryType: String): OneNoteSyncMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: OneNoteSyncQueueEntity): Long

    @Query("SELECT * FROM one_note_sync_queue ORDER BY enqueuedAt ASC")
    suspend fun getAllQueueItems(): List<OneNoteSyncQueueEntity>

    @Query("DELETE FROM one_note_sync_queue WHERE id = :id")
    suspend fun deleteQueueItem(id: Long)

    @Query("UPDATE one_note_sync_queue SET retryCount = :retryCount WHERE id = :id")
    suspend fun updateQueueRetryCount(id: Long, retryCount: Int)

    @Query("DELETE FROM one_note_sync_queue")
    suspend fun clearQueue()

    @Query("DELETE FROM one_note_sync_mappings")
    suspend fun clearMappings()

    @Query("SELECT COUNT(*) FROM one_note_sync_queue")
    suspend fun pendingQueueCount(): Int
}
