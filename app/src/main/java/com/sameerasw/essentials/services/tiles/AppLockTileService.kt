/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: AppLockTileService.kt
 * Description: Background service component for AppLockTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.activities.TileAuthActivity
import com.sameerasw.essentials.utils.PermissionUtils

class AppLockTileService : BaseTileService() {
    override fun onTileClick() {
        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("app_lock_enabled", false)

        val intent =
            Intent(this, TileAuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("feature_pref_key", "app_lock_enabled")
                putExtra("auth_title", getString(R.string.app_lock_auth_title))
                putExtra(
                    "auth_subtitle",
                    getString(if (isEnabled) R.string.app_lock_disable_auth_subtitle else R.string.app_lock_enable_auth_subtitle),
                )
            }

        if (Build.VERSION.SDK_INT >= 34) {
            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }

    override fun getTileLabel(): String = getString(R.string.tile_app_lock)

    override fun getTileSubtitle(): String = getString(if (getTileState() == Tile.STATE_ACTIVE) R.string.label_enabled else R.string.label_disabled)

    override fun hasFeaturePermission(): Boolean {
        // Accessibility is required for monitoring app usage
        return PermissionUtils.isAccessibilityServiceEnabled(this)
    }

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_shield_lock_24)

    override fun getTileState(): Int {
        val enabled =
            getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                .getBoolean("app_lock_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
