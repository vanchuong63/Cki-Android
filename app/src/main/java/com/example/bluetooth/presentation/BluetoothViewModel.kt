package com.example.bluetooth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.data.remote.SePayApi
import com.example.bluetooth.domain.controller.BluetoothController
import com.example.bluetooth.domain.model.BluetoothDeviceDomain
import com.example.bluetooth.domain.model.ConnectionResult
import com.example.bluetooth.presentation.user.SampleBeverages
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
import java.util.Locale
import java.text.SimpleDateFormat

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevice,
        bluetoothController.pairedDevice,
        _state
    ) { scannedDevices, pairedDevices, state ->
        state.copy(scannedDevices = scannedDevices, pairedDevices = pairedDevices)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    private val _paymentStatus = MutableSharedFlow<Boolean>()
    val paymentStatus = _paymentStatus.asSharedFlow()

    private val TARGET_DEVICE_NAME = "ESP32-Bluetooth"
    private var lastConnectedDevice: BluetoothDeviceDomain? = null
    private val processedTransactionIds = mutableSetOf<String>()
    private var paymentCheckJob: Job? = null
    private var baselineTime: Long = 0L

    private val SEPAY_TOKEN = "6UEOJPRT4BXY35YID8ICK2WPVPTR9NOZBQSUXD1PBLQN0WJVKMODLKGYWCEBHE5K"
    private val SEPAY_ACCOUNT = "VQRQAIDDN1936"

    private val sePayApi: SePayApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl("https://my.sepay.vn/userapi/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(SePayApi::class.java)
    }

    init {
        Log.d("VendingMachine", ">>> KHỞI ĐỘNG HỆ THỐNG <<<")
        viewModelScope.launch { loadInitialBaseline() }

        // Auto scan & kết nối ESP32
        viewModelScope.launch {
            while (true) {
                if (!state.value.isConnected && !state.value.isConnecting) {
                    startScan()
                    delay(4000)
                    stopScan()
                }
                delay(8000)
            }
        }

        bluetoothController.scannedDevice
            .onEach { devices ->
                val target = devices.find {
                    it.name?.trim()?.equals(TARGET_DEVICE_NAME, true) == true
                }
                if (target != null && !state.value.isConnected && !state.value.isConnecting)
                    connectToDevice(target)
            }.launchIn(viewModelScope)

        bluetoothController.incomingMessages
            .onEach { msg ->
                Log.d("BluetoothLog", "ESP32 gửi: $msg")
                if (msg.contains("COMPLETED", ignoreCase = true)) {
                    Log.d("VendingMachine", "XÁC NHẬN: Hoàn tất nhả nước.")
                    onPaymentDetected()
                }
            }.launchIn(viewModelScope)
    }

    private suspend fun loadInitialBaseline() {
        try {
            val res = sePayApi.getTransactions("Bearer $SEPAY_TOKEN", SEPAY_ACCOUNT, limit = 10)
            res.transactions.forEach { it.id?.let { id -> processedTransactionIds.add(id) } }
            baselineTime = System.currentTimeMillis()
            Log.d("SePayLog", "Đã lưu mốc ${processedTransactionIds.size} GD cũ.")
        } catch (e: Exception) {
            Log.e("SePayLog", "Lỗi tải mốc: ${e.message}")
        }
    }

    private fun startCheckingPayment(product: SelectedProduct) {
        paymentCheckJob?.cancel()
        paymentCheckJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val cleanId = product.id.uppercase().trim()

            Log.d("SePayLog", "Đợi tiền: $cleanId | ${product.price}đ")

            while (System.currentTimeMillis() - startTime < 180_000) {

                try {
                    val res = sePayApi.getTransactions(
                        "Bearer $SEPAY_TOKEN", SEPAY_ACCOUNT, limit = 10
                    )
                    res.transactions.forEach { tx ->
                        Log.d("TX_DEBUG", "id=${tx.id} | amount=${tx.amountIn} | content=${tx.content} | date=${tx.date} | processed=${tx.id in processedTransactionIds}")
                    }

                    val newTx = res.transactions.firstOrNull { tx ->
                        tx.id != null
                                && tx.id !in processedTransactionIds
                                && tx.content.contains(cleanId, ignoreCase = true)
                                && runCatching { tx.amountIn.toDouble().toInt() >= product.price }.getOrDefault(false)
                                && isTransactionAfterBaseline(tx.date)
                    }


                    if (newTx != null) {
                        Log.d("SePayLog", "TIỀN VỀ! ID: ${newTx.id} | ${newTx.amountIn}đ")
                        processedTransactionIds.add(newTx.id!!)

                        // --- LUỒNG 1: XỬ LÝ DATABASE (Bất chấp Bluetooth có chạy hay không) ---
                        viewModelScope.launch {
                            // 1. Lưu lịch sử đơn hàng & Tích điểm
                            if (sessionManager.isLoggedIn) {
                                SupabaseRepository.saveOrder(
                                    uid = sessionManager.currentUid!!,
                                    productId = product.id,
                                    productName = product.name,
                                    price = product.price
                                )
                            }

                            // 2. TRỪ KHO NGAY LẬP TỨC (Nằm chung luồng với việc lưu đơn)
                            Log.d("KhoHang", "Tiền đã vào túi -> Ép trừ kho cho mã: ${product.id}")
                            SupabaseRepository.decreaseStock(product.id.uppercase().trim())
                        }

                        // Gửi lệnh Bluetooth
                        var sent = false
                        for (attempt in 1..3) {
                            if (!state.value.isConnected) {
                                Log.w("BluetoothLog", "Chờ kết nối BT lần $attempt...")
                                lastConnectedDevice?.let { connectToDevice(it) }
                                delay(5000)
                            }
                            sent = bluetoothController.trySendMessage("$cleanId\n")
                            if (sent) {
                                Log.d("BluetoothLog", "Gửi ESP32 thành công lần $attempt!")
                                // TRỪ KHO HÀNG KHI MUA CÓ PHÍ
                                SupabaseRepository.decreaseStock(product.id)
                                break
                            }
                            Log.w("BluetoothLog", "Thất bại lần $attempt, thử lại...")
                            delay(1500)
                        }

                        if (!sent) Log.e("BluetoothLog", "Không gửi được sau 3 lần!")

                        _paymentStatus.emit(true)
                        break
                    }

                } catch (e: Exception) {
                    Log.e("SePayLog", "Lỗi API: ${e.message}")
                }

                delay(6000)
            }

            if (state.value.selectedProduct != null) {
                Log.w("SePayLog", "Hết 3 phút chờ thanh toán.")
            }
        }
    }

    fun selectProduct(productId: String) {
        val beverage = SampleBeverages.find { it.id == productId }
        if (beverage == null) {
            Log.e("VendingMachine", "Không tìm thấy sản phẩm: $productId")
            return
        }
        viewModelScope.launch {
            var finalPrice = beverage.price

            // Lấy discount nếu đã đăng nhập
            if (sessionManager.isLoggedIn) {
                val profile = SupabaseRepository
                    .getUserProfile(sessionManager.currentUid!!)
                    .getOrNull()

                when (profile?.discount_type) {
                    "FIX_5K" -> {
                        finalPrice = (beverage.price - 5000).coerceAtLeast(0)
                        Log.d("VendingMachine", "Giảm 5k: ${beverage.price} → $finalPrice")
                    }
                    "PERCENT_20" -> if (beverage.price >= 20000) {
                        finalPrice = (beverage.price * 0.8).toInt()
                        Log.d("VendingMachine", "Giảm 20%: ${beverage.price} → $finalPrice")
                    }
                    "FREE_WATER" -> if (productId == "WATER" || productId == "AQUAFINA") {
                        finalPrice = 0
                        Log.d("VendingMachine", "Nước suối miễn phí!")
                    }
                    "FREE_COCA" -> if (productId == "COCA") {
                        finalPrice = 0
                        Log.d("VendingMachine", "Coca miễn phí!")
                    }
                    "FREE_2_TEA" -> if (productId == "TEA" || productId == "C2") {
                        finalPrice = 0
                        Log.d("VendingMachine", "Trà miễn phí!")
                    }
                }
            }


            val product = SelectedProduct(
            id = beverage.id,
            name = beverage.name,
            price = finalPrice
        )

        Log.d("VendingMachine", "Chọn nước: ${product.name} - ${product.price}đ")
        _state.update { it.copy(selectedProduct = product) }
            if (finalPrice == 0) {
                // Miễn phí → gửi lệnh Bluetooth luôn, không cần chờ tiền
                Log.d("VendingMachine", "Sản phẩm miễn phí → gửi lệnh ngay")
                var sent = false
                val cleanId = product.id.uppercase().trim()

                // Thêm logic kiểm tra kết nối lại giống hệt phần thanh toán
                for (attempt in 1..3) {
                    if (!state.value.isConnected) {
                        Log.w("BluetoothLog", "Chờ kết nối BT lần $attempt (Miễn phí)...")
                        lastConnectedDevice?.let { connectToDevice(it) }
                        delay(5000)
                    }
                    sent = bluetoothController.trySendMessage("$cleanId\n")
                    if (sent) {
                        Log.d("BluetoothLog", "Gửi ESP32 thành công (miễn phí) lần $attempt!")
                        // TRỪ KHO HÀNG KHI ĐƯỢC MIỄN PHÍ (ĐỔI QUÀ)
                        SupabaseRepository.decreaseStock(product.id)
                        break
                    }
                    Log.w("BluetoothLog", "Thất bại lần $attempt, thử lại...")
                    delay(1500)
                }

                if (!sent) Log.e("BluetoothLog", "Không gửi được đồ miễn phí sau 3 lần!")

                // Lưu đơn với giá 0
                if (sessionManager.isLoggedIn) {
                    SupabaseRepository.saveOrder(
                        uid = sessionManager.currentUid!!,
                        productId = product.id,
                        productName = product.name,
                        price = 0
                    )
                }
                _paymentStatus.emit(true)
            } else {
                // Có tiền → chờ thanh toán bình thường
                startCheckingPayment(product)
            }
        }
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        lastConnectedDevice = device
        _state.update { it.copy(isConnecting = true) }
        bluetoothController.connectToDevice(device)
            .onEach { result ->
                when (result) {
                    is ConnectionResult.ConnectionEstablished ->
                        _state.update { it.copy(isConnected = true, isConnecting = false) }
                    is ConnectionResult.Error ->
                        _state.update { it.copy(isConnected = false, isConnecting = false) }
                }
            }.launchIn(viewModelScope)
    }

    fun onPaymentDetected() {
        _state.update { it.copy(selectedProduct = null) }
        paymentCheckJob?.cancel()
    }

    fun startScan() = bluetoothController.startDiscovery()
    fun stopScan() = bluetoothController.stopDiscovery()

    private fun isTransactionAfterBaseline(dateStr:String): Boolean {
        return try{
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val txTime = sdf.parse(dateStr)?.time ?: 0L
            val result = txTime > baselineTime
            Log.d("SepayLog", "TX=$txTime baseline=$baselineTime after=$result")
            result

        } catch (e: Exception) {
            Log.e("SepayLog", "Lỗi parse thời gian: ${e.message}")
            true // parse lỗi -> cho qua để không bỏ sót GD
        }
    }
}
