package com.example.meditationparticles.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "one_note_sync_queue")
data class OneNoteSyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val localEntryId: Long,
    val entryType: String,
    val enqueuedAt: Long,
    val retryCount: Int = 0,
)
