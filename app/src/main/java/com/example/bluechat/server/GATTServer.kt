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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bluechat.ble.operations.BluetoothSampleBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.bluechat.BlueChatApplication
import com.example.bluechat.data.Message
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope


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
    val scope = rememberCoroutineScope()
    val repository = (context.applicationContext as BlueChatApplication).repository
    
    var enableServer by remember {
        mutableStateOf(GATTServerService.isServerRunning.value)
    }
    var enableAdvertising by remember(enableServer) {
        mutableStateOf(enableServer)
    }
    var showLogs by remember { mutableStateOf(false) }
    val logs by GATTServerService.serverLogsState.collectAsState()
    
    // Get messages from database
    val messages by repository.allMessages.collectAsState(initial = emptyList())

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
        
        // Chat section
//        if (enableServer) {
//            Text(
//                text = "Chat Messages",
//                style = MaterialTheme.typography.titleMedium,
//                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
//            )
//
//            if (messages.isNotEmpty()) {
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(200.dp)
//                ) {
//                    items(messages) { message ->
//                        androidx.compose.material3.Card(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 4.dp)
//                        ) {
//                            Column(
//                                modifier = Modifier.padding(12.dp)
//                            ) {
//                                Row(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    horizontalArrangement = Arrangement.SpaceBetween
//                                ) {
//                                    Text(
//                                        text = if (message.isSent) "Server" else message.senderId,
//                                        style = MaterialTheme.typography.labelMedium,
//                                        color = MaterialTheme.colorScheme.primary
//                                    )
//                                    Text(
//                                        text = formatTimestamp(message.timestamp.time),
//                                        style = MaterialTheme.typography.labelSmall
//                                    )
//                                }
//                                Text(
//                                    text = message.content,
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    modifier = Modifier.padding(top = 4.dp)
//                                )
//                            }
//                        }
//                    }
//                }
//            } else {
//                Text(
//                    text = "No messages received yet",
//                    style = MaterialTheme.typography.bodyMedium,
//                    modifier = Modifier.padding(vertical = 8.dp)
//                )
//            }
//
//            // Add clear messages button
//            Button(
//                onClick = {
//                    scope.launch {
//                        repository.deleteAllMessages()
//                    }
//                },
//                modifier = Modifier.padding(top = 8.dp)
//            ) {
//                Text("Clear Messages")
//            }
//
//            // Logs toggle button
//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 8.dp),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Server Logs",
//                    style = MaterialTheme.typography.titleMedium
//                )
//
//                androidx.compose.material3.Switch(
//                    checked = showLogs,
//                    onCheckedChange = { showLogs = it }
//                )
//            }
//
//            // Only show logs if toggle is enabled
//            if (showLogs) {
//                Text(text = logs)
//            }
//        }
    }
}

// Helper function to format timestamp
private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
