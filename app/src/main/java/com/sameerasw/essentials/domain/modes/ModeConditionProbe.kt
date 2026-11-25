/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Modes
 * File: ModeConditionProbe.kt
 * Description: Reads the current state of conditions whose modules only report changes.
 */

package com.sameerasw.essentials.domain.modes

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import com.sameerasw.essentials.utils.CarConnectionMonitor
import java.util.concurrent.TimeUnit

object ModeConditionProbe {
    private const val TAG = "ModeConditionProbe"
    private const val LOCATION_TIMEOUT_SECONDS = 15L

    data class Target(
        val modeId: String,
        val key: String,
        val condition: ModeCondition,
    )

    fun probe(
        context: Context,
        targets: List<Target>,
    ) {
        targets.forEach { target ->
            val met =
                runCatching { read(context, target.condition) }
                    .onFailure { Log.w(TAG, "Probe failed for ${target.condition}", it) }
                    .getOrNull()
            if (met != null) ModeAutomation.onConditionChanged(context, target.modeId, target.key, met)
        }
    }

    private fun read(
        context: Context,
        condition: ModeCondition,
    ): Boolean? =
        when (condition) {
            is ModeCondition.Wifi -> currentSsid(context)?.let { it == condition.ssid }
            is ModeCondition.Bluetooth -> isBluetoothConnected(context, condition.deviceAddress)
            is ModeCondition.Place -> currentLocation(context)?.let { isInside(it, condition) }
            is ModeCondition.AndroidAuto -> CarConnectionMonitor.isProjecting
            is ModeCondition.Charging -> context.getSystemService(BatteryManager::class.java)?.isCharging
            else -> null
        }

    @Suppress("DEPRECATION")
    private fun currentSsid(context: Context): String? {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val ssid = wifi.connectionInfo?.ssid?.removeSurrounding("\"")
        return ssid?.takeIf { it.isNotEmpty() && it != "<unknown ssid>" }
    }

    @SuppressLint("MissingPermission")
    private fun isBluetoothConnected(
        context: Context,
        address: String,
    ): Boolean? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
        val device = adapter.bondedDevices?.find { it.address == address } ?: return false
        return BluetoothDevice::class.java.getMethod("isConnected").invoke(device) as? Boolean
    }

    @SuppressLint("MissingPermission")
    private fun currentLocation(context: Context): Location? {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val client = LocationServices.getFusedLocationProviderClient(context)
        return Tasks.await(client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null), LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun isInside(
        location: Location,
        place: ModeCondition.Place,
    ): Boolean {
        val distance = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, place.latitude, place.longitude, distance)
        return distance[0] <= place.radiusMeters
    }
}
