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

    private fun getStartOfDayUtcIso(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
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

    fun getMachinesFlow(): Flow<List<Machine>> = flow {
        while (true) {
            runCatching {
                supabase.postgrest["machines"].select(Columns.ALL).decodeList<Machine>()
            }.onSuccess { emit(it) }
            delay(5000)
        }
    }

    // ── INVENTORY ─────────────────────────────────────────────────────────────
    suspend fun updateInventory(machineId: String, inventory: MachineInventory): Result<MachineInventory> = runCatching {
        patchInventoryQuantity(productId = inventory.productId, quantity = inventory.quantity)
        getInventoryByProductId(inventory.productId) ?: throw IllegalStateException("Inventory not found")
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
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            doOutput = true
            setRequestProperty("apikey", "sb_publishable_wnKSk4-3-6tEaFhTj3_pJQ_Bcm6oELo")
            setRequestProperty("Authorization", "Bearer sb_publishable_wnKSk4-3-6tEaFhTj3_pJQ_Bcm6oELo")
            setRequestProperty("Content-Type", "application/json")
        }
        connection.outputStream.write("""{"quantity":$quantity}""".toByteArray())
        val code = connection.responseCode
        connection.disconnect()
        if (code !in 200..299) throw IllegalStateException("HTTP $code")
    }

    fun getInventoryFlow(): Flow<List<MachineInventory>> = flow {
        while (true) {
            runCatching {
                supabase.postgrest["machine_inventory"].select().decodeList<MachineInventory>()
            }.onSuccess { emit(it.sortedBy { i -> i.productId }) }
            delay(10000)
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
                        filter { eq("is_read", false) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(50)
                    }
                    .decodeList<AdminNotification>()
            }.onSuccess { emit(it) }
            delay(5000)
        }
    }

    suspend fun markNotificationRead(id: String) {
        runCatching {
            supabase.postgrest["notifications"].update(mapOf("is_read" to true)) { filter { eq("id", id) } }
        }
    }

    suspend fun markAllMachineNotificationsRead(machineId: String) {
        runCatching {
            supabase.postgrest["notifications"].update(mapOf("is_read" to true)) {
                filter { 
                    eq("machine_id", machineId)
                    eq("is_read", false)
                }
            }
        }
    }

    suspend fun saveAdminDeviceToken(token: String, deviceName: String): Result<Unit> = runCatching {
        supabase.postgrest["admin_devices"].upsert(AdminDevice(token = token, deviceName = deviceName))
    }

    // ── STATS ─────────────────────────────────────────────────────────────────
    fun getTodayOrdersFlow(): Flow<Int> = flow {
        while (true) {
            runCatching {
                val today = getStartOfDayUtcIso()
                supabase.postgrest["orders"].select { filter { gte("created_at", today) } }.decodeList<Order>().size
            }.onSuccess { emit(it) }
            delay(10000)
        }
    }

    fun getTodayRevenueFlow(): Flow<Long> = flow {
        while (true) {
            runCatching {
                val today = getStartOfDayUtcIso()
                supabase.postgrest["orders"].select { filter { gte("created_at", today) } }.decodeList<Order>().sumOf { it.price.toLong() }
            }.onSuccess { emit(it) }
            delay(10000)
        }
    }

    fun getTotalRevenueFlow(): Flow<Long> = flow {
        while (true) {
            runCatching { supabase.postgrest["orders"].select().decodeList<Order>().sumOf { it.price.toLong() } }.onSuccess { emit(it) }
            delay(15000)
        }
    }

    fun getTotalOrdersCountFlow(): Flow<Int> = flow {
        while (true) {
            runCatching { supabase.postgrest["orders"].select().decodeList<Order>().size }.onSuccess { emit(it) }
            delay(15000)
        }
    }

    fun getMonthlyRevenueFlow(): Flow<Long> = flow {
        while (true) {
            runCatching {
                val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'00:00:00.000'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                supabase.postgrest["orders"].select { filter { gte("created_at", sdf.format(cal.time)) } }.decodeList<Order>().sumOf { it.price.toLong() }
            }.onSuccess { emit(it) }
            delay(15000)
        }
    }

    fun getRecentOrdersFlow(): Flow<List<Order>> = flow {
        while (true) {
            runCatching {
                supabase.postgrest["orders"].select { order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING); limit(20) }.decodeList<Order>()
            }.onSuccess { emit(it) }
            delay(10000)
        }
    }
}
