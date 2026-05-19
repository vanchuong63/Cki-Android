package com.example.bluetooth.presentation.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.R
import com.example.bluetooth.subpabase.SupabaseRepository
import com.example.bluetooth.ui.theme.BluetoothTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
private fun registerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF00AEEF),
    unfocusedBorderColor = Color(0xFFE0E0E0),
    focusedLabelColor = Color(0xFF00AEEF),
    cursorColor = Color(0xFF00AEEF),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    // Check if we are in Preview mode to avoid crashing on Firebase/Patterns initialization
    val isPreview = LocalInspectionMode.current
    
    val auth = remember { 
        if (isPreview) null 
        else {
            // FirebaseAuth.getInstance() can fail if Firebase is not initialized (e.g. in some test/preview environments)
            try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        }
    }

    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun validate(): String? {
        if (phone.isBlank() || email.isBlank() || password.isBlank()) return "Vui lòng nhập đầy đủ thông tin"
        
        // Use a safe email validation for Preview as android.util.Patterns is a stub in some environments
        val isEmailValid = if (isPreview) {
            email.contains("@") && email.contains(".")
        } else {
            try {
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
            } catch (e: Exception) {
                email.contains("@") && email.contains(".")
            }
        }
        
        if (!isEmailValid) return "Email không đúng định dạng"
        if (!phone.matches(Regex("^(0|\\+84)[0-9]{9,10}$"))) return "Số điện thoại không hợp lệ"
        if (password.length < 6) return "Mật khẩu phải có ít nhất 6 ký tự"
        if (password != confirmPassword) return "Mật khẩu xác nhận không khớp"
        return null
    }

    fun register() {
        if (isPreview) return
        val err = validate()
        if (err != null) { errorMessage = err; return }
        isLoading = true
        errorMessage = null
        
        auth?.createUserWithEmailAndPassword(email.trim(), password)
            ?.addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                // Dùng Coroutine của Main để quản lý isLoading
                CoroutineScope(Dispatchers.Main).launch {
                    val resultSupabase = SupabaseRepository.createUserProfile(
                        uid = uid,
                        phone = phone.trim(),
                        email = email.trim()
                    )

                    isLoading = false

                    if (resultSupabase.isSuccess) {
                        onRegisterSuccess()
                    } else {
                        errorMessage = "Đăng ký thất bại. Thử lại sau"
                    }
                }

            }
            ?.addOnFailureListener { e ->
                isLoading = false
                e.printStackTrace()

                errorMessage = when {
                    e.message?.contains("email address is already in use") == true -> "Email này đã được đăng ký"
                    e.message?.contains("badly formatted") == true -> "Email không hợp lệ"
                    e.message?.contains("weak-password") == true -> "Mật khẩu quá yếu"
                    else -> "Đăng ký thất bại. Thử lại sau"
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF00AEEF), Color(0xFF0083B0))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Header ──────────────────────────────────────
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                imageVector = Icons.Default.LocalDrink,
                contentDescription = null,
                modifier = Modifier.size(90.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Tạo tài khoản mới", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Đăng ký để tích điểm & nhận ưu đãi", fontSize = 13.sp, color = Color.White.copy(0.8f))

            Spacer(modifier = Modifier.height(28.dp))

            // ── Card form ───────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Đăng ký", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))

                    // Số điện thoại
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Số điện thoại") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF00AEEF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = registerFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Color(0xFF00AEEF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = registerFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Mật khẩu
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF00AEEF)) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.Gray)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = registerFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Xác nhận mật khẩu
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Xác nhận mật khẩu") },
                        leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = Color(0xFF00AEEF)) },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = Color.Gray)
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = registerFieldColors(),
                        isError = confirmPassword.isNotEmpty() && confirmPassword != password
                    )

                    // Lỗi
                    AnimatedVisibility(visible = errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Nút đăng ký
                    Button(
                        onClick = { register() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF)),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Tạo tài khoản", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("Đã có tài khoản? ", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            "Đăng nhập",
                            color = Color(0xFF00AEEF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    BluetoothTheme {
        RegisterScreen(onRegisterSuccess =  {} , onNavigateToLogin = {})
    }
}
