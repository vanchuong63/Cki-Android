package com.example.admin.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Machine(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    @SerialName("is_online") val isOnline: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MachineInventory(
    @SerialName("machine_id") val machineId: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String = "",
    val quantity: Int = 2,
    @SerialName("min_quantity") val minQuantity: Int = 0
)

@Serializable
data class AdminNotification(
    val id: String? = null,
    @SerialName("machine_id") val machineId: String = "",
    @SerialName("machine_name") val machineName: String = "",
    val message: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
@Serializable
data class Order(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String = "",
    val price: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)