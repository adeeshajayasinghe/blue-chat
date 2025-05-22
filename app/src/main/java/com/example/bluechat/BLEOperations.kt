package com.example.bluechat

import android.os.Build
import androidx.compose.runtime.Composable
import com.example.bluechat.ble.operations.ConnectGATTSample
import com.example.bluechat.ble.operations.FindBLEDevices
import com.example.bluechat.server.GATTServerSample
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
            name = "Host a Chat",
            description = "Start a chat session that other devices can join",
            documentation = "https://developer.android.com/reference/android/bluetooth/BluetoothGattServer",
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
                    GATTServerSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "connect-gatt-server",
            name = "Join a Chat",
            description = "Connect to an existing chat session hosted by another device",
            documentation = "https://developer.android.com/guide/topics/connectivity/bluetooth/connect-gatt-server",
            tags = listOf("Bluetooth"),
            content = {
                MinSdkBox(minSdk = Build.VERSION_CODES.M) {
                    //noinspection NewApi
                    ConnectGATTSample()
                }
            },
        ),
        ComposableSampleDemo(
            id = "find-devices",
            name = "Find Nearby Devices",
            description = "Discover Bluetooth devices available for chat",
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