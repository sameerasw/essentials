/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: UsbDebuggingTileService.kt
 * Description: Quick Settings Tile Service for managing USB and Wireless (WiFi) debugging state.
 */

package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

@RequiresApi(Build.VERSION_CODES.N)
class UsbDebuggingTileService : BaseTileService() {

    override fun onClick() {
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            startActivityAndCollapse(intent)
            return
        }
        super.onClick()
    }

    override fun getTileLabel(): String = getString(R.string.tile_usb_debugging)

    override fun getTileSubtitle(): String {
        val usbOn = isUsbDebuggingEnabled()
        val wifiOn = isWifiDebuggingEnabled()
        return when {
            usbOn && wifiOn -> "USB & WiFi"
            usbOn -> "USB"
            wifiOn -> "WiFi"
            else -> getString(R.string.off)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon {
        return Icon.createWithResource(this, R.drawable.rounded_adb_24)
    }

    override fun getTileState(): Int {
        val usbOn = isUsbDebuggingEnabled()
        val wifiOn = isWifiDebuggingEnabled()
        return if (usbOn && wifiOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val usbOn = isUsbDebuggingEnabled()
        val wifiOn = isWifiDebuggingEnabled()

        val newState = if (usbOn && wifiOn) 0 else 1
        setUsbDebuggingEnabled(newState == 1)
        setWifiDebuggingEnabled(newState == 1)
    }

    private fun isUsbDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun setUsbDebuggingEnabled(enabled: Boolean) {
        try {
            Settings.Global.putInt(contentResolver, Settings.Global.ADB_ENABLED, if (enabled) 1 else 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isWifiDebuggingEnabled(): Boolean {
        return try {
            Settings.Global.getInt(contentResolver, "adb_wifi_enabled", 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun setWifiDebuggingEnabled(enabled: Boolean) {
        try {
            Settings.Global.putInt(contentResolver, "adb_wifi_enabled", if (enabled) 1 else 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
