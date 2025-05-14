package com.example.bluechat.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val messageDao: MessageDao,
    private val deviceDao: DeviceDao
) {
    // Message operations
    val allMessages: Flow<List<Message>> = messageDao.getAllMessages()

    fun getMessagesWithDevice(deviceId: String): Flow<List<Message>> {
        return messageDao.getMessagesWithDevice(deviceId)
    }

    fun getMessagesWithDeviceName(deviceName: String): Flow<List<Message>> {
        return messageDao.getMessagesWithDeviceName(deviceName)
    }

    val allDeviceNames: Flow<List<String>> = messageDao.getAllDeviceNames()

    suspend fun insertMessage(message: Message) {
        messageDao.insertMessage(message)
    }

    suspend fun insertMessages(messages: List<Message>) {
        messageDao.insertMessages(messages)
    }

    suspend fun deleteMessage(message: Message) {
        messageDao.deleteMessage(message)
    }

    suspend fun deleteAllMessages() {
        messageDao.deleteAllMessages()
    }

    // Device operations
    val allDevices: Flow<List<Device>> = deviceDao.getAllDevices()

    suspend fun getDevice(name: String): Device? {
        return deviceDao.getDevice(name)
    }

    suspend fun insertDevice(device: Device) {
        deviceDao.insertDevice(device)
    }

    suspend fun deleteDevice(device: Device) {
        deviceDao.deleteDevice(device)
    }

    suspend fun deleteAllDevices() {
        deviceDao.deleteAllDevices()
    }
} 