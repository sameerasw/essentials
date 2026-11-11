/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: NotificationLightingTileService.kt
 * Description: Background service component for NotificationLightingTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import androidx.core.content.edit
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

class NotificationLightingTileService : BaseTileService() {
    override fun onTileClick() {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("edge_lighting_enabled", false)
        prefs.edit { putBoolean("edge_lighting_enabled", !isEnabled) }
    }

    override fun getTileLabel(): String = getString(R.string.tile_notification_lighting)

    override fun getTileSubtitle(): String = getString(if (getTileState() == Tile.STATE_ACTIVE) R.string.label_enabled else R.string.label_disabled)

    override fun hasFeaturePermission(): Boolean {
        // Notification listener is required for notification lighting
        return PermissionUtils.hasNotificationListenerPermission(this) &&
            PermissionUtils.isNotificationLightingAccessibilityServiceEnabled(this) &&
            PermissionUtils.canDrawOverlays(this)
    }

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_blur_linear_24)

    override fun getTileState(): Int {
        val enabled =
            getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                .getBoolean("edge_lighting_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
