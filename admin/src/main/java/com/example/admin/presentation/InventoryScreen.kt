package com.example.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.admin.data.MachineInventory // Nhớ giữ nguyên các import cũ của bạn

@Composable
fun InventoryScreen(viewModel: AdminViewModel) {
    val inventoryList by viewModel.inventoryState.collectAsState()

    val totalTypes = inventoryList.size
    val outOfStockCount = inventoryList.count { it.quantity <= it.minQuantity }
    val inStockCount = totalTypes - outOfStockCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- KHỐI THỐNG KÊ TỔNG HỢP ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E)),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TỔNG LOẠI", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("$totalTypes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("CÒN HÀNG", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("$inStockCount", color = Color(0xFF4CAF50), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HẾT HÀNG", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("$outOfStockCount", color = Color(0xFFF44336), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Danh sách chi tiết các khe chứa",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A237E),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // --- DANH SÁCH TỪNG LOẠI NƯỚC ---
        items(inventoryList) { item ->
            val isOutOfStock = item.quantity <= item.minQuantity

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cột trái: Tên nước
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.productName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "Mã: ${item.productId} | Máy: ${item.machineId}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Cột phải: Số lượng + Nhãn + Nút Nạp Đầy
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${item.quantity} / 2 lon",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (isOutOfStock) Color(0xFFD32F2F) else Color(0xFF1A237E)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isOutOfStock) {
                            // THÊM NÚT BẤM "NẠP ĐẦY 2/2" Ở ĐÂY
                            Button(
                                onClick = {
                                    // Tạo một bản sao của item hiện tại nhưng sửa quantity thành 2
                                    val restockedItem = item.copy(quantity = 2)
                                    // Gọi hàm update của bạn đẩy lên Supabase
                                    viewModel.updateInventory(item.machineId, restockedItem)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("NẠP ĐẦY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "CÒN HÀNG",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}