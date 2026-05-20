package com.example.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


private fun formatNotifTime(isoString: String?): String {
    if (isoString == null) return "Vừa xong"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        // Cắt bỏ phần milliseconds và timezone nếu có
        val clean = isoString.substringBefore(".").substringBefore("Z").substringBefore("+")
        val date = sdf.parse(clean) ?: return isoString
        SimpleDateFormat("dd/MM HH:mm", Locale("vi")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }.format(date)
    } catch (e: Exception) {
        isoString.take(16).replace("T", " ")
    }
}

@Composable
fun NotificationScreen(viewModel: AdminViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F8))
            .padding(16.dp)
    ) {
        Text(
            "Thông báo hệ thống", fontSize = 20.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF2C3E7A),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Không có thông báo nào", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) Color.White else Color(0xFFFFF3F3)
                        ),
                        onClick = { viewModel.markRead(notif.id) }
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning, null,
                                tint = if (notif.isRead) Color.Gray else Color(0xFFE74C3C)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(notif.message, fontWeight = if (notif.isRead) FontWeight.Normal else FontWeight.Bold)
                                Text(
                                    SimpleDateFormat("dd/MM HH:mm", Locale("vi")).format(Date(notif.createdAt)),
                                    fontSize = 12.sp, color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}