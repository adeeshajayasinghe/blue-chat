package com.example.bluechat.server

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bluechat.ble.operations.toConnectionStateString
import com.example.bluechat.data.Message
import com.example.bluechat.data.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

@RequiresApi(Build.VERSION_CODES.M)
class GATTServerService : Service() {
    companion object {
        // Random UUID for our service known between the client and server to allow communication
        val SERVICE_UUID: UUID = UUID.fromString("00002222-0000-1000-8000-00805f9b34fb")

        // Same as the service but for the characteristic
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("00001111-0000-1000-8000-00805f9b34fb")

        // New broadcast UUID
        val BROADCAST_UUID: UUID = UUID.fromString("00003333-0000-1000-8000-00805f9b34fb")

        const val ACTION_START_ADVERTISING = "start_ad"
        const val ACTION_STOP_ADVERTISING = "stop_ad"

        // Important: this is just for simplicity, there are better ways to communicate between
        // a service and an activity/view
        val serverLogsState: MutableStateFlow<String> = MutableStateFlow("")
        val isServerRunning = MutableStateFlow(false)
        
        // For backward compatibility
        val lastReceivedMessage = MutableStateFlow("")

        private const val CHANNEL = "gatt_server_channel"
    }

    private val manager: BluetoothManager by lazy {
        applicationContext.getSystemService(BluetoothManager::class.java)
    }
    private val advertiser: BluetoothLeAdvertiser
        get() = manager.adapter.bluetoothLeAdvertiser

    private val service =
        BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).also {
            it.addCharacteristic(
                BluetoothGattCharacteristic(
                    CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ,
                ),
            )
        }

    private lateinit var server: BluetoothGattServer
    private lateinit var repository: ChatRepository

    private val scope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        // Initialize repository
        repository = (application as com.example.bluechat.BlueChatApplication).repository
        
        // If we are missing permission stop the service
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            PackageManager.PERMISSION_GRANTED
        }
        if (permission == PackageManager.PERMISSION_GRANTED) {
            startInForeground()

            serverLogsState.value = "Opening GATT server...\n"
            server = manager.openGattServer(applicationContext, SampleServerCallback())
            server.addService(service)
            isServerRunning.value = true
        } else {
            serverLogsState.value = "Missing connect permission\n"
            stopSelf()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || !hasAdvertisingPermission()) {
            return START_NOT_STICKY
        }
        when (intent.action) {
            ACTION_START_ADVERTISING -> {
                serverLogsState.value += "Start advertising\n"
                startAdvertising()
            }

            ACTION_STOP_ADVERTISING -> {
                serverLogsState.value += "Stop advertising\n"
                advertiser.stopAdvertising(SampleAdvertiseCallback)
            }

            else -> throw IllegalArgumentException("Unknown action")
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        isServerRunning.value = false
        if (hasAdvertisingPermission()) {
            advertiser.stopAdvertising(SampleAdvertiseCallback)
        }
        server.close()
        scope.cancel()
        serverLogsState.value += "Server destroyed\n"
    }

    private fun startInForeground() {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle("GATT Server")
            .setContentText("Running...")
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                100,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(100, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL,
            NotificationManagerCompat.IMPORTANCE_HIGH,
        )
            .setName("GATT Server channel")
            .setDescription("Channel for the GATT server sample")
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun hasAdvertisingPermission() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || (ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADVERTISE,
        ) == PackageManager.PERMISSION_GRANTED)

    @SuppressLint("InlinedApi")
    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .addServiceUuid(ParcelUuid(BROADCAST_UUID))
            .build()

        advertiser.startAdvertising(settings, data, SampleAdvertiseCallback)
    }

    @SuppressLint("MissingPermission")
    inner class SampleServerCallback : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(
            device: BluetoothDevice,
            status: Int,
            newState: Int,
        ) {
            // Log device details when connection state changes
            val deviceName = device.name ?: "Unknown Device"
            val deviceAddress = device.address
            val deviceType = device.type
            val deviceBondState = device.bondState
            val deviceId = device.toString()

            Log.d("GATTServer", "Device: $deviceName, Address: $deviceAddress, Type: $deviceType, Device ID: $deviceId")
            Log.d("GATTServer", "Broadcast UUID: $BROADCAST_UUID")

            // Check if the device is already in the database
//            scope.launch(Dispatchers.IO) {
//                val existingDevice = repository.getDevice(deviceName)
//                if (existingDevice == null) {
//                    val newDevice = Device(
//                        name = deviceName,
//                        address = deviceAddress,
//                        lastConnected = System.currentTimeMillis(),
//                        lastMessageTimestamp = null
//                    )
//                    repository.insertDevice(newDevice)
//                    serverLogsState.value += "Device saved to database: $deviceName\n"
//                    Log.d("CHAT MESSAGE", "Message received from device address: $deviceAddress")
//                } else {
//                    serverLogsState.value += "Device already exists in database: $deviceName\n"
//                }
//            }
            
            serverLogsState.value += """
                Connection state change: ${newState.toConnectionStateString()}
                Device status:
                - Name: ${deviceName ?: "null"}
                - Address: $deviceAddress
                - Type: $deviceType
                - Bond State: $deviceBondState
                - Status: $status
            """.trimIndent() + "\n"
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray,
        ) {
            val message = String(value)
            val timestamp = Date()
            
            // Log device details when receiving a message
            val deviceName = device.name
            val deviceAddress = device.address
            
            serverLogsState.value += """
                Message received from device:
                - Name: ${deviceName ?: "null"}
                - Address: $deviceAddress
                - Message: $message
            """.trimIndent() + "\n"

            // Store message in database
            scope.launch(Dispatchers.IO) {
                val broadcastUuid = ParcelUuid(BROADCAST_UUID)
//                val deviceUuid = result.scanRecord?.serviceUuids?.find { it == broadcastUuid }?.uuid?.toString()

                val messageEntity = Message(
                    content = message,
                    senderId = deviceAddress,
                    receiverId = "Server",
                    deviceUuid = broadcastUuid.toString(),
                    timestamp = timestamp,
                    isSent = false
                )
                repository.insertMessage(messageEntity)
            }
            
            // Update last message for backward compatibility
            lastReceivedMessage.value = message
            
            serverLogsState.value += "Message received from ${deviceName ?: deviceAddress}: $message\n"
            
            // Here you should apply the write of the characteristic and notify connected
            // devices that it changed

            // If response is needed reply to the device that the write was successful
            if (responseNeeded) {
                server.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null,
                )
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?,
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            serverLogsState.value += "Characteristic Read request: $requestId (offset $offset)\n"
            
            // Send back the last received message
            val data = lastReceivedMessage.value.toByteArray()
            val response = data.copyOfRange(offset, data.size)
            server.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                response,
            )
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            serverLogsState.value += "\nMTU change request: $mtu\n"
        }
    }

    object SampleAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            serverLogsState.value += "\nStarted advertising\n"
        }

        override fun onStartFailure(errorCode: Int) {
            serverLogsState.value += "\nFailed to start advertising: $errorCode\n"
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}