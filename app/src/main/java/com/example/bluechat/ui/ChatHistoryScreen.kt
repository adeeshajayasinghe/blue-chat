package com.example.bluechat.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluechat.ble.operations.DeviceConnectionState
import com.example.bluechat.ble.operations.sendData
import com.example.bluechat.data.Message
import com.example.bluechat.server.GATTServerService.Companion.CHARACTERISTIC_UUID
import com.example.bluechat.server.GATTServerService.Companion.SERVICE_UUID
import com.example.bluechat.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Locale

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

    var isDeviceOnline by remember { mutableStateOf(false) }
    var connectionState by remember { mutableStateOf<DeviceConnectionState?>(null) }
    val context = LocalContext.current
    val adapter = context.getSystemService(BluetoothManager::class.java).adapter

    // Load messages when the screen is first displayed
    LaunchedEffect(deviceId) {
        viewModel.loadMessagesForDeviceUuid(deviceId)
    }

    // BLE scanning effect to check if the device is online
    DisposableEffect(Unit) {
        val scanSettings = ScanSettings.Builder()
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        val leScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val gattServerID = result.scanRecord?.serviceUuids?.find { it.toString() == deviceId }
                Log.d("GATT_SERVER", "Scan result: ${gattServerID.toString()} and $deviceId")
                val isTargetDevice = (gattServerID != null) &&
                        (result.scanRecord?.serviceUuids?.contains(ParcelUuid(SERVICE_UUID)) == true)
                if (gattServerID != null) {
                    Log.d("GATT_SERVER", "Broadcast UUID found: ${gattServerID.uuid}")
                }

                if (isTargetDevice) {
                    isDeviceOnline = true
                    connectionState = DeviceConnectionState(
                        gatt = result.device.connectGatt(context, false, object : android.bluetooth.BluetoothGattCallback() {
                            override fun onConnectionStateChange(gatt: android.bluetooth.BluetoothGatt, status: Int, newState: Int) {
                                super.onConnectionStateChange(gatt, status, newState)
                                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                                    Log.d("CHAT", "Device connected")
                                    gatt.discoverServices() // Discover services after connection
                                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                                    Log.d("CHAT", "Device disconnected")
                                }
                            }
                        }),
                        connectionState = android.bluetooth.BluetoothProfile.STATE_CONNECTING,
                        mtu = -1
                    )
                    adapter?.bluetoothLeScanner?.stopScan(this)
                }
            }
        }

        adapter?.bluetoothLeScanner?.startScan(null, scanSettings, leScanCallback)
        onDispose {
            adapter?.bluetoothLeScanner?.stopScan(leScanCallback)
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
                    enabled = isDeviceOnline && connectionState?.gatt != null
                )

                IconButton(
                    onClick = {
                        if (messageInput.isNotBlank() && connectionState?.gatt != null) {
                            val services = connectionState?.gatt?.services
//                            if (services.isNullOrEmpty()) {
//                                Log.w("CHAT", "Services not yet discovered. Trying to discover now.")
//                                connectionState?.gatt?.discoverServices()
//                            }

                            val characteristic = services
                                ?.find { it.uuid == SERVICE_UUID }
                                ?.getCharacteristic(CHARACTERISTIC_UUID)

                            if (characteristic != null) {
                                Log.d("CHAT", "Characteristic found: $characteristic")
                                sendData(connectionState!!.gatt!!, characteristic, messageInput)
                                viewModel.addMessage(
                                    content = messageInput,
                                    senderId = "current_user",
                                    receiverId = deviceId,
                                    deviceUuid = deviceId,
                                    isSent = true
                                )
                                messageInput = ""
                            } else {
                                Log.w("CHAT", "Characteristic not found. Ensure UUIDs match and services are ready.")
                            }
                        }
                    },
                    enabled = messageInput.isNotBlank() && isDeviceOnline && connectionState?.gatt != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (messageInput.isNotBlank() && isDeviceOnline && connectionState?.gatt != null)
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