package com.example.bluetooth.presentation.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onContinueAsGuest: () -> Unit,
    registeredSuccess: Boolean = false
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val snackbarHostState = remember { SnackbarHostState() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(registeredSuccess) {
        if (registeredSuccess)
            snackbarHostState.showSnackbar("Đăng ký thành công! Vui lòng đăng nhập.")
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) { errorMessage = "Vui lòng nhập đầy đủ"; return }
        isLoading = true; errorMessage = null
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { isLoading = false; onLoginSuccess() }
            .addOnFailureListener { e ->
                isLoading = false
                errorMessage = when {
                    e.message?.contains("password") == true -> "Sai mật khẩu"
                    e.message?.contains("no user") == true  -> "Email chưa được đăng ký"
                    e.message?.contains("badly formatted") == true -> "Email không hợp lệ"
                    else -> "Đăng nhập thất bại. Thử lại sau"
                }
            }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF00AEEF), Color(0xFF0083B0))))
    ) {
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.TopCenter).padding(top = 16.dp))

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            // Thay thế Image bằng Icon để giao diện chuyên nghiệp hơn
            Icon(
                imageVector = Icons.Default.LocalDrink,
                contentDescription = null,
                modifier = Modifier.size(90.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(12.dp))
            Text("Máy Bán Nước Tự Động", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Chào mừng bạn trở lại!", fontSize = 14.sp, color = Color.White.copy(0.8f))
            Spacer(Modifier.height(36.dp))

            Card(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Đăng nhập", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it; errorMessage = null },
                        Modifier.fillMaxWidth(), label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF00AEEF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = authFieldColors()
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it; errorMessage = null },
                        Modifier.fillMaxWidth(), label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF00AEEF)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.Gray)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        colors = authFieldColors()
                    )

                    AnimatedVisibility(errorMessage != null) {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { login() },
                        Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Text("Đăng nhập", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.Center) {
                        Text("Chưa có tài khoản? ", color = Color.Gray, fontSize = 14.sp)
                        Text("Đăng ký", color = Color(0xFF00AEEF), fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToRegister() })
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onContinueAsGuest) {
                Text("Tiếp tục mà không đăng nhập →",
                    color = Color.White.copy(0.85f), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00AEEF),
    unfocusedBorderColor = Color(0xFFE0E0E0),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFF8F9FA)
)
