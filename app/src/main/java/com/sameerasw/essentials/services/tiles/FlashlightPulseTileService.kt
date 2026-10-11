/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: FlashlightPulseTileService.kt
 * Description: Background service component for FlashlightPulseTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.service.quicksettings.Tile
import androidx.core.content.edit

class FlashlightPulseTileService : BaseTileService() {
    override fun isDeviceSupported(): Boolean = !com.sameerasw.essentials.utils.DeviceUtils.isTorchRestrictedDevice()

    override fun onTileClick() {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("flashlight_pulse_enabled", false)
        prefs.edit {
            putBoolean("flashlight_pulse_enabled", !enabled)
        }
    }

    override fun getTileLabel(): String = "Flashlight Pulse"

    override fun getTileSubtitle(): String {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("flashlight_pulse_enabled", false)
        return if (enabled) "Enabled" else "Disabled"
    }

    override fun hasFeaturePermission(): Boolean {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        return prefs.getBoolean("edge_lighting_enabled", false)
    }

    override fun getTileState(): Int {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val enabled = prefs.getBoolean("flashlight_pulse_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
