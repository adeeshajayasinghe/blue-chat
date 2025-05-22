package com.example.bluechat.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE (senderId = :deviceId OR receiverId = :deviceId) ORDER BY timestamp ASC")
    fun getMessagesWithDevice(deviceId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE deviceUuid = :deviceName ORDER BY timestamp ASC")
    fun getMessagesWithDeviceUuid(deviceName: String): Flow<List<Message>>

    @Insert
    suspend fun insertMessage(message: Message)

    @Insert
    suspend fun insertMessages(messages: List<Message>)

    @Delete
    suspend fun deleteMessage(message: Message)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
} 