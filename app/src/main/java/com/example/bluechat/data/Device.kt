package com.example.bluechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val uuid: String,  // Store UUID as String
    val name: String?,
    val address: String,
    val lastConnected: Long,
    val lastMessageTimestamp: Long?
) 