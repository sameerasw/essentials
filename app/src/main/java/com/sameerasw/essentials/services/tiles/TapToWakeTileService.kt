/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: TapToWakeTileService.kt
 * Description: Background service component for TapToWakeTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R

class TapToWakeTileService : BaseTileService() {
    override fun getTileLabel(): String = getString(R.string.tile_tap_to_wake)

    override fun getTileSubtitle(): String = getString(if (isTapToWakeEnabled()) R.string.on else R.string.off)

    override fun hasFeaturePermission(): Boolean =
        checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    override fun getTileIcon(): Icon? =
        if (isTapToWakeEnabled()) {
            Icon.createWithResource(this, R.drawable.rounded_touch_app_24)
        } else {
            Icon.createWithResource(this, R.drawable.rounded_do_not_touch_24)
        }

    override fun getTileState(): Int = if (isTapToWakeEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun onTileClick() {
        val newState = if (isTapToWakeEnabled()) 0 else 1
        Settings.Secure.putInt(contentResolver, "doze_tap_gesture", newState)
    }

    private fun isTapToWakeEnabled(): Boolean = Settings.Secure.getInt(contentResolver, "doze_tap_gesture", 1) == 1
}
