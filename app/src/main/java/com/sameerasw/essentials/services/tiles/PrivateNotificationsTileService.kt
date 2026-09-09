/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: PrivateNotificationsTileService.kt
 * Description: Background service component for PrivateNotificationsTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R

class PrivateNotificationsTileService : BaseTileService() {
    override val isSensitiveTile: Boolean = true

    override fun getTileLabel(): String = "Sensitive Content"

    override fun getTileSubtitle(): String = if (arePrivateNotificationsAllowed()) "Shown on lock screen" else "Hidden on lock screen"

    override fun hasFeaturePermission(): Boolean =
        checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    override fun getTileIcon(): Icon {
        val iconRes =
            if (arePrivateNotificationsAllowed()) R.drawable.rounded_notifications_unread_24 else R.drawable.rounded_notifications_off_24
        return Icon.createWithResource(this, iconRes)
    }

    override fun getTileState(): Int = if (arePrivateNotificationsAllowed()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun onTileClick() {
        val newState = if (arePrivateNotificationsAllowed()) 0 else 1
        Settings.Secure.putInt(contentResolver, "lock_screen_allow_private_notifications", newState)
    }

    private fun arePrivateNotificationsAllowed(): Boolean {
        // 1 = allowed, 0 = not allowed
        return Settings.Secure.getInt(
            contentResolver,
            "lock_screen_allow_private_notifications",
            1,
        ) == 1
    }
}
