package com.example.bluechat.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

object UUIDManager {
    private const val PREFS_NAME = "uuid_prefs"
    private const val KEY_UNIQUE_UUID = "unique_uuid"
    
    // Common part of the UUID that will be shared across all devices
    private val COMMON_UUID_PART = "0000-1000-8000-00805f9b34fb"
    
    fun getOrCreateUniqueUUID(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var uniqueUUID = prefs.getString(KEY_UNIQUE_UUID, null)
        
        if (uniqueUUID == null) {
            // Generate a new unique part (first 8 characters of a random UUID)
            uniqueUUID = UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString(KEY_UNIQUE_UUID, uniqueUUID).apply()
        }
        
        return uniqueUUID
    }
    
    fun getFullUUID(context: Context): UUID {
        val uniquePart = getOrCreateUniqueUUID(context)
        // Combine unique part with common part
        val fullUUIDString = "$uniquePart-$COMMON_UUID_PART"
        return UUID.fromString(fullUUIDString)
    }
    
    fun isOurUUID(uuid: String): Boolean {
        return uuid.endsWith(COMMON_UUID_PART)
    }
    
    fun getUniquePart(uuid: String): String? {
        return if (isOurUUID(uuid)) {
            uuid.split("-")[0]
        } else {
            null
        }
    }
} 