package com.example.admin.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
// Đã thay thế FirestoreRepository bằng SupabaseAdminRepository
import com.example.admin.data.SupabaseAdminRepository
import com.example.admin.data.Machine
import com.example.admin.data.MachineInventory

private val BEVERAGES = listOf("COCA","PEPSI","7UP","STING","MONSTER","REDBULL","WATER","AQUAFINA","TEA","C2")
private val BEVERAGE_NAMES = mapOf(
    "COCA" to "Coca Cola", "PEPSI" to "Pepsi", "7UP" to "7Up",
    "STING" to "Sting Dâu", "MONSTER" to "Monster Energy", "REDBULL" to "Red Bull",
    "WATER" to "LaVie", "AQUAFINA" to "Aquafina", "TEA" to "Trà Không Độ", "C2" to "Trà C2"
)

@Composable
fun InventoryScreen(viewModel: AdminViewModel) {
    val machines by viewModel.machines.collectAsState()
    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var showUpdateDialog by remember { mutableStateOf<MachineInventory?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F8))
            .padding(16.dp)
    ) {
        Text(
            "Quản lý kho hàng", fontSize = 20.sp,
            fontWeight = FontWeight.Bold, color = Color(0xFF2C3E7A),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Chip chọn máy
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(machines) { machine ->
                val isSelected = selectedMachine?.id == machine.id
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF00AEEF) else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color(0xFFE0E0E0)),
                    modifier = Modifier.clickable { selectedMachine = machine }
                ) {
                    Text(
                        machine.name,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.padding(8.dp))

        selectedMachine?.let { machine ->
            // ĐÃ SỬA Ở ĐÂY: Gọi SupabaseAdminRepository thay vì Firestore
            val inventoryFlow = remember(machine.id) { SupabaseAdminRepository.getInventoryFlow(machine.id) }
            val inventory by inventoryFlow.collectAsState(emptyList())
            val inventoryMap = inventory.associateBy { it.productId }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(BEVERAGES) { productId ->
                    val item = inventoryMap[productId] ?: MachineInventory(
                        machineId = machine.id, // Bổ sung machineId cho an toàn với Supabase
                        productId = productId,
                        productName = BEVERAGE_NAMES[productId] ?: productId,
                        quantity = 0
                    )
                    val isLow = item.quantity <= item.minQuantity

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLow) Color(0xFFFFF3F3) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (isLow) Color(0xFFE74C3C).copy(alpha = 0.1f)
                                        else Color(0xFF00AEEF).copy(alpha = 0.1f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.LocalDrink, null,
                                    tint = if (isLow) Color(0xFFE74C3C) else Color(0xFF00AEEF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Còn: ${item.quantity} chai", fontSize = 12.sp, color = Color.Gray)
                                    if (isLow) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE74C3C).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "Sắp hết!",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp, color = Color(0xFFE74C3C),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { showUpdateDialog = item }) {
                                Icon(Icons.Default.Edit, null, tint = Color(0xFF00AEEF))
                            }
                        }
                    }
                }
            }
        } ?: Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
            Text("Chọn máy để xem tồn kho", color = Color.Gray)
        }
    }

    // Dialog cập nhật số lượng
    showUpdateDialog?.let { item ->
        var qtyStr by remember { mutableStateOf(item.quantity.toString()) }
        Dialog(onDismissRequest = { showUpdateDialog = null }) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Cập nhật tồn kho", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(item.productName, color = Color.Gray, fontSize = 13.sp)
                    Spacer(Modifier.padding(8.dp))
                    OutlinedTextField(
                        value = qtyStr, onValueChange = { qtyStr = it },
                        label = { Text("Số lượng (chai)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(Modifier.padding(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showUpdateDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Hủy") }
                        Button(
                            onClick = {
                                val qty = qtyStr.toIntOrNull() ?: return@Button
                                selectedMachine?.let { machine ->
                                    viewModel.updateInventory(machine.id, item.copy(quantity = qty))
                                }
                                showUpdateDialog = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF))
                        ) { Text("Lưu") }
                    }
                }
            }
        }
    }
}