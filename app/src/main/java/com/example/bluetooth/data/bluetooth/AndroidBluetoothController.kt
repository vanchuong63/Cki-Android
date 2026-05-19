package com.example.bluetooth.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.bluetooth.data.mapper.toBluetoothDeviceDomain
import com.example.bluetooth.domain.controller.BluetoothController
import com.example.bluetooth.domain.model.BluetoothDeviceDomain
import com.example.bluetooth.domain.model.ConnectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var currentSocket: BluetoothSocket? = null
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevice: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevice: StateFlow<List<BluetoothDeviceDomain>> = _pairedDevices.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (devices.any { it.address == newDevice.address }) devices else devices + newDevice
        }
    }

    private var isReceiverRegistered = false

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        if (!isReceiverRegistered) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(foundDeviceReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(foundDeviceReceiver, filter)
            }
            isReceiverRegistered = true
        }
        _scannedDevices.update { emptyList() }
        updatedPairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> = emptyFlow()

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) throw SecurityException("Thiếu quyền")
            stopDiscovery()
            currentSocket?.close()
            currentSocket = bluetoothAdapter?.getRemoteDevice(device.address)?.createRfcommSocketToServiceRecord(MY_UUID)

            try {
                currentSocket?.connect()
                emit(ConnectionResult.ConnectionEstablished)
                listenForIncomingMessages()
            } catch (e: IOException) {
                currentSocket?.close()
                currentSocket = null
                emit(ConnectionResult.Error("Kết nối thất bại"))
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun listenForIncomingMessages() {
        Thread {
            val reader = currentSocket?.inputStream?.bufferedReader()

            while (currentSocket?.isConnected == true) {
                try {
                    val line = reader?.readLine()

                    if (line != null) {
                        Log.d("BluetoothLog", "Nhận: $line")
                        _incomingMessages.tryEmit(line.trim())
                    }

                } catch (e: IOException) {
                    Log.e("BluetoothLog", "Lỗi đọc: ${e.message}")
                    break
                }
            }
        }.start()
    }

    override suspend fun trySendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (currentSocket == null || !currentSocket!!.isConnected) {
                Log.e("BluetoothLog", "Socket không khả dụng!")
                return@withContext false
            }
            try {
                val outputStream = currentSocket?.outputStream
                outputStream?.write(message.toByteArray(Charsets.UTF_8))
                outputStream?.flush()
                Log.d("BluetoothLog", "Gửi: $message")
                true
            } catch (e: IOException) {
                Log.e("BluetoothLog", "Lỗi gửi: ${e.message}")
                false
            }
        }
    }

    override fun closeConnection() {
        currentSocket?.close()
        currentSocket = null
    }

    override fun release() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(foundDeviceReceiver)
            } catch (e: Exception) {}
            isReceiverRegistered = false
        }
        closeConnection()
    }

    private fun updatedPairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        val devices = bluetoothAdapter?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?: emptyList()
        _pairedDevices.update { devices }
    }

    private fun hasPermission(permission: String): Boolean = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
