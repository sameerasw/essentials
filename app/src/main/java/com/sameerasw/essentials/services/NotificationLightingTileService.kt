package com.sameerasw.essentials.services

import android.content.Context
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils
import androidx.core.content.edit

class NotificationLightingTileService : BaseTileService() {

    override fun onTileClick() {
        val prefs = getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("edge_lighting_enabled", false)
        prefs.edit { putBoolean("edge_lighting_enabled", !isEnabled) }
    }

    override fun getTileLabel(): String = "Notification Lighting"

    override fun getTileSubtitle(): String {
        return if (qsTile.state == Tile.STATE_ACTIVE) "Enabled" else "Disabled"
    }

    override fun hasFeaturePermission(): Boolean {
        // Notification listener is required for notification lighting
        return PermissionUtils.hasNotificationListenerPermission(this) &&
               PermissionUtils.isNotificationLightingAccessibilityServiceEnabled(this) &&
               PermissionUtils.canDrawOverlays(this)
    }

    override fun getTileIcon(): Icon? = Icon.createWithResource(this, R.drawable.rounded_blur_linear_24)

    override fun getTileState(): Int {
        val enabled = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .getBoolean("edge_lighting_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
