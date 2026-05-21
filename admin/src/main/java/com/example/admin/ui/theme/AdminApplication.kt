package com.example.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import android.content.Context
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AdminApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createLowStockNotificationChannel()
    }

    private fun createLowStockNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            LOW_STOCK_CHANNEL_ID,
            "Cảnh báo kho hàng",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thông báo khi sản phẩm sắp hết hoặc đã hết hàng"
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        const val LOW_STOCK_CHANNEL_ID = "fcm_low_stock_alerts"
    }
}
