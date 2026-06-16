package com.example.admin.presentation

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun MachineMapScreen(viewModel: AdminViewModel) {
    val machines by viewModel.machines.collectAsState()
    val addState by viewModel.addMachineState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(16.0544, 108.2022), 13f)
    }

    LaunchedEffect(addState) {
        addState?.let {
            snackbar.showSnackbar(it)
            viewModel.clearAddState()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF00AEEF)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                machines.forEach { machine ->
                    Marker(
                        state = MarkerState(LatLng(machine.lat, machine.lng)),
                        title = machine.name,
                        snippet = machine.location,
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (machine.isOnline) BitmapDescriptorFactory.HUE_AZURE
                            else BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
            }

            // Card list dưới map
            LazyRow(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(machines) { machine ->
                    Card(
                        modifier = Modifier
                            .width(200.dp)
                            .clickable {
                                scope.launch {
                                    cameraState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(machine.lat, machine.lng), 16f
                                        )
                                    )
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalDrink, null,
                                tint = if (machine.isOnline) Color(0xFF00AEEF) else Color.Gray,
                                modifier = Modifier.width(28.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(machine.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (machine.isOnline) "● Online" else "● Offline",
                                    fontSize = 11.sp,
                                    color = if (machine.isOnline) Color(0xFF27AE60) else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMachineDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, location, lat, lng ->
                viewModel.addMachine(name, location, lat, lng)
                showAddDialog = false
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun AddMachineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("") }
    var lngStr by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val fusedLocation = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Hàm lấy tên địa chỉ từ tọa độ chạy ngầm
    fun fetchAddress(lat: Double, lng: Double) {
        latStr = lat.toString()
        lngStr = lng.toString()
        
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            location = addresses[0].getAddressLine(0) ?: ""
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            location = addresses[0].getAddressLine(0) ?: ""
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Không lấy được tên đường, vui lòng nhập tay", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(24.dp)) {
                Text("Thêm máy mới", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E7A))
                Spacer(Modifier.padding(8.dp))

                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Tên máy") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.padding(4.dp))
                OutlinedTextField(
                    value = location, onValueChange = { location = it },
                    label = { Text("Địa chỉ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.padding(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latStr, onValueChange = { latStr = it },
                        label = { Text("Latitude") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = lngStr, onValueChange = { lngStr = it },
                        label = { Text("Longitude") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Spacer(Modifier.padding(4.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            fusedLocation.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    fetchAddress(loc.latitude, loc.longitude)
                                    Toast.makeText(context, "Đã cập nhật vị trí!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Đang lấy vị trí mới...", Toast.LENGTH_SHORT).show()
                                    fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener { newLoc ->
                                            if (newLoc != null) {
                                                fetchAddress(newLoc.latitude, newLoc.longitude)
                                                Toast.makeText(context, "Đã lấy được vị trí!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Không thể lấy vị trí. Hãy bật GPS!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                }
                            }
                        } catch (e: SecurityException) {
                            Toast.makeText(context, "Thiếu quyền truy cập vị trí", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MyLocation, null, modifier = Modifier.width(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Lấy vị trí hiện tại")
                }
                Spacer(Modifier.padding(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Hủy") }
                    Button(
                        onClick = {
                            val lat = latStr.toDoubleOrNull() ?: 0.0
                            val lng = lngStr.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank() && location.isNotBlank()) {
                                onConfirm(name, location, lat, lng)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00AEEF))
                    ) { Text("Thêm") }
                }
            }
        }
    }
}