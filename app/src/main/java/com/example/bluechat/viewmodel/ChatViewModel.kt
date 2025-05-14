package com.example.bluechat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluechat.BlueChatApplication
import com.example.bluechat.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.Date

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BlueChatApplication).repository
    
    // State for messages
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    
    // State for loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // State for error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    // Load messages for a specific device
    fun loadMessagesForDevice(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getMessagesWithDevice(deviceId)
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _isLoading.value = false
                }
                .collect { messageList ->
                    _messages.value = messageList
                    _isLoading.value = false
                }
        }
    }
    
    // Add a new message
    fun addMessage(deviceName: String, content: String, senderId: String, receiverId: String, isSent: Boolean) {
        viewModelScope.launch {
            try {
                val message = Message(
                    content = content,
                    senderId = senderId,
                    receiverId = receiverId,
                    deviceName = deviceName,
                    timestamp = Date(),
                    isSent = isSent
                )
                repository.insertMessage(message)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add message"
            }
        }
    }
    
    // Clear all messages
    fun clearAllMessages() {
        viewModelScope.launch {
            try {
                repository.deleteAllMessages()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to clear messages"
            }
        }
    }
    
    // Clear error
    fun clearError() {
        _error.value = null
    }
} 