package com.example.bluechat

import android.os.Build
import androidx.compose.runtime.Composable
import com.example.bluechat.ble.operations.FindBLEDevices
import com.example.bluechat.shared.MinSdkBox

interface SampleDemo : CatalogItem {
    override val id: String
    override val name: String
    override val description: String?
    val documentation: String?
    val minSdk: Int
    val tags: List<String>
    val content: Any
}

data class ComposableSampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val minSdk: Int = Build.VERSION_CODES.LOLLIPOP,
    override val tags: List<String> = emptyList(),
    override val content: @Composable () -> Unit,
) : SampleDemo

data class ActivitySampleDemo(
    override val id: String,
    override val name: String,
    override val description: String? = null,
    override val documentation: String? = null,
    override val minSdk: Int = Build.VERSION_CODES.LOLLIPOP,
    override val tags: List<String> = emptyList(),
    override val content: Class<*>,
) : SampleDemo

val BLE_OPERATIONS by lazy {
    listOf(
        ComposableSampleDemo(
            id = "create-gatt-server",
            name = "Create a GATT server",
            description = "Shows how to create a GATT server and communicate with the GATT client",
            documentation = "https://developer.android.com/reference/android/bluetooth/BluetoothGattServer",
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
//                    GATTServerSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "scan-with-ble-intent",
            name = "Scan with BLE Intent",
            description = "This samples shows how to use the BLE intent to scan for devices",
            documentation = "https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(java.util.List%3Candroid.bluetooth.le.ScanFilter%3E,%20android.bluetooth.le.ScanSettings,%20android.app.PendingIntent)",
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.O) {
                    //noinspection NewApi
//                    BLEScanIntentSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "connect-gatt-server",
            name = "Connect to a GATT server",
            description = "Shows how to connect to a GATT server hosted by the BLE device and perform simple operations",
            documentation = "https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server",
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
//                    ConnectGATTSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "find-devices",
            name = "Find devices",
            description = "This example will demonstrate how to scanning for Low Energy Devices",
            documentation = "https://developer.android.com/guide/topics/connectivity/bluetooth",
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
                    FindBLEDevices()
                }
            },
        ),
    ).associateBy { it.id }
}