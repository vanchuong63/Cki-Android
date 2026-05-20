package com.example.bluetooth.presentation

import com.example.bluetooth.domain.model.BluetoothDevice


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
    val selectedProduct: SelectedProduct? = null,
    val isCheckingReward: Boolean = false,
    val isFreeOrder: Boolean = false,
    val paidOrdersUntilFree: Int = 0,
    val isCompletingOrder: Boolean = false
)
