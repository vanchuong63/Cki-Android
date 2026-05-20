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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.bluetooth.presentation.user.NearbyMachinesScreen
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
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        if (!hideBottomBar) {
                            AppBottomBar(
                                currentRoute = currentRoute,
                                isGuest = isGuest,
                                onNavigate = { route ->
                                    if (currentRoute != route) {
                                        navController.navigate(route) {
                                            launchSingleTop = true
                                            if (route == "selection") {
                                                popUpTo("selection") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
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
                                    onClaimFreeOrder = {
                                        viewModel.claimFreeOrder()
                                    },
                                    onBack = {
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable("help") {
                                HelpScreen()
                            }

                            composable("nearby") {
                                NearbyMachinesScreen()
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

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    isGuest: Boolean,
    onNavigate: (String) -> Unit
) {
    val items = buildList {
        add(BottomNavItem("selection", "Mua nước", Icons.Default.Home))
        add(BottomNavItem("nearby", "Tìm máy", Icons.Default.LocationOn))
        if (!isGuest) {
            add(BottomNavItem("history", "Lịch sử", Icons.AutoMirrored.Filled.List))
            add(BottomNavItem("reward", "Thưởng", Icons.Default.Star))
            add(BottomNavItem("profile", "Cá nhân", Icons.Default.Person))
        }
        add(BottomNavItem("help", "Hỗ trợ", Icons.Default.Info))
    }

    val rows = if (items.size > 4) items.chunked(3) else listOf(items)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(10.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        BottomNavButton(
                            item = item,
                            selected = currentRoute == item.route,
                            onClick = { onNavigate(item.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavButton(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF00AEEF)
    val inactiveColor = Color(0xFF6B7280)
    val backgroundColor = if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent
    val contentColor = if (selected) activeColor else inactiveColor

    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 28.dp else 26.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            color = contentColor,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}
