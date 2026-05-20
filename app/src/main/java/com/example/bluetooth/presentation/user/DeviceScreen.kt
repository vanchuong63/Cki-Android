package com.example.bluetooth.presentation.user

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.bluetooth.R
import com.example.bluetooth.presentation.BluetoothUiState
import com.example.bluetooth.ui.theme.BluetoothTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import java.text.DecimalFormat

// --- HỆ THỐNG MÀU SẮC ---
val PrimaryBlue = Color(0xFF00AEEF)
val SecondaryBlue = Color(0xFF0083B0)
val ActiveGreen = Color(0xFF00C853)
val BgGrayLight = Color(0xFFF8F9FA)
val BadgeOrange = Color(0xFFFF9F43)
val PriceBlue = Color(0xFF2980B9)

// --- DATA MODELS ---
data class Beverage(
    val id: String,
    val name: String,
    val price: Int,
    val imageRes: Int,
    val category: String,
    val description: String = "Giải khát tức thì"
)

data class CartItem(
    val beverage: Beverage,
    val quantity: Int
)

data class PromoBanner(
    val title: String,
    val subtitle: String,
    val imageRes: Int,
    val gradientColors: List<Color>
)

// --- DANH SÁCH BANNER ---
val BannerImages = listOf(
    R.drawable.ads_phanthuong,
    R.drawable.ads1,
    R.drawable.ads_pepsi,
    R.drawable.ads_coca,
)

// --- DANH SÁCH NƯỚC UỐNG (DATABASE) ---
val SampleBeverages = listOf(
    Beverage("COCA", "Coca Cola", 10000, R.drawable.coca, "Nước ngọt"),
    Beverage("PEPSI", "Pepsi", 10000, R.drawable.pepsi, "Nước ngọt"),
    Beverage("7UP", "7Up", 10000, R.drawable.up, "Nước ngọt"),
    Beverage("STING", "Sting Dâu", 12000, R.drawable.sting, "Nước ngọt"),
    Beverage("MONSTER", "Monster Energy", 25000, R.drawable.monster, "Tăng lực"),
    Beverage("REDBULL", "Red Bull", 20000, R.drawable.redbull, "Tăng lực"),
    Beverage("WATER", "Nước khoáng LaVie", 5000, R.drawable.lavie, "Nước lọc"),
    Beverage("AQUAFINA", "Aquafina", 5000, R.drawable.nuocloc, "Nước lọc"),
    Beverage("TEA", "Trà Xanh Không Độ", 12000, R.drawable.khongdo, "Trà"),
    Beverage("C2", "Trà C2", 10000, R.drawable.c2, "Trà")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onProductSelect: (product: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    // --- STATES ---
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    val cartItems = remember { mutableStateListOf<CartItem>() }
    
    val categories = remember { listOf("Tất cả", "Nước ngọt", "Nước lọc", "Trà", "Tăng lực") }

    // --- LOGIC LỌC SẢN PHẨM ---
    val filteredBeverages = remember(searchQuery, selectedCategory) {
        SampleBeverages.filter { beverage ->
            val matchesSearch = beverage.name.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "Tất cả" || beverage.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    val totalQuantity = cartItems.sumOf { it.quantity }
    val totalPrice = cartItems.sumOf { it.beverage.price * it.quantity }

    var showCartSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var selectedBeverageForConfirm by remember { mutableStateOf<Beverage?>(null) }

    val gridBottomPadding = if (totalQuantity > 0) 96.dp else 24.dp

    Scaffold(
        containerColor = BgGrayLight,
        topBar = {
            HeaderSection(
                isConnected = state.isConnected,
                cartCount = totalQuantity,
                onCartClick = { if (totalQuantity > 0) showCartSheet = true },
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(BgGrayLight)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 0.dp,
                    end = 0.dp,
                    bottom = gridBottomPadding
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Banner Promo (Carousel)
                item(span = { GridItemSpan(2) }) {
                    PromoCarousel()
                }

                // 2. Ô Tìm Kiếm
                item(span = { GridItemSpan(2) }) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it }
                    )
                }

                // 3. Danh mục
                item(span = { GridItemSpan(2) }) {
                    CategoryChips(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                }

                // 4. Tiêu đề danh sách
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Danh sách đồ uống" else "Kết quả tìm kiếm",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "${filteredBeverages.size} món",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 5. Lưới sản phẩm
                itemsIndexed(filteredBeverages) { index, beverage ->
                    BeverageGridItem(
                        beverage = beverage,
                        isEnabled = state.isConnected,
                        onAddToCart = {
                            val existingIndex = cartItems.indexOfFirst { it.beverage.id == beverage.id }
                            if (existingIndex != -1) {
                                cartItems[existingIndex] = cartItems[existingIndex].copy(quantity = cartItems[existingIndex].quantity + 1)
                            } else {
                                cartItems.add(CartItem(beverage, 1))
                            }
                        },
                        onSelect = { selectedBeverageForConfirm = beverage },
                        modifier = Modifier.padding(
                            start = if (index % 2 == 0) 24.dp else 0.dp,
                            end = if (index % 2 != 0) 24.dp else 0.dp
                        )
                    )
                }
                
                // Trường hợp không có kết quả
                if (filteredBeverages.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Không tìm thấy nước bạn cần", color = Color.Gray)
                        }
                    }
                }
            }

            // Thanh tổng hợp giỏ hàng nổi
            AnimatedVisibility(
                visible = totalQuantity > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CartSummaryBar(
                    quantity = totalQuantity,
                    totalPrice = totalPrice,
                    onClick = { showCartSheet = true }
                )
            }
        }

        // Overlay Xác nhận
        selectedBeverageForConfirm?.let { beverage ->
            ConfirmPurchaseDialog(
                beverage = beverage,
                onDismiss = { selectedBeverageForConfirm = null },
                onConfirm = {
                    selectedBeverageForConfirm = null
                    val existingIndex = cartItems.indexOfFirst { it.beverage.id == beverage.id }
                    if (existingIndex != -1) {
                        cartItems[existingIndex] = cartItems[existingIndex].copy(quantity = cartItems[existingIndex].quantity + 1)
                    } else {
                        cartItems.add(CartItem(beverage, 1))
                    }
                }
            )
        }

        // Bottom Sheet Giỏ hàng
        if (showCartSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCartSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                CartDetailSheetContent(
                    cartItems = cartItems,
                    totalPrice = totalPrice,
                    onIncrease = { item ->
                        val idx = cartItems.indexOf(item)
                        cartItems[idx] = item.copy(quantity = item.quantity + 1)
                    },
                    onDecrease = { item ->
                        val idx = cartItems.indexOf(item)
                        if (item.quantity > 1) {
                            cartItems[idx] = item.copy(quantity = item.quantity - 1)
                        } else {
                            cartItems.removeAt(idx)
                            if (cartItems.isEmpty()) showCartSheet = false
                        }
                    },
                    onRemove = { item ->
                        cartItems.remove(item)
                        if (cartItems.isEmpty()) showCartSheet = false
                    },
                    onCheckout = {
                        showCartSheet = false
                        if (cartItems.isNotEmpty()) onProductSelect(cartItems[0].beverage.id)
                    }
                )
            }
        }
    }
}

