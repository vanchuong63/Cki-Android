package com.example.admin.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdminApp() }
    }
}

@Composable
fun AdminApp() {
    var selectedTab by remember { mutableStateOf(0) }
    val viewModel: AdminViewModel = hiltViewModel()
    val unread by viewModel.unreadCount.collectAsState()

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
                                if (i == 3 && unread > 0) Badge { Text("$unread") }
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