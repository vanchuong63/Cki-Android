package com.example.bluetooth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.data.remote.SePayApi
import com.example.bluetooth.domain.controller.BluetoothController
import com.example.bluetooth.domain.model.BluetoothDeviceDomain
import com.example.bluetooth.domain.model.ConnectionResult
import com.example.bluetooth.presentation.user.SampleBeverages
import com.example.bluetooth.subpabase.Order
import com.example.bluetooth.subpabase.SessionManager
import com.example.bluetooth.subpabase.SupabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val session: SessionManager
): ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevice,
        bluetoothController.pairedDevice,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private val _paymentStatus = MutableSharedFlow<Boolean>()
    val paymentStatus = _paymentStatus.asSharedFlow()

    private val TARGET_DEVICE_NAME = "ESP32-Bluetooth"

    private val seenTransactionIds = mutableSetOf<String>()
    private var paymentCheckJob: Job? = null
    private var orderFinalized = false

    private companion object {
        const val FREE_ORDER_THRESHOLD = 10
    }

    // 🔥 SEPAY CONFIG
    private val API_TOKEN = "6UEOJPRT4BXY35YID8ICK2WPVPTR9NOZBQSUXD1PBLQN0WJVKMODLKGYWCEBHE5K"
    private val ACCOUNT_NUMBER = "VQRQAIDDN1936"

    private val sePayApi: SePayApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://my.sepay.vn/userapi/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(SePayApi::class.java)
    }

    init {
        Log.d("BluetoothLog", "ViewModel started")
        
        // 🔥 VÒNG LẶP TỰ ĐỘNG QUÉT VÀ KẾT NỐI
        viewModelScope.launch {
            while (true) {
                if (!state.value.isConnected && !state.value.isConnecting) {
                    Log.d("BluetoothLog", "Đang tự động tìm kiếm máy bán nước...")
                    startScan()
                }
                delay(10000)
            }
        }

        // 🔥 TỰ ĐỘNG KẾT NỐI KHI THẤY ESP32 TRONG DANH SÁCH QUÉT
        bluetoothController.scannedDevice
            .onEach { devices ->
                val target = devices.find {
                    it.name?.trim()?.equals(TARGET_DEVICE_NAME, true) == true
                }

                if (target != null && !state.value.isConnected && !state.value.isConnecting) {
                    Log.d("BluetoothLog", "Đã tìm thấy máy! Đang kết nối...")
                    stopScan()
                    connectToDevice(target)
                }
            }
            .launchIn(viewModelScope)

        // 🔥 LẮNG NGHE PHẢN HỒI TỪ ESP32
        bluetoothController.incomingMessages
            .onEach { msg ->
                Log.d("BluetoothLog", "ESP: $msg")
                if (msg.contains("COMPLETED") && state.value.selectedProduct != null) {
                    completeSelectedOrder()
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun loadBaseline() {
        try {
            val res = sePayApi.getTransactions("Bearer $API_TOKEN", ACCOUNT_NUMBER)
            seenTransactionIds.clear()
            res.transactions.forEach {
                it.id?.let { id -> seenTransactionIds.add(id) }
            }
        } catch (e: Exception) {
            Log.e("SePayLog", "Lỗi lấy dữ liệu ban đầu: ${e.message}")
        }
    }

    private fun startCheckingPayment() {
        paymentCheckJob?.cancel()
        paymentCheckJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < 120000) {
                try {
                    val res = sePayApi.getTransactions("Bearer $API_TOKEN", ACCOUNT_NUMBER)
                    val newTx = res.transactions.firstOrNull {
                        it.id != null && it.id !in seenTransactionIds
                    }

                    if (newTx != null) {
                        Log.d("SePayLog", "Thanh toán thành công ID: ${newTx.id}")
                        
                        // QUY ĐỊNH KÍ HIỆU GỬI CHO ESP32
                        val success = sendSelectedProductCommand()

                        if (success) {
                            seenTransactionIds.add(newTx.id!!)
                            completeSelectedOrder()
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SePayLog", "Lỗi API: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    fun selectProduct(productId: String) {
        val selectedProduct = SampleBeverages.firstOrNull { it.id == productId }?.let {
            SelectedProduct(
                id = it.id,
                name = it.name,
                price = it.price
            )
        } ?: SelectedProduct(
            id = productId,
            name = productId,
            price = 0
        )

        orderFinalized = false
        _state.update {
            it.copy(
                selectedProduct = selectedProduct,
                isCheckingReward = true,
                isFreeOrder = false,
                paidOrdersUntilFree = 0,
                isCompletingOrder = false,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            val paidOrdersUntilFree = getPaidOrdersSinceLastFree()
            val isFreeOrder = !session.isGuest && paidOrdersUntilFree >= FREE_ORDER_THRESHOLD

            _state.update {
                it.copy(
                    isCheckingReward = false,
                    isFreeOrder = isFreeOrder,
                    paidOrdersUntilFree = paidOrdersUntilFree.coerceAtMost(FREE_ORDER_THRESHOLD)
                )
            }

            if (isFreeOrder) {
                paymentCheckJob?.cancel()
                return@launch
            }

            if (state.value.isConnected) {
                loadBaseline()
                startCheckingPayment()
            } else {
                Log.e("BluetoothLog", "Chưa kết nối với máy bán nước")
            }
        }
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        _state.update { it.copy(isConnecting = true) }
        bluetoothController.connectToDevice(device)
            .onEach { result ->
                when (result) {
                    is ConnectionResult.ConnectionEstablished -> {
                        Log.d("BluetoothLog", "Đã kết nối thành công")
                        _state.update { it.copy(isConnected = true, isConnecting = false, errorMessage = null) }
                    }
                    is ConnectionResult.Error -> {
                        Log.e("BluetoothLog", "Lỗi kết nối: ${result.message}")
                        _state.update {
                            it.copy(isConnected = false, isConnecting = false, errorMessage = result.message)
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun claimFreeOrder() {
        val currentState = state.value
        if (!currentState.isFreeOrder || currentState.selectedProduct == null || currentState.isCompletingOrder) return

        viewModelScope.launch {
            _state.update { it.copy(isCompletingOrder = true, errorMessage = null) }
            val success = sendSelectedProductCommand()
            if (success) {
                completeSelectedOrder()
            } else {
                _state.update {
                    it.copy(
                        isCompletingOrder = false,
                        errorMessage = "Khong gui duoc lenh den may ban nuoc"
                    )
                }
            }
        }
    }

    fun onPaymentDetected() {
        _state.update {
            it.copy(
                selectedProduct = null,
                isCheckingReward = false,
                isFreeOrder = false,
                paidOrdersUntilFree = 0,
                isCompletingOrder = false
            )
        }
        paymentCheckJob?.cancel()
    }

    fun startScan() = bluetoothController.startDiscovery()
    fun stopScan() = bluetoothController.stopDiscovery()

    private suspend fun sendSelectedProductCommand(): Boolean {
        val command = when(state.value.selectedProduct?.id) {
            "COCA" -> "COCA\n"
            "WATER" -> "WATER\n"
            else -> "ON\n"
        }
        return bluetoothController.trySendMessage(command)
    }

    private suspend fun completeSelectedOrder() {
        if (orderFinalized) return

        val product = state.value.selectedProduct
        if (product == null) {
            _paymentStatus.emit(true)
            return
        }

        orderFinalized = true
        val uid = session.currentUid
        if (uid != null) {
            val finalPrice = if (state.value.isFreeOrder) 0 else product.price
            SupabaseRepository.saveOrder(
                uid = uid,
                productId = product.id,
                productName = product.name,
                price = finalPrice
            ).onFailure {
                Log.e("Supabase", "Khong luu duoc don hang: ${it.message}")
            }
        }

        _state.update { it.copy(isCompletingOrder = false) }
        _paymentStatus.emit(true)
    }

    private suspend fun getPaidOrdersSinceLastFree(): Int {
        val uid = session.currentUid ?: return 0
        val orders = SupabaseRepository.getOrders(uid).getOrNull().orEmpty()
        return countPaidOrdersSinceLastFree(orders)
    }

    private fun countPaidOrdersSinceLastFree(orders: List<Order>): Int {
        var count = 0
        orders.sortedByDescending { it.created_at ?: "" }.forEach { order ->
            if (order.price <= 0) return count
            count++
        }
        return count
    }
}
