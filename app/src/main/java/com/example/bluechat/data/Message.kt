package com.example.bluechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val senderId: String,
    val receiverId: String,
    val deviceUuid: String,  // UUID of the device this message is associated with
    val timestamp: Date,
    val isSent: Boolean // true if sent by this device, false if received
) 