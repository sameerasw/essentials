package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import androidx.core.content.edit
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

class DynamicNightLightTileService : BaseTileService() {

    override fun onTileClick() {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("dynamic_night_light_enabled", false)
        prefs.edit { putBoolean("dynamic_night_light_enabled", !isEnabled) }
    }

    override fun getTileLabel(): String = "Dynamic Night Light"

    override fun getTileSubtitle(): String {
        return if (qsTile.state == Tile.STATE_ACTIVE) "Enabled" else "Disabled"
    }

    override fun hasFeaturePermission(): Boolean {
        // Accessibility is required to monitor apps
        return PermissionUtils.isAccessibilityServiceEnabled(this) &&
                PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon =
        Icon.createWithResource(this, R.drawable.rounded_nightlight_24)

    override fun getTileState(): Int {
        val enabled = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .getBoolean("dynamic_night_light_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
