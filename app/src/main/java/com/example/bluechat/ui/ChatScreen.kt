package com.example.bluechat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluechat.data.Message
import com.example.bluechat.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

//@Composable
//fun ChatScreen(
//    deviceId: String,
//    viewModel: ChatViewModel = viewModel()
//) {
//    val messages by viewModel.messages.collectAsState()
//    val isLoading by viewModel.isLoading.collectAsState()
//    val error by viewModel.error.collectAsState()
//    var messageText by remember { mutableStateOf("") }
//
//    // Load messages when the screen is first displayed
//    LaunchedEffect(deviceId) {
//        viewModel.loadMessagesForDevice(deviceId)
//    }
//
//    // Show error if any
//    error?.let { errorMessage ->
//        LaunchedEffect(errorMessage) {
//            // Show error message (you might want to use a Snackbar or Toast)
//        }
//    }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        // Messages list
//        Box(
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth()
//        ) {
//            if (isLoading) {
//                CircularProgressIndicator(
//                    modifier = Modifier.align(Alignment.Center)
//                )
//            } else {
//                LazyColumn(
//                    modifier = Modifier.fillMaxSize(),
//                    reverseLayout = true
//                ) {
//                    items(messages.reversed()) { message ->
//                        MessageItem(message)
//                    }
//                }
//            }
//        }
//
//        // Message input
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(vertical = 8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            TextField(
//                value = messageText,
//                onValueChange = { messageText = it },
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(end = 8.dp),
//                placeholder = { Text("Type a message...") }
//            )
//
//            Button(
//                onClick = {
//                    if (messageText.isNotBlank()) {
//                        viewModel.addMessage(
//                            content = messageText,
//                            senderId = "current_user", // Replace with actual user ID
//                            receiverId = deviceId,
//                            deviceName = ,
//                            isSent = true
//                        )
//                        messageText = ""
//                    }
//                },
//                enabled = messageText.isNotBlank()
//            ) {
//                Text("Send")
//            }
//        }
//    }
//}

@Composable
fun MessageItem(message: Message) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
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