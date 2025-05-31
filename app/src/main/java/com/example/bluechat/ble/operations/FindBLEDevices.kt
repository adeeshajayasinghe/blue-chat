package com.example.bluechat.ble.operations

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.bluechat.utils.UUIDManager

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun FindBLEDevices() {
    BluetoothSampleBox {
        FindDevicesScreen { device, broadcastUuid ->
            Log.d("FindDeviceSample", "Name: ${device.name} Address: ${device.address} Broadcast UUID: $broadcastUuid")
        }
    }
}

@SuppressLint("InlinedApi", "MissingPermission")
@RequiresApi(Build.VERSION_CODES.M)
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
internal fun FindDevicesScreen(onConnect: (BluetoothDevice, String?) -> Unit) {
    val context = LocalContext.current
    val adapter = checkNotNull(context.getSystemService(BluetoothManager::class.java).adapter)
    var scanning by remember {
        mutableStateOf(true)
    }
    val devices = remember {
        mutableStateListOf<BluetoothDevice>()
    }
    val deviceBroadcastUuids = remember {
        mutableStateMapOf<String, String>()
    }
    val pairedDevices = remember {
        // Get a list of previously paired devices
        mutableStateListOf<BluetoothDevice>(*adapter.bondedDevices.toTypedArray())
    }
    val sampleServerDevices = remember {
        mutableStateListOf<BluetoothDevice>()
    }
    val scanSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        .build()

    // This effect will start scanning for devices when the screen is visible
    // If scanning is stop removing the effect will stop the scanning.
    if (scanning) {
        BluetoothScanEffect(
            scanSettings = scanSettings,
            onScanFailed = { errorCode ->
                Log.e("FindDevicesScreen", "Scan failed with error: $errorCode")
            },
            onDeviceFound = { result ->
                val device = result.device
                val broadcastUuid = result.scanRecord?.serviceUuids?.find { 
                    UUIDManager.isOurUUID(it.uuid.toString())
                }?.uuid?.toString()
                
                if (broadcastUuid != null) {
                    val uniquePart = UUIDManager.getUniquePart(broadcastUuid)
                    Log.d("FindDevicesScreen", "Found device with broadcast UUID: $broadcastUuid (Unique part: $uniquePart)")
                    if (!devices.contains(device)) {
                        devices.add(device)
                        deviceBroadcastUuids[device.address] = broadcastUuid
                    }
                }
            }
        )
        // Stop scanning after a while
        LaunchedEffect(true) {
            delay(15000)
            scanning = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Available devices", style = MaterialTheme.typography.titleSmall)
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(
                    onClick = {
                        devices.clear()
                        deviceBroadcastUuids.clear()
                        scanning = true
                    },
                ) {
                    Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (devices.isEmpty()) {
                item {
                    Text(text = "No devices found")
                }
            }
            items(devices) { item ->
                BluetoothDeviceItem(
                    bluetoothDevice = item,
                    isSampleServer = sampleServerDevices.contains(item),
                    onConnect = { device, _ -> 
                        onConnect(device, deviceBroadcastUuids[device.address])
                    }
                )
            }

            if (pairedDevices.isNotEmpty()) {
                item {
                    Text(text = "Saved devices", style = MaterialTheme.typography.titleSmall)
                }
                items(pairedDevices) {
                    BluetoothDeviceItem(
                        bluetoothDevice = it,
                        onConnect = { device, _ -> onConnect(device, null) }
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
internal fun BluetoothDeviceItem(
    bluetoothDevice: BluetoothDevice,
    isSampleServer: Boolean = false,
    onConnect: (BluetoothDevice, String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
//            .clickable { onConnect(bluetoothDevice, null) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (isSampleServer) {
                "GATT Sample server"
            } else {
                bluetoothDevice.name ?: "N/A"
            },
            style = if (isSampleServer) {
                TextStyle(fontWeight = FontWeight.Bold)
            } else {
                TextStyle(fontWeight = FontWeight.Normal)
            },
        )

//        Text(bluetoothDevice.address)

        val state = when (bluetoothDevice.bondState) {
            BluetoothDevice.BOND_BONDED -> "Paired"
            BluetoothDevice.BOND_BONDING -> "Pairing"
            else -> "Connect"
        }
        if (state == "Connect") {
            Button(onClick = {onConnect(bluetoothDevice, null)}) {
                Text(text = state)
            }
        } else {
            Text(text = state)
        }
    }
}

@SuppressLint("InlinedApi")
@RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
@Composable
private fun BluetoothScanEffect(
    scanSettings: ScanSettings,
    onScanFailed: (Int) -> Unit,
    onDeviceFound: (device: ScanResult) -> Unit,
) {
    val context = LocalContext.current
    val adapter = context.getSystemService(BluetoothManager::class.java).adapter

    if (adapter == null) {
        onScanFailed(ScanCallback.SCAN_FAILED_INTERNAL_ERROR)
        return
    }

    val currentOnDeviceFound by rememberUpdatedState(onDeviceFound)

    DisposableEffect(scanSettings) {
        val leScanCallback: ScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                currentOnDeviceFound(result)
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                onScanFailed(errorCode)
            }
        }

        // Start scanning
        adapter.bluetoothLeScanner.startScan(null, scanSettings, leScanCallback)

        onDispose {
            adapter.bluetoothLeScanner.stopScan(leScanCallback)
        }
    }
}
