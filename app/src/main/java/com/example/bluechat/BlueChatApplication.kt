package com.example.bluechat

import android.app.Application
import com.example.bluechat.data.ChatDatabase
import com.example.bluechat.data.ChatRepository

class BlueChatApplication : Application() {
    // Database instance
    private val database by lazy { ChatDatabase.getDatabase(this) }
    
    // Repository instance
    val repository by lazy { ChatRepository(database.messageDao()) }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any other app-wide components here
    }
} 