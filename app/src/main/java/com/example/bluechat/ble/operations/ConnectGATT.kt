package com.example.bluechat.ble.operations

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bluechat.server.GATTServerService.Companion.CHARACTERISTIC_UUID
import com.example.bluechat.server.GATTServerService.Companion.SERVICE_UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import com.example.bluechat.BlueChatApplication
import com.example.bluechat.data.Message
import java.util.Date
import com.example.bluechat.data.Device
import com.example.bluechat.server.GATTServerService
import androidx.compose.foundation.background

@OptIn(ExperimentalAnimationApi::class)
@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun ConnectGATTSample() {
    var selectedDevice by remember {
        mutableStateOf<BluetoothDevice?>(null)
    }
    var discoveredBroadcastUuid by remember {
        mutableStateOf<String?>(null)
    }
    // Check that BT permissions and that BT is available and enabled
    BluetoothSampleBox {
        AnimatedContent(targetState = selectedDevice, label = "Selected device") { device ->
            if (device == null) {
                // Scans for BT devices and handles clicks (see FindDeviceSample)
                FindDevicesScreen { discoveredDevice, broadcastUuid ->
                    selectedDevice = discoveredDevice
                    discoveredBroadcastUuid = broadcastUuid
                    // Update the GATTServerService with the discovered UUID
                    GATTServerService.updateDiscoveredBroadcastUuid(broadcastUuid)
                }
            } else {
                // Once a device is selected show the UI and try to connect device
                ConnectDeviceScreen(device = device, broadcastUuid = discoveredBroadcastUuid) {
                    selectedDevice = null
                    discoveredBroadcastUuid = null
                    // Clear the discovered UUID when disconnecting
                    GATTServerService.updateDiscoveredBroadcastUuid(null)
                }
            }
        }
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectDeviceScreen(device: BluetoothDevice, broadcastUuid: String?, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = (context.applicationContext as BlueChatApplication).repository

    // Keeps track of the last connection state with the device
    var state by remember(device) {
        mutableStateOf<DeviceConnectionState?>(null)
    }
    // Once the device services are discovered find the GATTServer service
    val service by remember(state?.services) {
        mutableStateOf(state?.services?.find { it.uuid == SERVICE_UUID })
    }
    // If the GATTServerSample service is found, get the characteristic
    val characteristic by remember(service) {
        mutableStateOf(service?.getCharacteristic(CHARACTERISTIC_UUID))
    }
    
    // Add state for the message input
    var messageInput by remember { mutableStateOf("") }
    
    // Use the database messages instead of in-memory storage
    val messages by repository.getMessagesWithDevice(device.address).collectAsState(initial = emptyList())
    
    // Add state for showing logs
    var showLogs by remember { mutableStateOf(false) }

    // This effect will handle the connection and notify when the state changes
    BLEConnectEffect(device = device) {
        // Update state to recompose the UI
        state = it

        if (state?.connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            state?.gatt?.connect()
        }

        // Save device information when connected
        if (it.connectionState == BluetoothProfile.STATE_CONNECTED) {
            state?.gatt?.discoverServices() // Discover services after connection
        }

        // Handle discovered services
        if (it.services.isNotEmpty()) {
            // Check for SERVICE_UUID in services
            val serviceUuid = it.services.find { service -> service.uuid == SERVICE_UUID }?.uuid?.toString()
            
            Log.d("GATT_Clients", "Service UUID: $serviceUuid")
            Log.d("GATT_Clients", "Broadcast UUID from advertisement: $broadcastUuid")

            // Save device information if service UUID is found and we have a broadcast UUID
            if (serviceUuid != null && broadcastUuid != null) {
                scope.launch(Dispatchers.IO) {
                    val deviceName = device.name ?: "Unknown Device"
                    val deviceEntity = Device(
                        uuid = broadcastUuid,
                        name = deviceName,
                        address = device.address,
                        lastConnected = System.currentTimeMillis(),
                        lastMessageTimestamp = null
                    )
                    repository.insertDevice(deviceEntity)
                    Log.d("GATT_Clients", "Device saved to database with UUID: ${deviceEntity.uuid}")
                }
            }

            // Add received message to database
            if (it.messageReceived.isNotEmpty() && broadcastUuid != null) {
                scope.launch(Dispatchers.IO) {
                    val message = Message(
                        content = it.messageReceived,
                        senderId = device.address,
                        receiverId = "current_user",
                        deviceUuid = broadcastUuid,
                        timestamp = Date(),
                        isSent = false
                    )
                    repository.insertMessage(message)

                    // Update device's last message timestamp
                    repository.getDevice(broadcastUuid)?.let { existingDevice ->
                        repository.insertDevice(existingDevice.copy(
                            lastMessageTimestamp = System.currentTimeMillis()
                        ))
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Device Details",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Device info card
            Column(
                modifier = Modifier
                    .fillMaxWidth()

                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Name",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = device.name ?: "Unknown Device",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = state?.connectionState?.toConnectionStateString() ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (state?.connectionState) {
                            BluetoothProfile.STATE_CONNECTED -> MaterialTheme.colorScheme.primary
                            BluetoothProfile.STATE_CONNECTING -> MaterialTheme.colorScheme.tertiary
                            BluetoothProfile.STATE_DISCONNECTED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

// Helper function to format timestamp
private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * Writes the provided message to the server characteristic
 */
@SuppressLint("MissingPermission")
fun sendData(
    gatt: BluetoothGatt,
    characteristic: BluetoothGattCharacteristic,
    message: String
) {
    val data = message.toByteArray()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeCharacteristic(
            characteristic,
            data,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
        )
    } else {
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        @Suppress("DEPRECATION")
        characteristic.value = data
        @Suppress("DEPRECATION")
        gatt.writeCharacteristic(characteristic)
    }
}

internal fun Int.toConnectionStateString() = when (this) {
    BluetoothProfile.STATE_CONNECTED -> "Connected"
    BluetoothProfile.STATE_CONNECTING -> "Connecting"
    BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
    BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
    else -> "N/A"
}

data class DeviceConnectionState(
    val gatt: BluetoothGatt?,
    val connectionState: Int,
    val mtu: Int,
    val services: List<BluetoothGattService> = emptyList(),
    val messageSent: Boolean = false,
    val messageReceived: String = "",
) {
    companion object {
        val None = DeviceConnectionState(null, -1, -1)
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun BLEConnectEffect(
    device: BluetoothDevice,
    onStateChange: (DeviceConnectionState) -> Unit,
) {
    val context = LocalContext.current
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    // Keep the current connection state
    var state by remember {
        mutableStateOf(DeviceConnectionState.None)
    }

    DisposableEffect(device) {
        // This callback will notify us when things change in the GATT connection so we can update
        // our state
        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int,
            ) {
                super.onConnectionStateChange(gatt, status, newState)
                state = state.copy(gatt = gatt, connectionState = newState)
                currentOnStateChange(state)

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    // Here you should handle the error returned in status based on the constants
                    // https://developer.android.com/reference/android/bluetooth/BluetoothGatt#summary
                    // For example for GATT_INSUFFICIENT_ENCRYPTION or
                    // GATT_INSUFFICIENT_AUTHENTICATION you should create a bond.
                    // https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createBond()
                    Log.e("BLEConnectEffect", "An error happened: $status")
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                super.onMtuChanged(gatt, mtu, status)
                state = state.copy(gatt = gatt, mtu = mtu)
                currentOnStateChange(state)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                state = state.copy(services = gatt.services)
                currentOnStateChange(state)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int,
            ) {
                super.onCharacteristicWrite(gatt, characteristic, status)
                state = state.copy(messageSent = status == BluetoothGatt.GATT_SUCCESS)
                currentOnStateChange(state)
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int,
            ) {
                super.onCharacteristicRead(gatt, characteristic, status)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    doOnRead(characteristic.value)
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int,
            ) {
                super.onCharacteristicRead(gatt, characteristic, value, status)
                doOnRead(value)
            }

            private fun doOnRead(value: ByteArray) {
                state = state.copy(messageReceived = value.decodeToString())
                currentOnStateChange(state)
            }
        }

        // Create a new GATT connection
        state = state.copy(gatt = device.connectGatt(context, false, callback))

        // When the effect leaves the Composition, close the connection
        onDispose {
            state.gatt?.close()
            state = DeviceConnectionState.None
        }
    }
}
