package com.example.bluetooth.presentation.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.DecimalFormat

@Composable
fun HistoryScreen(
    viewModel: UserViewModel=hiltViewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    // Guest → hiện màn hình mời đăng nhập
    if (viewModel.isGuest) {
        GuestPlaceholder("Đăng nhập để xem lịch sử mua hàng", onNavigateToLogin)
        return
    }

    val state by viewModel.state.collectAsState()
    val fmt = DecimalFormat("#,###đ")

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lịch sử mua hàng", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, null, tint = Color(0xFF00AEEF))
            }
        }

        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00AEEF))
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error ?: "", color = Color.Gray)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.refresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF))
                        ) { Text("Thử lại") }
                    }
                }
            }
            state.orders.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.History, null, Modifier.size(72.dp), tint = Color(0xFFDFE6E9))
                        Spacer(Modifier.height(12.dp))
                        Text("Chưa có lịch sử mua hàng", color = Color.Gray)
                    }
                }
            }
            else -> {
                // Thẻ tổng chi tiêu
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF00AEEF))
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tổng chi tiêu", color = Color.White.copy(0.8f), fontSize = 13.sp)
                            Text(
                                fmt.format(state.orders.sumOf { it.price }),
                                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            "${state.orders.size} đơn",
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.orders) { order ->
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        order.product_name,
                                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                                    )
                                    Text(
                                        order.created_at?.take(10)?.replace("-", "/") ?: "",
                                        fontSize = 12.sp, color = Color.Gray
                                    )
                                }
                                Text(
                                    fmt.format(order.price),
                                    color = Color(0xFF00AEEF),
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}