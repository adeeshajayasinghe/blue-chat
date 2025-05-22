package com.example.bluechat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluechat.BlueChatApplication
import com.example.bluechat.data.Device
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ContactListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as BlueChatApplication).repository
    
    // State for devices
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices
    
    // State for loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // State for error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    init {
        loadDevices()
    }
    
    private fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.allDevices
                .catch { e ->
                    _error.value = e.message ?: "Unknown error"
                    _isLoading.value = false
                }
                .collect { deviceList ->
                    _devices.value = deviceList
                    _isLoading.value = false
                }
        }
    }
    
    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            try {
                repository.deleteDevice(device)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to delete device"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
} 