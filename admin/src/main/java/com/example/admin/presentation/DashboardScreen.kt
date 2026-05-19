package com.example.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class StatCard(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun DashboardScreen(viewModel: AdminViewModel) {
    val todayOrders by viewModel.todayOrders.collectAsState()
    val todayRevenue by viewModel.todayRevenue.collectAsState()
    val machines by viewModel.machines.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    val formatter = DecimalFormat("#,###")
    val onlineMachines = machines.count { it.isOnline }
    val lowStockAlerts = notifications.count { !it.isRead }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F8)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF2C3E7A), Color(0xFF00AEEF)))
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text("Xin chào, Admin 👋", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text("Dashboard", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi")).format(Date()),
                        color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
                    )
                }
            }
        }

        // 4 Stat Cards
        item {
            val stats = listOf(
                StatCard("Doanh thu hôm nay", "${formatter.format(todayRevenue)}đ", Icons.Default.AttachMoney, Color(0xFF00AEEF)),
                StatCard("Đơn hàng hôm nay", "$todayOrders đơn", Icons.Default.ShoppingCart, Color(0xFFF39C12)),
                StatCard("Máy đang online", "$onlineMachines/${machines.size}", Icons.Default.Router, Color(0xFF27AE60)),
                StatCard("Cảnh báo tồn kho", "$lowStockAlerts cảnh báo", Icons.Default.Warning, Color(0xFFE74C3C)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { card ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(3.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(card.color.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(card.icon, null, tint = card.color, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(card.value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF2C3E7A))
                                    Text(card.title, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Tiêu đề danh sách máy
        item {
            Text("Trạng thái máy", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E7A))
        }

        // Danh sách máy
        items(machines) { machine ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (machine.isOnline) Color(0xFF27AE60).copy(alpha = 0.15f)
                                else Color.Gray.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalDrink, null,
                            tint = if (machine.isOnline) Color(0xFF27AE60) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(machine.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(machine.location, fontSize = 12.sp, color = Color.Gray)
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (machine.isOnline) Color(0xFF27AE60).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                    ) {
                        Text(
                            if (machine.isOnline) "Online" else "Offline",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (machine.isOnline) Color(0xFF27AE60) else Color.Gray
                        )
                    }
                }
            }
        }

        if (machines.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocalDrink, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(8.dp))
                        Text("Chưa có máy nào", color = Color.Gray)
                    }
                }
            }
        }
    }
}