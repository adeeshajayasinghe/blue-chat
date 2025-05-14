package com.example.bluechat.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY CASE WHEN lastMessageTimestamp IS NULL THEN 1 ELSE 0 END, lastMessageTimestamp DESC")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE name = :name")
    suspend fun getDevice(name: String): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("DELETE FROM devices")
    suspend fun deleteAllDevices()
} 