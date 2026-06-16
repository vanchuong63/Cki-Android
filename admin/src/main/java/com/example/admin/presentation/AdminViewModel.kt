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

    // ── 1. Khai báo Inventory State trước ─────────────────────────────────────
    private val _inventoryState = MutableStateFlow<List<MachineInventory>>(emptyList())
    val inventoryState = _inventoryState.asStateFlow()

    // ── 2. Khai báo Notifications sử dụng Inventory State ─────────────────────
    private val rawNotifications = SupabaseAdminRepository.getNotificationsFlow()

    val notifications = combine(rawNotifications, _inventoryState) { notifs, inventory ->
        notifs.filter { notif ->
            val machineInv = inventory.filter { it.machineId == notif.machineId }
            if (machineInv.isEmpty()) return@filter true

            val mentionedProduct = machineInv.find { notif.message.contains(it.productName, ignoreCase = true) }
            if (mentionedProduct != null) {
                mentionedProduct.quantity <= mentionedProduct.minQuantity
            } else {
                machineInv.any { it.quantity <= it.minQuantity }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount = notifications.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Luồng tự động quét thiết bị Bluetooth
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
                delay(15000)
            }
        }

        // Tự động kết nối Bluetooth
        bluetoothController.scannedDevice
            .onEach { devices ->
                val target = devices.find {
                    it.name?.trim()?.equals(TARGET_DEVICE_NAME, true) == true
                }
                val s = _state.value
                if (target != null && !s.isConnected && !s.isConnecting) {
                    connectToDevice(target)
                }
            }.launchIn(viewModelScope)

        // Theo dõi kho hàng và dọn dẹp thông báo
        SupabaseAdminRepository.getInventoryFlow()
            .onEach { remoteInventory ->
                _inventoryState.value = remoteInventory
                remoteInventory.groupBy { it.machineId }.forEach { (mId, items) ->
                    if (items.all { it.quantity > it.minQuantity }) {
                        viewModelScope.launch {
                            SupabaseAdminRepository.markAllMachineNotificationsRead(mId)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    // ── Các hàm xử lý ──────────────────────────────────────────────────────────
    fun connectToDevice(device: BluetoothDeviceDomain) {
        if (_state.value.isConnecting && lastConnectedDevice?.address == device.address) return
        connectionJob?.cancel()
        lastConnectedDevice = device
        _state.update { it.copy(isConnecting = true) }
        
        connectionJob = bluetoothController.connectToDevice(device)
            .onEach { result ->
                when (result) {
                    is ConnectionResult.ConnectionEstablished -> {
                        _state.update { it.copy(isConnected = true, isConnecting = false) }
                        stopScan()
                    }
                    is ConnectionResult.Error -> {
                        _state.update { it.copy(isConnected = false, isConnecting = false) }
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun startScan() = bluetoothController.startDiscovery()
    fun stopScan() = bluetoothController.stopDiscovery()

    val todayOrders = SupabaseAdminRepository.getTodayOrdersFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val todayRevenue = SupabaseAdminRepository.getTodayRevenueFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val totalRevenue = SupabaseAdminRepository.getTotalRevenueFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val totalOrdersCount = SupabaseAdminRepository.getTotalOrdersCountFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val monthlyRevenue = SupabaseAdminRepository.getMonthlyRevenueFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val recentOrders = SupabaseAdminRepository.getRecentOrdersFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val machines = SupabaseAdminRepository.getMachinesFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            SupabaseAdminRepository.updateInventory(machineId, inventory)
                .onSuccess { updatedInventory ->
                    _inventoryState.update { current ->
                        current.map { if (it.productId == updatedInventory.productId) updatedInventory else it }
                    }
                    if (updatedInventory.quantity > updatedInventory.minQuantity) {
                        SupabaseAdminRepository.markAllMachineNotificationsRead(machineId)
                    }
                }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch { SupabaseAdminRepository.markNotificationRead(id) }
    }

    fun clearAddState() { _addMachineState.value = null }
    fun toggleMaintenanceDoor(isOpen: Boolean) {
        viewModelScope.launch {
            bluetoothController.trySendMessage(if (isOpen) "OPEN_DOOR\n" else "CLOSE_DOOR\n")
        }
    }
}
