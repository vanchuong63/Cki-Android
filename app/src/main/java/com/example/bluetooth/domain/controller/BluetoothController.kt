package com.example.bluetooth.domain.controller

import com.example.bluetooth.domain.model.BluetoothDeviceDomain
import com.example.bluetooth.domain.model.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevice : StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevice : StateFlow<List<BluetoothDeviceDomain>>
    val incomingMessages: SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun startBluetoothServer(): Flow<ConnectionResult>

    fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult>

    suspend fun trySendMessage(message: String): Boolean

    fun closeConnection()

    fun release()
}