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
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

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
        return if (isUsbDebuggingEnabled()) getString(R.string.on) else getString(R.string.off)
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon {
        return Icon.createWithResource(this, R.drawable.rounded_adb_24)
    }

    override fun getTileState(): Int {
        return if (isUsbDebuggingEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        setUsbDebuggingEnabled(!isUsbDebuggingEnabled())
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
            Settings.Global.putInt(
                contentResolver,
                Settings.Global.ADB_ENABLED,
                if (enabled) 1 else 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
