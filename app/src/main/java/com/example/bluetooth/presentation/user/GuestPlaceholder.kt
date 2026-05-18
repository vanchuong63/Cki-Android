package com.example.bluetooth.presentation.user


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GuestPlaceholder(message: String, onLogin: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Lock, null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFFDFE6E9)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                message,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onLogin,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF)),
                modifier = Modifier.fillMaxWidth(0.6f).height(48.dp)
            ) {
                Text("Đăng nhập ngay", fontWeight = FontWeight.Bold)
            }
        }
    }
}