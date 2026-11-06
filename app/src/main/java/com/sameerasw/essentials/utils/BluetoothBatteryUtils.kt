/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: BluetoothBatteryUtils.kt
 * Description: Utility helper for BluetoothBatteryUtils.kt.
 */

package com.sameerasw.essentials.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.annotation.Keep

object BluetoothBatteryUtils {
    @Keep
    data class BluetoothDeviceBattery(
        val name: String,
        val level: Int,
        val address: String,
        val left: Int? = null,
        val right: Int? = null,
        val case: Int? = null,
    ) {
        val hasParts: Boolean get() = left != null || right != null || case != null
    }

    private const val TAG = "BtBattery"
    private const val KEY_LEFT = 10
    private const val KEY_RIGHT = 11
    private const val KEY_CASE = 12

    private class PartReader(private val context: Context) {
        private var reflectionBlocked = false
        private val dump: Map<String, Map<Int, String>> by lazy { loadDump() }

        private fun loadDump(): Map<String, Map<Int, String>> {
            if (!ShellUtils.isAvailable(context) || !ShellUtils.hasPermission(context)) return emptyMap()
            val text =
                ShellUtils.runCommandWithOutput(context, "dumpsys bluetooth_manager | grep -E 'Device:|Custom Metadata:|^ +[0-9]+ '", notifyOnError = false)
                    ?: return emptyMap()
            val result = HashMap<String, MutableMap<Int, String>>()
            var current: String? = null
            var inMetadata = false
            val deviceLine = Regex("""^\s*Device:\s+([0-9A-Fa-f:X]{17})""")
            val row = Regex("""^\s+(\d+)\s+(\S.*)$""")
            for (line in text.lineSequence()) {
                val device = deviceLine.find(line)
                if (device != null) {
                    current = device.groupValues[1].takeLast(5).uppercase()
                    inMetadata = false
                    continue
                }
                if (current == null) continue
                if (line.trim() == "Custom Metadata:") {
                    inMetadata = true
                    continue
                }
                if (inMetadata) {
                    val match = row.find(line)
                    if (match != null) {
                        result.getOrPut(current) { mutableMapOf() }[match.groupValues[1].toInt()] = match.groupValues[2].trim()
                    } else if (!line.contains("Key") && !line.contains("---") && line.isNotBlank()) {
                        inMetadata = false
                    }
                }
            }
            android.util.Log.d(TAG, "dump parsed for ${result.size} devices")
            return result
        }

        fun read(
            device: android.bluetooth.BluetoothDevice,
            key: Int,
        ): Int? {
            if (!reflectionBlocked) {
                try {
                    val raw = device.javaClass.getMethod("getMetadata", Int::class.javaPrimitiveType).invoke(device, key) as? ByteArray
                    return raw?.let { String(it).trim().toIntOrNull() }?.takeIf { it in 0..100 }
                } catch (e: Throwable) {
                    reflectionBlocked = true
                    android.util.Log.w(TAG, "metadata api blocked, using dump", e.cause ?: e)
                }
            }
            val value = dump[device.address.takeLast(5).uppercase()]?.get(key)?.toIntOrNull()?.takeIf { it in 0..100 }
            android.util.Log.d(TAG, "${device.address} key=$key dump=$value")
            return value
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevicesBattery(context: Context): List<BluetoothDeviceBattery> {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return emptyList()

        val isEnabled =
            try {
                adapter.isEnabled
            } catch (e: SecurityException) {
                false
            }
        if (!isEnabled) return emptyList()

        val devices =
            try {
                adapter.bondedDevices
            } catch (e: SecurityException) {
                return emptyList()
            }

        val batteryList = mutableListOf<BluetoothDeviceBattery>()
        val reader = PartReader(context)
        val separate = com.sameerasw.essentials.data.repository.SettingsRepository(context).isBatterySeparateChannelsEnabled()

        devices.forEach { device ->
            try {
                // Method 1: Reflection on device.getBatteryLevel()
                // This is a hidden API, widely supported on many devices.
                // Returns -1 if not supported or not connected.
                val method = device.javaClass.getMethod("getBatteryLevel")
                val level = method.invoke(device) as Int
                val left = if (separate) reader.read(device, KEY_LEFT) else null
                val right = if (separate) reader.read(device, KEY_RIGHT) else null
                val case = if (separate) reader.read(device, KEY_CASE) else null

                if (level != -1 || left != null || right != null || case != null) {
                    val name = device.alias ?: device.name ?: "Unknown"
                    val overall = if (level != -1) level else listOfNotNull(left, right).minOrNull() ?: case ?: -1
                    batteryList.add(BluetoothDeviceBattery(name, overall, device.address, left, right, case))
                }
            } catch (e: Exception) {
                // Reflection might fail or permission issues
            }
        }

        return batteryList
    }
}
