package com.example.bluetooth.presentation.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bluetooth.R
import com.example.bluetooth.presentation.BluetoothUiState
import com.example.bluetooth.ui.theme.BluetoothTheme
import java.text.DecimalFormat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaymentScreenPreview() {
    val fakeFlow = remember { MutableSharedFlow<Boolean>() }

    BluetoothTheme {
        PaymentScreen(
            state = BluetoothUiState(isFreeOrder = true),
            paymentStatus = fakeFlow,
            onPaymentDetected = {},
            onClaimFreeOrder = {},
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
    onClaimFreeOrder: () -> Unit,
    onBack: () -> Unit
) {
    val product = state.selectedProduct
    val payablePrice = if (state.isFreeOrder) 0 else product?.price ?: 0
    val qrUrl = "https://qr.sepay.vn/img?acc=VQRQAIDDN1936&bank=MBBank&amount=$payablePrice&des=${product?.id}"
    val formatter = remember { DecimalFormat("#,###'d'") }
    var selectedMethod by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        paymentStatus.collectLatest { isPaid ->
            if (isPaid) onPaymentDetected()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Thanh toan", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF00AEEF))
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
            if (!state.isCheckingReward && !state.isFreeOrder) {
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
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                    when {
                        state.isCheckingReward -> CheckingRewardContent()
                        state.isFreeOrder -> FreeOrderContent(
                            isCompletingOrder = state.isCompletingOrder,
                            onClaimFreeOrder = onClaimFreeOrder
                        )
                        else -> QrPaymentContent(qrUrl = qrUrl, productId = product?.id)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text("TONG THANH TOAN", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(
                        formatter.format(payablePrice),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00AEEF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEFF4F8),
                    contentColor = Color(0xFF1C1E23)
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Huy giao dich", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "THANH TOAN BAO MAT BOI VENDINGFLOW",
                fontSize = 10.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun CheckingRewardContent() {
    CircularProgressIndicator(color = Color(0xFF00AEEF))
    Spacer(modifier = Modifier.height(16.dp))
    Text("Dang kiem tra uu dai", fontSize = 16.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun FreeOrderContent(
    isCompletingOrder: Boolean,
    onClaimFreeOrder: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.CardGiftcard,
        contentDescription = null,
        tint = Color(0xFF00AEEF),
        modifier = Modifier.size(72.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("Don nay mien phi", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color(0xFF00AEEF))
    Text(
        "Ban da mua du 10 lan. Nhan don tiep theo voi gia 0d.",
        fontSize = 13.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(20.dp))
    Button(
        onClick = onClaimFreeOrder,
        enabled = !isCompletingOrder,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF))
    ) {
        if (isCompletingOrder) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text("Nhan nuoc mien phi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun QrPaymentContent(qrUrl: String, productId: String?) {
    Text("Quet ma de thanh toan", fontSize = 16.sp, fontWeight = FontWeight.Medium)
    Text("Su dung ung dung vi de quet", fontSize = 13.sp, color = Color.Gray)

    Spacer(modifier = Modifier.height(20.dp))

    Box(
        modifier = Modifier.size(220.dp).border(2.dp, Color(0xFF00AEEF), RoundedCornerShape(16.dp)).padding(10.dp)
    ) {
        AsyncImage(
            model = qrUrl,
            contentDescription = "Ma QR thanh toan",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    Surface(
        color = Color(0xFFE8F7FD),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Noi dung: $productId",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color(0xFF0083B0),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PaymentMethodTab(icon: Int, name: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(80.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(2.dp, if (isSelected) Color(0xFF00AEEF) else Color.Transparent)
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
                    color = Color(0xFF00AEEF)
                ) {
                    Text("OK", color = Color.White, fontSize = 8.sp, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
