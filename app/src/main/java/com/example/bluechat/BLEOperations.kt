package com.example.bluechat

import android.os.Build
import androidx.compose.runtime.Composable
import com.example.bluechat.ble.operations.ConnectGATTSample
import com.example.bluechat.ble.operations.FindBLEDevices
import com.example.bluechat.server.GATTServerSample
import com.example.bluechat.shared.MinSdkBox
import com.example.bluechat.ui.ContactListScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluechat.viewmodel.ContactListViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.bluechat.ui.ChatHistoryScreen

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
        ComposableSampleDemo(
            id = "contact-list",
            name = "Contact List",
            description = "View and manage your chat contacts",
            documentation = null,
            tags = listOf("Bluetooth", "Contacts"),
            content = {
                var selectedDevice by remember { mutableStateOf<Pair<String, String?>?>(null) }
                val viewModel: ContactListViewModel = viewModel()
                val devices by viewModel.devices.collectAsState()
                val isLoading by viewModel.isLoading.collectAsState()
                val error by viewModel.error.collectAsState()

                if (selectedDevice != null) {
                    ChatHistoryScreen(
                        deviceId = selectedDevice!!.first,
                        deviceName = selectedDevice!!.second!!,
                        onNavigateBack = { selectedDevice = null }
                    )
                } else {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        ContactListScreen(
                            devices = devices,
                            onDeviceClick = { deviceId, deviceName ->
                                selectedDevice = deviceId to deviceName
                            },
                            onDeleteDevice = { device ->
                                viewModel.deleteDevice(device)
                            }
                        )
                    }

                    error?.let { errorMessage ->
                        LaunchedEffect(errorMessage) {
                            // Show error message (you might want to use a Snackbar or Toast)
                            viewModel.clearError()
                        }
                    }
                }
            },
        ),
    ).associateBy { it.id }
}