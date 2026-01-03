package com.sameerasw.essentials.services

import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils
import androidx.core.content.edit

class ScreenLockedSecurityTileService : BaseTileService() {

    override fun onTileClick() {
        val prefs = getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("screen_locked_security_enabled", false)
        prefs.edit { putBoolean("screen_locked_security_enabled", !isEnabled) }
    }

    override fun getTileLabel(): String = "Locked Security"

    override fun getTileSubtitle(): String {
        return if (qsTile.state == Tile.STATE_ACTIVE) "Enabled" else "Disabled"
    }

    override fun hasFeaturePermission(): Boolean {
        // Accessibility, Device Admin and Write Secure Settings are required
        return PermissionUtils.isAccessibilityServiceEnabled(this) &&
               PermissionUtils.isDeviceAdminActive(this) &&
               PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_security_24)

    override fun getTileState(): Int {
        val enabled = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .getBoolean("screen_locked_security_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
