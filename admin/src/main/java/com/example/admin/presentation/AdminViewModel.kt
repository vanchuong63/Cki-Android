package com.example.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.admin.data.AdminNotification
import com.example.admin.data.Machine
import com.example.admin.data.MachineInventory
import com.example.admin.data.SupabaseAdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor() : ViewModel() {

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

    val unreadCount = notifications.map { list ->
        list.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // ── Add Machine State ──────────────────────────────────────────────────────
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

    // ── Inventory ──────────────────────────────────────────────────────────────
    fun updateInventory(machineId: String, inventory: MachineInventory) {
        viewModelScope.launch {
            SupabaseAdminRepository.updateInventory(machineId, inventory)
            if (inventory.quantity <= inventory.minQuantity) {
                sendLowStockNotification(machineId, inventory)
            }
        }
    }

    private suspend fun sendLowStockNotification(
        machineId: String,
        inventory: MachineInventory
    ) {
        val machine = machines.value.find { it.id == machineId } ?: return
        val notif = AdminNotification(
            machineId = machineId,
            machineName = machine.name,
            message = "⚠️ ${machine.name}: ${inventory.productName} còn ${inventory.quantity} chai!",
        )
        SupabaseAdminRepository.addNotification(notif)
    }

    // ── Notifications ──────────────────────────────────────────────────────────
    fun markRead(id: String) {
        viewModelScope.launch { SupabaseAdminRepository.markNotificationRead(id) }
    }

    fun clearAddState() { _addMachineState.value = null }
}