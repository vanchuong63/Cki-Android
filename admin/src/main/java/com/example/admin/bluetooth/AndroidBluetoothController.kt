package com.example.admin.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.admin.bluetooth.domain.controller.BluetoothController
import com.example.admin.bluetooth.domain.model.BluetoothDeviceDomain
import com.example.admin.bluetooth.domain.model.ConnectionResult
import com.example.admin.bluetooth.mapper.toBluetoothDeviceDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

private const val TAG = "BT_ADMIN"

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
        val newDevice = device.toBluetoothDeviceDomain()
        Log.d(TAG, "📡 Thấy thiết bị: ${newDevice.name ?: "Unknown"} | ${newDevice.address}")
        _scannedDevices.update { devices ->
            if (devices.any { it.address == newDevice.address }) devices else devices + newDevice
        }
    }

    private var isReceiverRegistered = false

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            Log.e(TAG, "❌ Thiếu quyền BLUETOOTH_SCAN")
            return
        }
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
        Log.d(TAG, "🔍 Bắt đầu scan...")
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun startBluetoothServer(): Flow<ConnectionResult> = emptyFlow()

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                emit(ConnectionResult.Error("Thiếu quyền BLUETOOTH_CONNECT"))
                return@flow
            }

            Log.d(TAG, "🔗 Đang kết nối: ${device.name} (${device.address})")
            stopDiscovery()
            currentSocket?.close()

            // ✅ Dùng đúng cách như app — chỉ Secure socket, không thêm Insecure
            currentSocket = bluetoothAdapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(MY_UUID)

            try {
                currentSocket?.connect()
                Log.d(TAG, "🎉 ===== KẾT NỐI ESP32 THÀNH CÔNG =====")
                emit(ConnectionResult.ConnectionEstablished)
                listenForIncomingMessages()
            } catch (e: IOException) {
                Log.e(TAG, "❌ Kết nối thất bại: ${e.message}")
                currentSocket?.close()
                currentSocket = null
                emit(ConnectionResult.Error("Kết nối thất bại: ${e.message}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun listenForIncomingMessages() {
        Log.d(TAG, "👂 Lắng nghe tin nhắn từ ESP32...")
        Thread {
            val reader = currentSocket?.inputStream?.bufferedReader()
            while (currentSocket?.isConnected == true) {
                try {
                    val line = reader?.readLine()
                    if (line != null) {
                        Log.d(TAG, "📨 ESP32 gửi: $line")
                        _incomingMessages.tryEmit(line.trim())
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "💔 Mất kết nối: ${e.message}")
                    break
                }
            }
            Log.w(TAG, "⚠️ Vòng lặp lắng nghe kết thúc")
        }.start()
    }

    override suspend fun trySendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "📤 Gửi lệnh: '${message.trim()}' | Connected: ${currentSocket?.isConnected}")
            if (currentSocket == null || !currentSocket!!.isConnected) {
                Log.e(TAG, "❌ Socket null hoặc chưa kết nối!")
                return@withContext false
            }
            try {
                val outputStream = currentSocket?.outputStream
                outputStream?.write(message.toByteArray(Charsets.UTF_8))
                outputStream?.flush()
                Log.d(TAG, "✅ Gửi thành công!")
                true
            } catch (e: IOException) {
                Log.e(TAG, "❌ Lỗi gửi: ${e.message}")
                false
            }
        }
    }

    override fun closeConnection() {
        currentSocket?.close()
        currentSocket = null
        Log.d(TAG, "🔌 Đã đóng kết nối")
    }

    override fun release() {
        if (isReceiverRegistered) {
            try { context.unregisterReceiver(foundDeviceReceiver) } catch (e: Exception) {}
            isReceiverRegistered = false
        }
        closeConnection()
    }

    private fun updatedPairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) return
        val devices = bluetoothAdapter?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?: emptyList()
        Log.d(TAG, "📋 Thiết bị đã ghép: ${devices.map { it.name }}")
        _pairedDevices.update { devices }
    }

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}