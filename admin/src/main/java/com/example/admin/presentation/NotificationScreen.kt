package com.example.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationScreen(viewModel: AdminViewModel) {
    val notifications by viewModel.notifications.collectAsState()
    var isDoorOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F8)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Thông báo hệ thống",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E2D60),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "MỞ KHÓA BẢO TRÌ (SERVO)",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1565C0),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDoorOpen) {
                                "Trạng thái: CỬA ĐANG MỞ"
                            } else {
                                "Trạng thái: ĐÃ KHÓA AN TOÀN"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDoorOpen) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                        )
                    }

                    Switch(
                        checked = isDoorOpen,
                        onCheckedChange = { open ->
                            isDoorOpen = open
                            viewModel.toggleMaintenanceDoor(open)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFF44336),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF4CAF50)
                        )
                    )
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Text("Chưa có thông báo nào.", color = Color.Gray)
            }
        }

        items(notifications) { notif ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (notif.isRead) Color.White else Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "CẢNH BÁO",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                        Text(text = notif.machineName, fontSize = 12.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = notif.message, fontSize = 15.sp, color = Color.Black)
                }
            }
        }
    }
}
