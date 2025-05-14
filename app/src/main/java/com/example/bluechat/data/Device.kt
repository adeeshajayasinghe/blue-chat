package com.example.bluechat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey
    val name: String,
    val address: String,
    val lastConnected: Long,
    val lastMessageTimestamp: Long?
) 