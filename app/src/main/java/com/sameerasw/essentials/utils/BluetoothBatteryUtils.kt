package com.sameerasw.essentials.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.annotation.Keep

object BluetoothBatteryUtils {

    @Keep
    data class BluetoothDeviceBattery(
        val name: String,
        val level: Int,
        val address: String
    )

    @SuppressLint("MissingPermission")
    fun getPairedDevicesBattery(context: Context): List<BluetoothDeviceBattery> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return emptyList()

        if (!adapter.isEnabled) return emptyList()

        val devices = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            return emptyList()
        }
        
        val batteryList = mutableListOf<BluetoothDeviceBattery>()

        devices.forEach { device ->
            try {
                // Method 1: Reflection on device.getBatteryLevel()
                // This is a hidden API, widely supported on many devices.
                // Returns -1 if not supported or not connected.
                val method = device.javaClass.getMethod("getBatteryLevel")
                val level = method.invoke(device) as Int

                if (level != -1) {
                    val name = device.alias ?: device.name ?: "Unknown"
                    batteryList.add(BluetoothDeviceBattery(name, level, device.address))
                }
            } catch (e: Exception) {
                // Reflection might fail or permission issues
            }
        }
        
        return batteryList
    }
}
