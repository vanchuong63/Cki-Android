package com.example.admin.bluetooth.mapper

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import com.example.admin.bluetooth.domain.model.BluetoothDeviceDomain

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return com.example.admin.bluetooth.domain.model.BluetoothDeviceDomain(
        name = name,
        address = address
    )
}