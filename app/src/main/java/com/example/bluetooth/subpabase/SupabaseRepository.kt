package com.example.bluetooth.subpabase

import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable


@Serializable
data class UserProfile(
    val id: String,           // Firebase UID
    val phone: String,
    val email: String,
    val points: Int = 0,
    val discount_type: String? = null, // "FIX_5K" hoặc "PERCENT_20"
    val created_at: String? = null
)

@Serializable
data class Order(
    val id: String? = null,   // uid, Supabase tự tạo
    val user_id: String,      // Firebase UID
    val product_id: String,
    val product_name: String,
    val price: Int,
    val created_at: String? = null
)

@Serializable
data class ProfileUpdate(
    val points: Int,
    val discount_type: String?
)
// ── Repository ───────────────────────────────────────────────

object SupabaseRepository {

    // Tạo profile user sau khi đăng ký Firebase thành công
    suspend fun createUserProfile(uid: String, phone: String, email: String): Result<Unit> {
        return try {
            supabase.postgrest["users"].insert(
                UserProfile(id = uid, phone = phone, email = email)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Supabase", "Lỗi tạo profile: ${e.message}")
            Result.failure(e)
        }
    }

    // Lấy thông tin user
    suspend fun getUserProfile(uid: String): Result<UserProfile> {
        return try {
            val result = supabase.postgrest["users"]
                .select(Columns.ALL) { filter { eq("id", uid) } }
                .decodeSingle<UserProfile>()
            Result.success(result)
        } catch (e: Exception) {
            Log.e("Supabase", "Lỗi lấy profile cho $uid: ${e.message}")
            Result.failure(e)
        }
    }

    // Lưu đơn hàng và cập nhật thống kê vào Profile
    suspend fun saveOrder(
        uid: String,
        productId: String,
        productName: String,
        price: Int
    ): Result<Unit> {
        return try {
            Log.d("Supabase", "Đang lưu đơn hàng: $productName ($price VNĐ) cho $uid")
            
            // 1. Lưu đơn hàng vào bảng orders
            supabase.postgrest["orders"].insert(
                Order(
                    user_id = uid,
                    product_id = productId,
                    product_name = productName,
                    price = price
                )
            )
            Log.d("Supabase", "Đã lưu lịch sử đơn hàng thành công")

            // 2. Cập nhật thống kê vào bảng users (Profile)
            // Lấy profile mới nhất để tích điểm
            val profileResponse = getUserProfile(uid)
            val profile = profileResponse.getOrNull()
            
            if (profile != null) {
                val pointsToAdd = price / 1000
                val newPoints = profile.points + pointsToAdd
                Log.d("Supabase", "Tiến hành tích điểm: ${profile.points} -> $newPoints (+$pointsToAdd)")

                supabase.postgrest["users"].update(
                    ProfileUpdate(
                        points = newPoints,
                        discount_type = null // Tiêu thụ luôn ưu đãi nếu có
                    )
                ) { filter { eq("id", uid) } }
                
                Log.d("Supabase", "Đã cập nhật điểm vào Profile thành công")
                Result.success(Unit)
            } else {
                val errMsg = profileResponse.exceptionOrNull()?.message ?: "Không tìm thấy profile người dùng"
                Log.e("Supabase", "Lỗi khi lấy profile để tích điểm: $errMsg")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e("Supabase", "Lỗi hệ thống trong saveOrder: ${e.message}")
            Result.failure(e)
        }
    }

    // Lấy lịch sử mua hàng
    suspend fun getOrders(uid: String): Result<List<Order>> {
        return try {
            val result = supabase.postgrest["orders"]
                .select(Columns.ALL) {
                    filter { eq("user_id", uid) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Order>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Đổi quà: Trừ điểm và gán loại giảm giá
    suspend fun redeemReward(uid: String, cost: Int, discountType: String): Result<Unit> {
        return try {
            val profile = getUserProfile(uid).getOrNull() ?: throw Exception("User not found")
            if (profile.points < cost) throw Exception("Không đủ điểm")
            
            val newPoints = profile.points - cost
            Log.d("Supabase", "Đổi quà: trừ $cost điểm, còn lại $newPoints")

            supabase.postgrest["users"].update(
                ProfileUpdate(
                    points = newPoints,
                    discount_type = discountType
                )
            ) { filter { eq("id", uid) } }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("Supabase", "Lỗi đổi quà: ${e.message}")
            Result.failure(e)
        }
    }
}
