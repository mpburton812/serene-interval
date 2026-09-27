package com.safehaven.affirmations.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ThermometerDao {
    @Query("SELECT * FROM thermometers ORDER BY sortOrder ASC, id ASC")
    fun observeAll(): Flow<List<ThermometerEntity>>

    @Query("SELECT * FROM thermometers ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<ThermometerEntity>

    @Query("SELECT * FROM thermometers WHERE id = :id")
    suspend fun getById(id: Long): ThermometerEntity?

    @Insert
    suspend fun insert(entity: ThermometerEntity): Long

    @Update
    suspend fun update(entity: ThermometerEntity)

    @Query("DELETE FROM thermometers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM thermometers")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM thermometers WHERE isArchived = 0")
    suspend fun activeCount(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM thermometers")
    suspend fun maxSortOrder(): Int
}

@Dao
interface ThermometerEventDao {
    @Query("SELECT * FROM thermometer_events ORDER BY recordedAt DESC, id DESC")
    fun observeAll(): Flow<List<ThermometerEventEntity>>

    @Query(
        """
        SELECT * FROM thermometer_events
        WHERE thermometerId = :thermometerId
        ORDER BY recordedAt DESC, id DESC
        """,
    )
    fun observeForThermometer(thermometerId: Long): Flow<List<ThermometerEventEntity>>

    @Query("SELECT * FROM thermometer_events ORDER BY recordedAt ASC, id ASC")
    suspend fun getAll(): List<ThermometerEventEntity>

    @Insert
    suspend fun insert(entity: ThermometerEventEntity): Long

    @Query("DELETE FROM thermometer_events")
    suspend fun deleteAll()
}
