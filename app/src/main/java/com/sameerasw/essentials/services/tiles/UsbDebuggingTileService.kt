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
import android.util.Log
import android.widget.Toast
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
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
            if (!enabled) {
                toggleShizuku(false)
            }
            Settings.Global.putInt(
                contentResolver,
                Settings.Global.ADB_ENABLED,
                if (enabled) 1 else 0
            )
            if (enabled) {
                toggleShizuku(true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getShizukuToken(): String {
        return SettingsRepository(this).getShizukuAuthToken()
    }

    private fun toggleShizuku(enabled: Boolean) {
        val token = getShizukuToken()
        val action =
            if (enabled) "moe.shizuku.privileged.api.START" else "moe.shizuku.privileged.api.STOP"

        if (token.isEmpty()) {
            Toast.makeText(
                this,
                this.getString(R.string.toast_enter_shizuku_token),
                Toast.LENGTH_LONG
            ).show()
        } else {
            try {
                val shizukuIntent = Intent(action).apply {
                    `package` = "moe.shizuku.privileged.api"
                    putExtra("auth", token)
                    addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                }
                this.sendBroadcast(shizukuIntent)
            } catch (e: Exception) {
                Log.e("ShizukuActionReceiver", "Failed to restart Shizuku", e)
            }
        }
    }
}
