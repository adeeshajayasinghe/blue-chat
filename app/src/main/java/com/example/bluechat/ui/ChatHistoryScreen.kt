package com.example.bluechat.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluechat.BlueChatApplication
import com.example.bluechat.data.Message
import com.example.bluechat.server.GATTServerService.Companion.SERVICE_UUID
import com.example.bluechat.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ChatHistoryScreen(
    deviceId: String,
    deviceName: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var messageInput by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // State for device online status
    var isDeviceOnline by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val adapter = context.getSystemService(BluetoothManager::class.java).adapter

    // Load messages when the screen is first displayed
    LaunchedEffect(deviceId) {
        viewModel.loadMessagesForDeviceName(deviceName)
    }

    // Start scanning for devices
    LaunchedEffect(Unit) {
        isScanning = true
    }

    // Stop scanning when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            isScanning = false
        }
    }

    // BLE scanning effect
    if (isScanning && adapter != null) {
        val scanSettings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        val leScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                // Check if this is our target device by comparing name and service UUID
                val isTargetDevice = result.device.name == deviceName && 
                    result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true
                
                if (isTargetDevice) {
                    isDeviceOnline = true
                }
            }
        }

        DisposableEffect(scanSettings) {
            adapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)
            onDispose {
                adapter.bluetoothLeScanner.stopScan(leScanCallback)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(deviceName ?: "Chat")
                        Text(
                            text = if (isDeviceOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDeviceOnline) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages.reversed()) { message ->
                            MessageItem(message, dateFormat)
                        }
                    }
                }
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    placeholder = { Text("Type a message...") },
                    enabled = isDeviceOnline // Disable input when device is offline
                )

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            viewModel.addMessage(
                                content = messageInput,
                                senderId = "current_user",
                                receiverId = deviceId,
                                deviceName = deviceName,
                                isSent = true
                            )
                            messageInput = ""
                        }
                    },
                    enabled = messageInput.isNotBlank() && isDeviceOnline // Disable send when device is offline
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (messageInput.isNotBlank() && isDeviceOnline)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }

        // Show error if any
        error?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
                // Show error message (you might want to use a Snackbar or Toast)
                viewModel.clearError()
            }
        }
    }
}

@Composable
private fun MessageItem(message: Message, dateFormat: SimpleDateFormat) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (message.isSent) Alignment.End else Alignment.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 340.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isSent) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isSent) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Text(
                    text = dateFormat.format(message.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isSent) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
} 