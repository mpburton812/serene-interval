package com.example.meditationparticles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FutureSelfMessageDao {
    @Query("SELECT * FROM future_self_messages ORDER BY scheduledAtMillis ASC")
    fun observeAll(): Flow<List<FutureSelfMessageEntity>>

    @Query("SELECT * FROM future_self_messages WHERE id = :id")
    suspend fun getById(id: Long): FutureSelfMessageEntity?

    @Query("SELECT * FROM future_self_messages WHERE delivered = 0 AND scheduledAtMillis > :nowMillis")
    suspend fun getPendingAfter(nowMillis: Long): List<FutureSelfMessageEntity>

    @Query("SELECT * FROM future_self_messages WHERE delivered = 0 AND scheduledAtMillis <= :nowMillis")
    suspend fun getOverdueUndelivered(nowMillis: Long): List<FutureSelfMessageEntity>

    @Insert
    suspend fun insert(entity: FutureSelfMessageEntity): Long

    @Update
    suspend fun update(entity: FutureSelfMessageEntity)

    @Query("DELETE FROM future_self_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE future_self_messages SET delivered = 1 WHERE id = :id")
    suspend fun markDelivered(id: Long)
}
