package com.example.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

@Composable
fun RevenueScreen(viewModel: AdminViewModel) {
    val totalRevenue by viewModel.totalRevenue.collectAsState()
    val totalOrdersCount by viewModel.totalOrdersCount.collectAsState()
    val monthlyRevenue by viewModel.monthlyRevenue.collectAsState()
    val recentOrders by viewModel.recentOrders.collectAsState()

    val formatter = DecimalFormat("#,###")

    // Tự động tính toán giá trị trung bình/đơn hàng
    val avgPrice = remember(totalRevenue, totalOrdersCount) {
        if (totalOrdersCount == 0) 0L else totalRevenue / totalOrdersCount
    }

    // Tự động tìm sản phẩm bán chạy nhất dựa trên nhật ký gần đây
    val topProduct = remember(recentOrders) {
        if (recentOrders.isEmpty()) "Chưa có dữ liệu"
        else recentOrders.groupBy { it.productName }.maxByOrNull { it.value.size }?.key ?: "Chưa có dữ liệu"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F8)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thẻ Tổng doanh thu tích lũy (To đùng quyền lực nhất)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF1E2D60), Color(0xFF2C3E7A))))
                        .padding(24.dp)
                ) {
                    Column {
                        Text("TỔNG DOANH THU TÍCH LŨY", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("${formatter.format(totalRevenue)}đ", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF00AEEF), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }
            }
        }

        // Khối 2 thẻ nhỏ song song: Doanh thu tháng này & Tổng đơn hàng
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // Thẻ tháng
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.MonetizationOn, null, tint = Color(0xFF27AE60), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tháng này", fontSize = 12.sp, color = Color.Gray)
                        Text("${formatter.format(monthlyRevenue)}đ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E7A))
                    }
                }
                // Thẻ tổng đơn
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Default.Assignment, null, tint = Color(0xFFF39C12), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tổng đơn hàng", fontSize = 12.sp, color = Color.Gray)
                        Text("$totalOrdersCount đơn", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E7A))
                    }
                }
            }
        }

        // Phần thông tin chi tiết mở rộng bổ sung
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Thông tin phân tích bổ sung", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E2D60))
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Trung bình/đơn hàng:", color = Color.Gray, fontSize = 13.sp)
                        Text("${formatter.format(avgPrice)}đ", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sản phẩm chuộng gần đây:", color = Color.Gray, fontSize = 13.sp)
                        Text(topProduct, fontWeight = FontWeight.Bold, color = Color(0xFF00AEEF), fontSize = 13.sp)
                    }
                }
            }
        }

        // Tiêu đề Lịch sử
        item {
            Text("Nhật ký giao dịch gần đây", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E7A))
        }

        // Danh sách đơn hàng thời gian thực
        items(recentOrders) { order ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF00AEEF).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛒", fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(order.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = order.createdAt?.substringBefore(".")?.replace("T", " ") ?: "Vừa xong",
                            fontSize = 11.sp, color = Color.Gray
                        )
                    }
                    Text(
                        "+${formatter.format(order.price)}đ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF27AE60),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}