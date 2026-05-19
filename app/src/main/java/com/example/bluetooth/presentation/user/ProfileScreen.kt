package com.example.bluetooth.presentation.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DecimalFormat

@Composable
fun ProfileScreen(
    viewModel: UserViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    if (viewModel.isGuest) {
        GuestPlaceholder("Đăng nhập để xem thông tin cá nhân", onNavigateToLogin)
        return
    }

    val state by viewModel.state.collectAsState()
    val fmt = DecimalFormat("#,###đ")
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Tự động làm mới dữ liệu mỗi khi vào màn hình này
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng xuất") },
            text = { Text("Bạn có chắc muốn đăng xuất không?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.signOut { onNavigateToLogin() }
                }) { Text("Đăng xuất", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Hủy") }
            }
        )
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Thông tin cá nhân", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp)
        )

        if (state.isLoading && state.profile == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00AEEF)) }
            return@Column
        }

        // Avatar chữ cái đầu
        Box(
            Modifier.size(96.dp).background(Color(0xFF00AEEF).copy(0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                state.profile?.email?.take(1)?.uppercase() ?: "?",
                fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00AEEF)
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(state.profile?.email ?: "—", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(state.profile?.phone ?: "—", fontSize = 14.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        // Thống kê nhanh - Lấy dữ liệu thực tế từ danh sách đơn hàng
        val totalOrders = state.orders.size
        val totalSpending = state.orders.sumOf { it.price }
        val currentPoints = state.profile?.points ?: 0

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), "Tổng đơn", "$totalOrders đơn")
            StatCard(Modifier.weight(1f), "Chi tiêu", fmt.format(totalSpending))
            StatCard(Modifier.weight(1f), "Điểm", "$currentPoints")
        }

        Spacer(Modifier.height(20.dp))

        // Thông tin chi tiết
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(1.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(8.dp)) {
                InfoRow("Email", state.profile?.email ?: "—")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(0.3f))
                InfoRow("Số điện thoại", state.profile?.phone ?: "—")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(0.3f))
                InfoRow("Ngày tham gia", state.profile?.created_at?.take(10)?.replace("-", "/") ?: "—")
            }
        }

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(0.5f))
        ) { Text("Đăng xuất", fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// Hàm Component định nghĩa cấu trúc cho các thẻ thống kê nhanh
@Composable
private fun StatCard(modifier: Modifier, label: String, value: String) {
    Card(
        modifier, shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00AEEF).copy(0.08f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF00AEEF), fontSize = 15.sp)
            Text(label, color = Color.Gray, fontSize = 11.sp)
        }
    }
}
