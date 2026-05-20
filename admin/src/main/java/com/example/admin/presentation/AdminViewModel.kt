package com.example.admin.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.bluetooth.BluetoothUiState
import com.example.admin.bluetooth.domain.controller.BluetoothController
import com.example.admin.bluetooth.domain.model.BluetoothDeviceDomain
import com.example.admin.bluetooth.domain.model.ConnectionResult
import com.example.admin.data.AdminNotification
import com.example.admin.data.Machine
import com.example.admin.data.MachineInventory
import com.example.admin.data.SupabaseAdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val bluetoothController: BluetoothController
) : ViewModel() {

    private val TARGET_DEVICE_NAME = "ESP32-Bluetooth"
    private var lastConnectedDevice: BluetoothDeviceDomain? = null
    private var connectionJob: Job? = null

    private val _state = MutableStateFlow(BluetoothUiState())

    val state = combine(
        bluetoothController.scannedDevice,
        bluetoothController.pairedDevice,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(scannedDevices = scannedDevices, pairedDevices = pairedDevices)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    init {
        // 1. Luồng tự động quét thiết bị: Kiểm tra trực tiếp từ _state gốc để tránh trễ dữ liệu
        viewModelScope.launch {
            while (true) {
                val s = _state.value
                if (!s.isConnected && !s.isConnecting) {
                    startScan()
                    delay(5000)
                    stopScan()
                } else {
                    stopScan()
                }
                delay(15000) // Tăng thời gian chờ để ổn định kết nối
            }
        }

        // 2. Tự động kết nối: Chỉ gọi khi thực sự chưa kết nối
        bluetoothController.scannedDevice
            .onEach { devices ->
                val target = devices.find {
                    it.name?.trim()?.equals(TARGET_DEVICE_NAME, true) == true
                }
                val s = _state.value
                if (target != null && !s.isConnected && !s.isConnecting) {
                    Log.d("AdminBluetooth", "Đã thấy ESP32, đang tiến hành kết nối...")
                    connectToDevice(target)
                }
            }.launchIn(viewModelScope)
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        // Nếu đang kết nối đúng thiết bị rồi thì không làm gì cả
        if (_state.value.isConnecting && lastConnectedDevice?.address == device.address) return
        
        connectionJob?.cancel() // Hủy bỏ tiến trình kết nối cũ nếu đang chạy
        lastConnectedDevice = device
        _state.update { it.copy(isConnecting = true) }
        
        connectionJob = bluetoothController.connectToDevice(device)
            .onEach { result ->
                when (result) {
                    is ConnectionResult.ConnectionEstablished -> {
                        _state.update { it.copy(isConnected = true, isConnecting = false) }
                        Log.d("AdminBluetooth", "✅ KẾT NỐI THÀNH CÔNG!")
                        stopScan()
                    }
                    is ConnectionResult.Error -> {
                        _state.update { it.copy(isConnected = false, isConnecting = false) }
                        Log.e("AdminBluetooth", "❌ Lỗi kết nối: ${result.message}")
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun startScan() = bluetoothController.startDiscovery()
    fun stopScan() = bluetoothController.stopDiscovery()

    // ── Stats ──────────────────────────────────────────────────────────────────
    val todayOrders = SupabaseAdminRepository.getTodayOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayRevenue = SupabaseAdminRepository.getTodayRevenueFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalRevenue = com.example.admin.data.SupabaseAdminRepository.getTotalRevenueFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalOrdersCount = com.example.admin.data.SupabaseAdminRepository.getTotalOrdersCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monthlyRevenue = com.example.admin.data.SupabaseAdminRepository.getMonthlyRevenueFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val recentOrders = com.example.admin.data.SupabaseAdminRepository.getRecentOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Machines ───────────────────────────────────────────────────────────────
    val machines = SupabaseAdminRepository.getMachinesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Notifications ──────────────────────────────────────────────────────────
    val notifications = SupabaseAdminRepository.getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationState = notifications

    val unreadCount = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _inventoryState = MutableStateFlow<List<MachineInventory>>(emptyList())
    val inventoryState = _inventoryState.asStateFlow()
    private val pendingInventoryUpdates = MutableStateFlow<Map<String, MachineInventory>>(emptyMap())

    init {
        SupabaseAdminRepository.getInventoryFlow()
            .onEach { remoteInventory ->
                val pending = pendingInventoryUpdates.value
                val resolvedKeys = pending.filter { (key, pendingItem) ->
                    remoteInventory.any { remoteItem ->
                        inventoryKey(remoteItem) == key && remoteItem.quantity == pendingItem.quantity
                    }
                }.keys

                if (resolvedKeys.isNotEmpty()) {
                    pendingInventoryUpdates.update { updates -> updates - resolvedKeys }
                }

                val activePending = pendingInventoryUpdates.value
                _inventoryState.value = remoteInventory.map { item ->
                    activePending[inventoryKey(item)] ?: item
                }
            }
            .launchIn(viewModelScope)
    }

    private val _addMachineState = MutableStateFlow<String?>(null)
    val addMachineState = _addMachineState.asStateFlow()

    fun addMachine(name: String, location: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            val machine = Machine(name = name, location = location, lat = lat, lng = lng)
            SupabaseAdminRepository.addMachine(machine)
                .onSuccess { _addMachineState.value = "✅ Thêm máy thành công!" }
                .onFailure { _addMachineState.value = "❌ Lỗi: ${it.message}" }
        }
    }

    fun deleteMachine(machineId: String) {
        viewModelScope.launch {
            SupabaseAdminRepository.deleteMachine(machineId)
        }
    }

    fun updateInventory(machineId: String, inventory: MachineInventory) {
        viewModelScope.launch {
            val updatedInventory = inventory.copy(machineId = machineId)
            val key = inventoryKey(updatedInventory)

            pendingInventoryUpdates.update { it + (key to updatedInventory) }
            _inventoryState.update { current ->
                current.map { item ->
                    if (inventoryKey(item) == key || item.productId == inventory.productId) updatedInventory else item
                }
            }

            SupabaseAdminRepository.updateInventory(machineId, inventory)
                .onSuccess {
                    _inventoryState.update { current ->
                        current.map { item ->
                            if (inventoryKey(item) == key || item.productId == inventory.productId) updatedInventory else item
                        }
                    }
                    if (inventory.quantity <= inventory.minQuantity) {
                        sendLowStockNotification(machineId, inventory)
                    }
                }
                .onFailure { error ->
                    Log.e("AdminInventory", "Cap nhat kho that bai: ${error.message}", error)
                    pendingInventoryUpdates.update { it - key }
                }
        }
    }

    private fun inventoryKey(inventory: MachineInventory): String =
        if (inventory.machineId.isNotBlank()) {
            "${inventory.machineId}|${inventory.productId}"
        } else {
            inventory.productId
        }

    private suspend fun sendLowStockNotification(machineId: String, inventory: MachineInventory) {
        val machine = machines.value.find { it.id == machineId } ?: return
        val notif = AdminNotification(
            machineId = machineId,
            machineName = machine.name,
            message = "⚠️ ${machine.name}: ${inventory.productName} còn ${inventory.quantity} chai!",
        )
        SupabaseAdminRepository.addNotification(notif)
    }

    fun markRead(id: String) {
        viewModelScope.launch { SupabaseAdminRepository.markNotificationRead(id) }
    }

    fun clearAddState() { _addMachineState.value = null }

    fun toggleMaintenanceDoor(isOpen: Boolean) {
        viewModelScope.launch {
            val cmd = if (isOpen) "OPEN_DOOR\n" else "CLOSE_DOOR\n"
            Log.d("AdminBluetooth", "Gửi lệnh: $cmd")
            val sent = bluetoothController.trySendMessage(cmd)
            if (!sent) Log.e("AdminBluetooth", "Lỗi: Không gửi được lệnh. BT chưa kết nối.")
        }
    }
}
