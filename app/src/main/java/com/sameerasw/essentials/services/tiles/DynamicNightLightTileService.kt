/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: DynamicNightLightTileService.kt
 * Description: Background service component for DynamicNightLightTileService.kt.
 */

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

    override fun getTileLabel(): String = getString(R.string.tile_dynamic_night_light)

    override fun getTileSubtitle(): String = getString(if (getTileState() == Tile.STATE_ACTIVE) R.string.label_enabled else R.string.label_disabled)

    override fun hasFeaturePermission(): Boolean {
        // Accessibility is required to monitor apps
        return PermissionUtils.isAccessibilityServiceEnabled(this) &&
            PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_nightlight_24)

    override fun getTileState(): Int {
        val enabled =
            getSharedPreferences("essentials_prefs", MODE_PRIVATE)
                .getBoolean("dynamic_night_light_enabled", false)
        return if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
