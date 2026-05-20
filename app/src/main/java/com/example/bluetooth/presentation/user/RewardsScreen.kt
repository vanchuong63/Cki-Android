package com.example.bluetooth.presentation.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private data class RewardOffer(val title: String,val description: String, val cost: Int, val type: String)


private val offers = listOf(
    RewardOffer(
        title = "Giảm 5.000đ",
        description = "Áp dụng cho đơn hàng tiếp theo",
        cost = 50,
        type = "FIX_5K"
    ),
    RewardOffer(
        title = "Giảm 20%",
        description = "Áp dụng cho đơn từ 20.000đ trở lên",
        cost = 80,
        type = "PERCENT_20"
    ),
    RewardOffer(
        title = "Nước suối miễn phí",
        description = "LaVie hoặc Aquafina",
        cost = 100,
        type = "FREE_WATER"
    ),
    RewardOffer(
        title = "1 lon Coca miễn phí",
        description = "Áp dụng cho Coca Cola",
        cost = 150,
        type = "FREE_COCA"
    ),
    RewardOffer(
        title = "Tặng 2 chai trà",
        description = "Trà Xanh Không Độ hoặc Trà C2",
        cost = 200,
        type = "FREE_2_TEA"
    ),
)

@Composable
fun RewardsScreen(
    viewModel: UserViewModel=hiltViewModel(),
    onNavigateToLogin: () -> Unit = {}
) {
    if (viewModel.isGuest) {
        GuestPlaceholder("Đăng nhập để tích điểm & nhận phần thưởng", onNavigateToLogin)
        return
    }

    val state by viewModel.state.collectAsState()
    val points = state.profile?.points ?: 0
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Hiển thị thông báo khi có message hoặc error từ ViewModel
    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Text(
                "Phần thưởng", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp, bottom = 16.dp)
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Color(0xFF00AEEF)) }
                return@Column
            }

            // Thẻ điểm tích lũy
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF00AEEF))
            ) {
                Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Điểm tích lũy của bạn", color = Color.White.copy(0.8f), fontSize = 13.sp)
                        Text("$points điểm", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text("Mỗi 1.000đ = 1 điểm", color = Color.White.copy(0.7f), fontSize = 11.sp)
                    }
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFDCB6E), modifier = Modifier.size(52.dp))
                }
            }

            // Progress bar đến mốc tiếp theo
            val next = offers.firstOrNull { it.cost > points }
            if (next != null) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Tiến đến: ${next.title}", fontSize = 12.sp, color = Color.Gray)
                    Text("$points/${next.cost}", fontSize = 12.sp, color = Color(0xFF00AEEF), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (points.toFloat() / next.cost).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF00AEEF), trackColor = Color(0xFF00AEEF).copy(0.15f)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Ưu đãi có thể đổi", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(offers) { offer ->
                    val canRedeem = points >= offer.cost
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (canRedeem) Color.White else Color(0xFFF8F9FA)
                        ),
                        elevation = CardDefaults.cardElevation(if (canRedeem) 3.dp else 0.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CardGiftcard, null,
                                tint = if (canRedeem) Color(0xFFE17055) else Color.LightGray,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(offer.title, fontWeight = FontWeight.SemiBold,
                                    color = if (canRedeem) Color.DarkGray else Color.LightGray)
                                Text("${offer.cost} điểm", fontSize = 12.sp,
                                    color = if (canRedeem) Color(0xFF00AEEF) else Color.LightGray)
                            }
                            Button(
                                onClick = { viewModel.redeemReward(offer.cost,offer.type, offer.title) },
                                enabled = canRedeem && !state.isLoading,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00AEEF),
                                    disabledContainerColor = Color(0xFFEEEEEE)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Đổi", fontSize = 13.sp,
                                    color = if (canRedeem) Color.White else Color.LightGray)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
