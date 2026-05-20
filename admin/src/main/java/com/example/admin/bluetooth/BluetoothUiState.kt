package com.example.admin.bluetooth

import com.example.admin.bluetooth.domain.model.BluetoothDevice


data class SelectedProduct(
    val id: String,
    val name: String,
    val price: Int
)
data class BluetoothUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val selectedProduct: SelectedProduct? = null
)
