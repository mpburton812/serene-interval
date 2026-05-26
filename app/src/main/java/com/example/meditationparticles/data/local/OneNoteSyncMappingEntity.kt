package com.example.meditationparticles.data.local

import androidx.room.Entity

@Entity(
    tableName = "one_note_sync_mappings",
    primaryKeys = ["localEntryId", "entryType"],
)
data class OneNoteSyncMappingEntity(
    val localEntryId: Long,
    val entryType: String,
    val oneNotePageId: String? = null,
    val syncStatus: String,
    val lastError: String? = null,
    val syncedAt: Long? = null,
)
