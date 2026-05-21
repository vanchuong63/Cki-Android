package com.example.admin.data

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object SupabaseAdminRepository {

    // Lấy mốc thời gian 00:00:00 ngày hôm nay theo định dạng ISO để lọc đơn hàng
    private fun getStartOfDayUtcIso(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Chuyển mốc 0h sáng VN thành định dạng ISO UTC để truy vấn Supabase
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(calendar.time)
    }

    // ── MACHINES ──────────────────────────────────────────────────────────────
    suspend fun addMachine(machine: Machine): Result<Unit> = runCatching {
        supabase.postgrest["machines"].insert(machine)
    }

    suspend fun deleteMachine(machineId: String): Result<Unit> = runCatching {
        supabase.postgrest["machines"].delete { filter { eq("id", machineId) } }
    }

    suspend fun updateMachineStatus(machineId: String, isOnline: Boolean): Result<Unit> = runCatching {
        supabase.postgrest["machines"].update(mapOf("is_online" to isOnline)) {
            filter { eq("id", machineId) }
        }
    }

    fun getMachinesFlow(): Flow<List<Machine>> = flow {
        while (true) {
            runCatching {
                supabase.postgrest["machines"].select(Columns.ALL).decodeList<Machine>()
            }.onSuccess { emit(it) }
            delay(5000) // Tải lại danh sách máy mỗi 5 giây
        }
    }

    // ── INVENTORY ─────────────────────────────────────────────────────────────
    suspend fun updateInventory(machineId: String, inventory: MachineInventory): Result<MachineInventory> = runCatching {
        patchInventoryQuantity(productId = inventory.productId, quantity = inventory.quantity)

        val updatedInventory = getInventoryByProductId(inventory.productId)
            ?: throw IllegalStateException("Khong tim thay san pham ${inventory.productId} sau khi cap nhat")

        if (updatedInventory.quantity != inventory.quantity) {
            throw IllegalStateException(
                "Database chua cap nhat ${inventory.productId}: hien tai ${updatedInventory.quantity}, mong muon ${inventory.quantity}"
            )
        }

        updatedInventory
    }

    private suspend fun getInventoryByProductId(productId: String): MachineInventory? {
        return supabase.postgrest["machine_inventory"]
            .select(Columns.ALL) { filter { eq("product_id", productId) } }
            .decodeList<MachineInventory>()
            .firstOrNull()
    }

    private suspend fun patchInventoryQuantity(productId: String, quantity: Int) = withContext(Dispatchers.IO) {
        val safeProductId = productId.trim().uppercase(Locale.US)
        val url = URL("https://onjsgzthfxlvcuolfczb.supabase.co/rest/v1/machine_inventory?product_id=eq.$safeProductId")
        val body = """{"quantity":$quantity}"""

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            connectTimeout = 15_000
            readTimeout = 15_000
            doOutput = true
            setRequestProperty("apikey", "sb_publishable_wnKSk4-3-6tEaFhTj3_pJQ_Bcm6oELo")
            setRequestProperty("Authorization", "Bearer sb_publishable_wnKSk4-3-6tEaFhTj3_pJQ_Bcm6oELo")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=representation")
        }

        connection.outputStream.use { output ->
            output.write(body.toByteArray(Charsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val responseBody = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }

        connection.disconnect()

        if (responseCode !in 200..299) {
            throw IllegalStateException("Supabase update failed HTTP $responseCode: $responseBody")
        }

        if (responseBody == "[]" || responseBody.isBlank()) {
            throw IllegalStateException("Khong co dong nao duoc cap nhat cho product_id=$safeProductId")
        }
    }

    // lấy dl kho hàng
    fun getInventoryFlow(): Flow<List<MachineInventory>> = flow {
        while (true) {
            runCatching {
                supabase.postgrest["machine_inventory"].select().decodeList<MachineInventory>()
            }.onSuccess { list ->
                emit(list.sortedBy { it.productId }) // Sắp xếp theo mã nước cho dễ quản lý
            }.onFailure { error ->
                Log.e("AdminRepo", "Lỗi tải dữ liệu kho: ${error.message}")
                emit(emptyList())
            }
            delay(10000) // 10 giây cập nhật tình hình kho 1 lần
        }
    }

    // ── NOTIFICATIONS ─────────────────────────────────────────────────────────
    suspend fun addNotification(notif: AdminNotification): Result<Unit> = runCatching {
        supabase.postgrest["notifications"].insert(notif)
    }

    fun getNotificationsFlow(): Flow<List<AdminNotification>> = flow {
        while (true) {
            runCatching {
                supabase.postgrest["notifications"]
                    .select(Columns.ALL) {
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<AdminNotification>()
            }.onSuccess { list ->
                emit(list)
            }.onFailure { error ->
                Log.e("AdminThongBao", "❌ LỖI TẢI THÔNG BÁO TỪ MẠNG: ${error.message}", error)
                emit(emptyList())
            }
            delay(5000)
        }
    }

    suspend fun markNotificationRead(id: String) {
        runCatching {
            supabase.postgrest["notifications"].update(mapOf("is_read" to true)) {
                filter { eq("id", id) }
            }
        }
    }

    // ── STATS ORDERS (Đọc trực tiếp từ bảng orders chung của App User) ─────────
    suspend fun saveAdminDeviceToken(token: String, deviceName: String): Result<Unit> = runCatching {
        supabase.postgrest["admin_devices"].upsert(
            AdminDevice(
                token = token,
                deviceName = deviceName
            )
        )
    }

    fun getTodayOrdersFlow(): Flow<Int> = flow {
        while (true) {
            runCatching {
                val todayUtc = getStartOfDayUtcIso()
                val res = supabase.postgrest["orders"]
                    .select { filter { gte("created_at", todayUtc) } }
                    .decodeList<Order>()
                res.size // trả về số lượng đơn hàng
            }.onSuccess { emit(it) }
            delay(10000) // Cập nhật thống kê doanh thu mỗi 10 giây
        }
    }

    fun getTodayRevenueFlow(): Flow<Long> = flow {
        while (true) {
            runCatching {
                val todayUtc = getStartOfDayUtcIso()
                val res = supabase.postgrest["orders"]
                    .select { filter { gte("created_at", todayUtc) } }
                    .decodeList<Order>()
                // tổng tất cả các đơn hàng
                res.sumOf { it.price.toLong() }
            }.onSuccess { emit(it) }
            delay(10000)
        }
    }
    // Lấy mốc 00:00:00 của ngày đầu tiên trong tháng này (Giờ Việt Nam -> UTC)
    private fun getStartOfMonthUtcIso(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(calendar.time)
    }

    // 1. Lấy TỔNG DOANH THU tích lũy từ trước đến nay
    fun getTotalRevenueFlow(): Flow<Long> = flow {
        while (true) {
            runCatching {
                val res = supabase.postgrest["orders"].select().decodeList<Order>()
                res.sumOf { it.price.toLong() }
            }.onSuccess { total ->
                android.util.Log.d("Doanh thu", "Tổng hiện tại: $total")

                emit(total)
            }.onFailure { error ->
                android.util.Log.e("Doanh thu", "Lỗi: ${error.message}")
            }
            delay(15000) // Tải lại sau mỗi 15 giây
        }
    }

    // 2. Lấy TỔNG SỐ ĐƠN HÀNG đã bán từ trước đến nay
    fun getTotalOrdersCountFlow(): Flow<Int> = flow {
        while (true) {
            runCatching {
                val res = supabase.postgrest["orders"].select().decodeList<Order>()
                res.size
            }.onSuccess { emit(it) }
            delay(15000)
        }
    }

    // 3. Lấy DOANH THU TRONG THÁNG NÀY
    fun getMonthlyRevenueFlow(): Flow<Long> = flow {
        while (true) {
            runCatching {
                val startOfMonth = getStartOfMonthUtcIso()
                val res = supabase.postgrest["orders"]
                    .select { filter { gte("created_at", startOfMonth) } }
                    .decodeList<Order>()
                res.sumOf { it.price.toLong() }
            }.onSuccess { emit(it) }
            delay(15000)
        }
    }

    // 4. Lấy danh sách 20 ĐƠN HÀNG MỚI NHẤT để làm nhật ký giao dịch
    fun getRecentOrdersFlow(): Flow<List<Order>> = flow {
        while (true) {
            runCatching {
                supabase.postgrest["orders"]
                    .select {
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(20)
                    }
                    .decodeList<Order>()
            }.onSuccess { emit(it) }
            delay(10000) // Cập nhật nhật ký mỗi 10 giây
        }
    }

}
