package com.example.admin.presentation

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.admin.R
import com.example.admin.AdminApplication
import com.example.admin.data.AdminNotification
import com.example.admin.data.SupabaseAdminRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminMainActivity : ComponentActivity() {
    private val bluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdminApp(isBluetoothEnabled, bluetoothAdapter) } 
    }
}

@Composable
fun AdminApp(isBluetoothEnabled: Boolean, bluetoothAdapter: BluetoothAdapter?) {
    val context = LocalContext.current
    val viewModel: AdminViewModel = hiltViewModel()
    val unread by viewModel.unreadCount.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    var knownNotificationIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasLoadedNotifications by remember { mutableStateOf(false) }

    // 1. Launcher để yêu cầu bật Bluetooth
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(context, "Vui lòng bật Bluetooth để quản lý máy", Toast.LENGTH_LONG).show()
        }
    }

    // 2. Launcher để yêu cầu các quyền Bluetooth & Vị trí
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val canConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true

        if (canConnect && bluetoothAdapter?.isEnabled == false) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    // 3. Tự động xin quyền khi mở App
    LaunchedEffect(Unit) {
        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.toTypedArray()
        permissionLauncher.launch(permissions)

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    SupabaseAdminRepository.saveAdminDeviceToken(
                        token = token,
                        deviceName = Build.MODEL ?: "Android Admin"
                    )
                }
            }
    }

    LaunchedEffect(notifications) {
        val currentIds = notifications.map { notificationKey(it) }.toSet()
        if (!hasLoadedNotifications) {
            knownNotificationIds = currentIds
            hasLoadedNotifications = true
        } else {
            notifications
                .filter { !it.isRead && notificationKey(it) !in knownNotificationIds }
                .forEach { showAdminSystemNotification(context, it) }
            knownNotificationIds = currentIds
        }
    }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF2C3E7A)) {
                val tabs = listOf("Dashboard", "Máy & Map", "Kho hàng","Doanh Thu", "Thông báo")
                val icons = listOf(
                    Icons.Default.Dashboard,
                    Icons.Default.Map,
                    Icons.Default.Inventory,
                    Icons.Default.MonetizationOn,
                    Icons.Default.Notifications
                )
                tabs.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        icon = {
                            BadgedBox(badge = {
                                // Tab Thông báo ở vị trí index 4
                                if (i == 4 && unread > 0) Badge { Text("$unread") }
                            }) {
                                Icon(icons[i], contentDescription = label)
                            }
                        },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00AEEF),
                            selectedTextColor = Color(0xFF00AEEF),
                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                            indicatorColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel)
                1 -> MachineMapScreen(viewModel)
                2 -> InventoryScreen(viewModel)
                3 -> RevenueScreen(viewModel)
                4 -> NotificationScreen(viewModel)
            }
        }
    }
}

private const val LOW_STOCK_CHANNEL_ID = AdminApplication.LOW_STOCK_CHANNEL_ID

private fun notificationKey(notification: AdminNotification): String =
    notification.id ?: "${notification.machineId}|${notification.message}|${notification.createdAt}"

private fun showAdminSystemNotification(context: Context, notification: AdminNotification) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            LOW_STOCK_CHANNEL_ID,
            "Canh bao kho hang",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Thong bao khi san pham sap het hoac da het hang"
        }
        manager.createNotificationChannel(channel)
    }

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(context, LOW_STOCK_CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(context)
    }

    val builtNotification = builder
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Canh bao het hang")
        .setContentText(notification.message)
        .setStyle(Notification.BigTextStyle().bigText(notification.message))
        .setAutoCancel(true)
        .build()

    manager.notify(notificationKey(notification).hashCode(), builtNotification)
}
