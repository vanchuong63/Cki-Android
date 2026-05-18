package com.example.bluetooth.presentation.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.R
import com.example.bluetooth.presentation.BluetoothUiState
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.ui.tooling.preview.Preview
import com.example.bluetooth.ui.theme.BluetoothTheme
import coil.compose.AsyncImage

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaymentScreenPreview() {
    val fakeState = BluetoothUiState(
    )

    val fakeFlow = remember {
        kotlinx.coroutines.flow.MutableSharedFlow<Boolean>()
    }

    BluetoothTheme {
        PaymentScreen(
            state = fakeState,
            paymentStatus = fakeFlow,
            onPaymentDetected = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    state: BluetoothUiState,
    paymentStatus: SharedFlow<Boolean>,
    onPaymentDetected: () -> Unit,
    onBack: () -> Unit
) {

    val product = state.selectedProduct
    val qrUrl = "https://qr.sepay.vn/img?acc=VQRQAIDDN1936&bank=MBBank&amount=${product?.price}&des=${product?.id}"
    var selectedMethod by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = true) {
        paymentStatus.collectLatest { isPaid ->
            if (isPaid) {
                onPaymentDetected()
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thanh toán", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF1ABC9C))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = Color.LightGray.copy(alpha = 0.2f)) {
                            Icon(
                                imageVector = Icons.Outlined.DonutLarge,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentMethodTab(
                    icon = R.drawable.qr,
                    name = "MB Bank",
                    isSelected = selectedMethod == 0,
                    onClick = { selectedMethod = 0 },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodTab(
                    icon = R.drawable.qr,
                    name = "Momo",
                    isSelected = selectedMethod == 1,
                    onClick = { selectedMethod = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Khung chứa QR
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Quét mã để thanh toán", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text("Sử dụng ứng dụng Ví để quét", fontSize = 13.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Box(
                        modifier = Modifier.size(220.dp).border(2.dp, Color(0xFF1ABC9C), RoundedCornerShape(16.dp)).padding(10.dp)
                    ) {
                            AsyncImage(
                                model = qrUrl,
                                contentDescription = "Mã QR thanh toán",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit

                            )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = Color(0xFFE8F8F5),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Nội dung: ${product?.id}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color(0xFF16A085),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    val formatter = java.text.DecimalFormat("#,###đ")
                    val displayPrice = state.selectedProduct?.price?.let {formatter.format(it)} ?: "0đ"

                    Text("TỔNG THANH TOÁN", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(displayPrice, fontSize = 20.sp, fontWeight = FontWeight.Black, color= Color(0xFF1ABC9C))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Surface(
                        color = Color(0xFFFFF5F5),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nút hủy
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1E23))
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hủy giao dịch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Text(
                "THANH TOÁN BẢO MẬT BỞI VENDINGFLOW",
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun PaymentMethodTab(icon: Int, name: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(2.dp, if (isSelected) Color(0xFF1ABC9C) else Color.Transparent)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(painter = painterResource(id = icon), contentDescription = null, modifier = Modifier.size(32.dp))
            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (isSelected) Color.Black else Color.Gray)
        }
        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize()) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(16.dp),
                    shape = CircleShape,
                    color = Color(0xFF1ABC9C)
                ) {
                    Text("✓", color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
