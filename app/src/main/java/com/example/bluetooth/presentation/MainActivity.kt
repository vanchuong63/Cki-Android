package com.example.bluetooth.presentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.bluetooth.presentation.log.LoginScreen
import com.example.bluetooth.presentation.log.RegisterScreen
import com.example.bluetooth.presentation.user.DeviceScreen
import com.example.bluetooth.presentation.user.HelpScreen
import com.example.bluetooth.presentation.user.HistoryScreen
import com.example.bluetooth.presentation.user.PaymentScreen
import com.example.bluetooth.presentation.user.RewardsScreen
import com.example.bluetooth.presentation.user.ProfileScreen
import com.example.bluetooth.presentation.user.UserViewModel
import com.example.bluetooth.presentation.user.ThankYouScreen
import com.example.bluetooth.ui.theme.BluetoothTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val bluetoothManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BluetoothTheme {
                val viewModel = hiltViewModel<BluetoothViewModel>()
                val userViewModel = hiltViewModel<UserViewModel>()
                val state by viewModel.state.collectAsState()
                val navController = rememberNavController()

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Kiểm tra xem có đang ở màn hình login/register/payment không để ẩn BottomBar
                val hideBottomBar = currentRoute == "login" || currentRoute == "register" || currentRoute == "payment" || currentRoute == "thank_you"
                val isGuest = userViewModel.isGuest

                // Launcher để yêu cầu bật Bluetooth
                val enableBluetoothLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (!isBluetoothEnabled) {
                        Toast.makeText(this, "Vui lòng bật Bluetooth để sử dụng máy bán nước", Toast.LENGTH_LONG).show()
                    }
                }

                // Launcher để yêu cầu các quyền Bluetooth & Vị trí
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    val canEnableBluetooth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        perms[Manifest.permission.BLUETOOTH_CONNECT] == true
                    } else true

                    if (canEnableBluetooth && !isBluetoothEnabled) {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                }

                LaunchedEffect(Unit) {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } else {
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                    permissionLauncher.launch(permissions)
                }

                Scaffold(
                    bottomBar = {
                        if (!hideBottomBar) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 24.dp)
                                    .shadow(12.dp, RoundedCornerShape(24.dp))
                                    .background(Color.White, RoundedCornerShape(24.dp))
                                    .height(80.dp)
                            ) {
                                NavigationBar(
                                    containerColor = Color.Transparent,
                                    tonalElevation = 0.dp
                                ) {
                                    val itemColors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0984E3),
                                        selectedTextColor = Color(0xFF0984E3),
                                        indicatorColor = Color(0xFF0984E3).copy(alpha = 0.1f),
                                        unselectedIconColor = Color.LightGray
                                    )
                                    // ── Mua nước ──────────────────────────────
                                    NavigationBarItem(
                                        selected = currentRoute == "selection",
                                        onClick = {
                                            if (currentRoute != "selection") {
                                                navController.navigate("selection") {
                                                    popUpTo("selection") { inclusive = true }
                                                }
                                            }
                                        },
                                        label = { Text("Mua nước", fontSize = 11.sp) },
                                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                        colors = itemColors
                                    )

                                    if (!isGuest) {
                                        // ── Lịch sử ───────────────────────────────
                                        NavigationBarItem(
                                            selected = currentRoute == "history",
                                            onClick = {
                                                if (currentRoute != "history") {
                                                    navController.navigate("history")
                                                }
                                            },
                                            label = { Text("Lịch sử", fontSize = 11.sp) },
                                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                            colors = itemColors
                                        )

                                        // ── Phần thưởng ───────────────────────────
                                        NavigationBarItem(
                                            selected = currentRoute == "reward",
                                            onClick = {
                                                if (currentRoute != "reward") {
                                                    navController.navigate("reward")
                                                }
                                            },
                                            label = { Text("Thưởng", fontSize = 11.sp) },
                                            icon = { Icon(Icons.Default.Star, contentDescription = null) },
                                            colors = itemColors
                                        )

                                        NavigationBarItem(
                                            selected = currentRoute == "profile",
                                            onClick = {
                                                if (currentRoute != "profile") {
                                                    navController.navigate("profile")
                                                }
                                            },
                                            label = { Text("Cá nhân", fontSize = 11.sp) },
                                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                            colors = itemColors
                                        )
                                    }

                                    // ── Hỗ trợ ────────────────────────────────
                                    NavigationBarItem(
                                        selected = currentRoute == "help",
                                        onClick = {
                                            if (currentRoute != "help") {
                                                navController.navigate("help")
                                            }
                                        },
                                        label = { Text("Hỗ trợ", fontSize = 11.sp) },
                                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                                        colors = itemColors
                                    )
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "login"
                        ) {
                            composable("login") {
                                LoginScreen(
                                    onLoginSuccess = {
                                        userViewModel.refresh()
                                        navController.navigate("selection") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onNavigateToRegister = { navController.navigate("register") },
                                    onContinueAsGuest = {
                                        userViewModel.refresh()
                                        navController.navigate("selection") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("register") {
                                RegisterScreen(
                                    onRegisterSuccess = { navController.navigate("login") },
                                    onNavigateToLogin = { navController.navigate("login") }
                                )
                            }

                            composable("selection") {
                                DeviceScreen(
                                    state = state,
                                    onProductSelect = { id ->
                                        viewModel.selectProduct(id)
                                        navController.navigate("payment")
                                    },
                                    onNavigateBack = {
                                        userViewModel.signOut {
                                            navController.navigate("login") {
                                                popUpTo(0)
                                            }
                                        }
                                    }
                                )
                            }

                            composable("payment") {
                                PaymentScreen(
                                    state = state,
                                    paymentStatus = viewModel.paymentStatus,
                                    onPaymentDetected = {
                                        viewModel.onPaymentDetected()

                                        // refresh lại để cập nhật giao diện
                                        userViewModel.refresh()

                                        navController.navigate("thank_you") {
                                            popUpTo("payment") { inclusive = true }
                                        }
                                    },
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("help") {
                                HelpScreen()
                            }

                            composable("history") {
                                HistoryScreen()
                            }

                            composable("reward") {
                                RewardsScreen()
                            }

                            composable("profile") {
                                ProfileScreen(onNavigateToLogin = {
                                    userViewModel.signOut {
                                        navController.navigate("login") {
                                            popUpTo(0)
                                        }
                                    }
                                })
                            }
                            composable("thank_you") {
                                // Bạn có thể truyền tên sản phẩm từ ViewModel nếu muốn, ở đây để mặc định
                                ThankYouScreen(
                                    productName = state.selectedProduct?.name ?: "đồ uống",
                                    onNavigateHome = {
                                        // Reset lại giao diện user và quay về trang chọn nước
                                        userViewModel.refresh()
                                        navController.navigate("selection") {
                                            popUpTo("selection") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
