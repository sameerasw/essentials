package com.sameerasw.essentials.services

import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R

class TapToWakeTileService : BaseTileService() {

    override fun getTileLabel(): String = "Tap to Wake"

    override fun getTileSubtitle(): String {
        return if (isTapToWakeEnabled()) "On" else "Off"
    }

    override fun hasFeaturePermission(): Boolean {
        return checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun getTileIcon(): Icon? {
        return if (isTapToWakeEnabled()) {
            Icon.createWithResource(this, R.drawable.rounded_touch_app_24)
        } else {
            Icon.createWithResource(this, R.drawable.rounded_do_not_touch_24)
        }
    }

    override fun getTileState(): Int {
        return if (isTapToWakeEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val newState = if (isTapToWakeEnabled()) 0 else 1
        Settings.Secure.putInt(contentResolver, "doze_tap_gesture", newState)
    }

    private fun isTapToWakeEnabled(): Boolean {
        return Settings.Secure.getInt(contentResolver, "doze_tap_gesture", 1) == 1
    }
}
