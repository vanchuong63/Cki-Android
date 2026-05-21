package com.example.admin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.admin.R
import com.example.admin.data.SupabaseAdminRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AdminFcmService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        scope.launch {
            SupabaseAdminRepository.saveAdminDeviceToken(
                token = token,
                deviceName = Build.MODEL ?: "Android Admin"
            ).onFailure {
                Log.e("FCM", "Không lưu được FCM token: ${it.message}", it)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Cảnh báo hết hàng"
        val body = message.notification?.body ?: message.data["body"] ?: message.data["message"] ?: ""
        Log.d("FCM", "Nhận thông báo: $title - $body")
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "fcm_low_stock_alerts"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Cảnh báo hết hàng",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        manager.notify((title + body).hashCode(), notification)
    }
}
