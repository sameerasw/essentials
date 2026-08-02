package com.sameerasw.essentials.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.annotation.Keep

object BluetoothPairedDevicesUtil {

    @Keep
    data class PairedDevice(
        val name: String,
        val address: String
    )

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<PairedDevice> {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return emptyList()

        val devices = try {
            adapter.bondedDevices
        } catch (e: SecurityException) {
            return emptyList()
        }

        return devices.map { device ->
            PairedDevice(
                name = device.alias ?: device.name ?: device.address,
                address = device.address
            )
        }.sortedBy { it.name.lowercase() }
    }
}