// --- CÁC COMPONENT CON ---

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp),
        placeholder = { Text("Bạn muốn uống gì hôm nay?", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = BgGrayLight.copy(alpha = 0.5f)
        )
    )
}

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            
            // Lấy Icon tương ứng
            val icon = when (category) {
                "Nước ngọt" -> Icons.Default.LocalDrink
                "Nước lọc" -> Icons.Default.WaterDrop
                "Trà" -> Icons.Default.EmojiFoodBeverage
                "Tăng lực" -> Icons.Default.ElectricBolt
                else -> null
            }

            // Cấu hình màu sắc theo hình ảnh bạn cung cấp
            val (bgColor, contentColor, iconColor) = when (category) {
                "Tất cả" -> if (isSelected) {
                    Triple(Color(0xFF00AEEF), Color.White, Color.White)
                } else {
                    Triple(Color.White, Color.Gray, Color.Gray)
                }
                "Nước ngọt" -> Triple(Color(0xFFFFF1F2), Color(0xFFD32F2F), Color(0xFFD32F2F))
                "Nước lọc" -> Triple(Color(0xFFE0F7FA), Color(0xFF1976D2), Color(0xFF1976D2))
                "Trà" -> Triple(Color(0xFFF1F8E9), Color(0xFF388E3C), Color(0xFF388E3C))
                "Tăng lực" -> Triple(Color(0xFFFFF4E5), Color(0xFFD35400), Color(0xFFFF9100))
                else -> Triple(Color.White, Color.Gray, Color.Gray)
            }

            val borderColor = if (isSelected && category == "Tất cả") Color.Transparent else contentColor.copy(alpha = 0.15f)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = bgColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onCategorySelected(category) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = category,
                        color = contentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    isConnected: Boolean, 
    cartCount: Int, 
    onCartClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(PrimaryBlue, SecondaryBlue)))
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(32.dp).offset(x = (-8).dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "XIN CHÀO!",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isConnected) ActiveGreen else Color.Red, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "Sẵn sàng" else "Mất kết nối",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Máy Bán Nước Tự Động",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Box(modifier = Modifier.clickable { onCartClick() }) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                    }
                    if (cartCount > 0) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(18.dp),
                            shape = CircleShape,
                            color = BadgeOrange
                        ) {
                            Text(
                                text = cartCount.toString(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartSummaryBar(quantity: Int, totalPrice: Int, onClick: () -> Unit) {
    val formatter = DecimalFormat("#,###đ")
    Surface(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = PrimaryBlue
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("$quantity sản phẩm", color = Color.White, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatter.format(totalPrice), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun BeverageGridItem(
    beverage: Beverage,
    isEnabled: Boolean,
    onAddToCart: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = DecimalFormat("#,###đ")
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled) { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp).background(BgGrayLight),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = beverage.imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(90.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(beverage.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(beverage.category, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatter.format(beverage.price), fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Surface(
                        modifier = Modifier.size(28.dp).clickable { onAddToCart() },
                        shape = CircleShape,
                        color = PrimaryBlue
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PromoCarousel() {
    val pagerState = rememberPagerState(pageCount = { BannerImages.size })

    // Tự động chuyển banner sau mỗi 4 giây
    LaunchedEffect(Unit) {
        while (true) {
            yield()
            delay(4000)
            if (BannerImages.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % BannerImages.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 12.dp
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).let { 
                if (it < 0) -it else it 
            }
            
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Hiệu ứng phóng to/thu nhỏ và làm mờ khi chuyển trang
                        val scale = 1f - (pageOffset * 0.15f).coerceIn(0f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - (pageOffset * 0.5f).coerceIn(0f, 1f)
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Image(
                    painter = painterResource(id = BannerImages[page]),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Pager Indicator
        Row(
            Modifier
                .height(24.dp)
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(BannerImages.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color = if (isSelected) PrimaryBlue else Color.LightGray.copy(alpha = 0.5f)
                val width = if (isSelected) 18.dp else 6.dp
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clip(CircleShape)
                        .background(color)
                        .width(width)
                        .height(6.dp)
                        .animateContentSize()
                )
            }
        }
    }
}

@Composable
fun CartDetailSheetContent(
    cartItems: List<CartItem>,
    totalPrice: Int,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (CartItem) -> Unit,
    onCheckout: () -> Unit
) {
    val formatter = DecimalFormat("#,###đ")
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text("Giỏ hàng của bạn", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(cartItems) { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(item.beverage.imageRes), null, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.beverage.name, fontWeight = FontWeight.Bold)
                        Text(formatter.format(item.beverage.price), color = PrimaryBlue, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onDecrease(item) }, modifier = Modifier.size(32.dp)) {
                            Icon(if (item.quantity > 1) Icons.Rounded.Remove else Icons.Rounded.Delete, null, tint = Color.Gray)
                        }
                        Text("${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = { onIncrease(item) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Add, null, tint = PrimaryBlue)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tổng cộng", fontWeight = FontWeight.Bold)
            Text(formatter.format(totalPrice), fontWeight = FontWeight.Black, fontSize = 18.sp, color = PrimaryBlue)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text("Thanh toán ngay", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfirmPurchaseDialog(beverage: Beverage, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val formatter = DecimalFormat("#,###đ")
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(beverage.imageRes), null, modifier = Modifier.size(120.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(beverage.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(formatter.format(beverage.price), fontSize = 24.sp, fontWeight = FontWeight.Black, color = PriceBlue)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) { Text("Thêm vào giỏ") }
                TextButton(onClick = onDismiss) { Text("Bỏ qua", color = Color.Gray) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceScreenPreview() {
    BluetoothTheme {
        DeviceScreen(state = BluetoothUiState(isConnected = true), onProductSelect = {}, onNavigateBack = {})
    }
}
