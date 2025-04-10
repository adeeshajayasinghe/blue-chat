package com.example.bluechat.server

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bluechat.ble.operations.BluetoothSampleBox


@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun GATTServerSample() {
    // In addition to the Bluetooth permissions we also need the BLUETOOTH_ADVERTISE from Android 12
    val extraPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        setOf(Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        emptySet()
    }
    BluetoothSampleBox(extraPermissions = extraPermissions) { adapter ->
        if (adapter.isMultipleAdvertisementSupported) {
            GATTServerScreen()
        } else {
            Text(text = "Cannot run server:\nDevices does not support multi-advertisement")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@Composable
internal fun GATTServerScreen() {
    val context = LocalContext.current
    var enableServer by remember {
        mutableStateOf(GATTServerService.isServerRunning.value)
    }
    var enableAdvertising by remember(enableServer) {
        mutableStateOf(enableServer)
    }
    val logs by GATTServerService.serverLogsState.collectAsState()

    LaunchedEffect(enableServer, enableAdvertising) {
        val intent = Intent(context, GATTServerService::class.java).apply {
            action = if (enableAdvertising) {
                GATTServerService.ACTION_START_ADVERTISING
            } else {
                GATTServerService.ACTION_STOP_ADVERTISING
            }
        }
        if (enableServer) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { enableServer = !enableServer }) {
                Text(text = if (enableServer) "Stop Server" else "Start Server")
            }

            Button(onClick = { enableAdvertising = !enableAdvertising }, enabled = enableServer) {
                Text(text = if (enableAdvertising) "Stop Advertising" else "Start Advertising")
            }
        }
        Text(text = logs)
    }
}
