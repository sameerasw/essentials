package com.sameerasw.essentials.services

import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R

@RequiresApi(Build.VERSION_CODES.N)
class AlwaysOnDisplayTileService : BaseTileService() {

    override fun getTileLabel(): String = "AOD"

    override fun getTileSubtitle(): String {
        return if (isAodEnabled()) "On" else "Off"
    }

    override fun hasFeaturePermission(): Boolean {
        return checkCallingOrSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun getTileIcon(): Icon? {
        return if (isAodEnabled()) {
            Icon.createWithResource(this, R.drawable.rounded_mobile_text_2_24)
        } else {
            Icon.createWithResource(this, R.drawable.rounded_mobile_off_24)
        }
    }

    override fun getTileState(): Int {
        return if (isAodEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun onTileClick() {
        val newState = if (isAodEnabled()) 0 else 1
        Settings.Secure.putInt(contentResolver, "doze_always_on", newState)
    }

    private fun isAodEnabled(): Boolean {
        return Settings.Secure.getInt(contentResolver, "doze_always_on", 1) == 1
    }
}
