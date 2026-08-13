/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SmartPixelsTileService.kt
 * Description: Quick Settings tile service component for Smart Pixels feature.
 */

package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.PermissionUtils

@RequiresApi(Build.VERSION_CODES.N)
class SmartPixelsTileService : BaseTileService() {

    private val prefs by lazy {
        getSharedPreferences(SettingsRepository.PREFS_NAME, MODE_PRIVATE)
    }

    override fun onTileClick() {
        val currentState = prefs.getBoolean(SettingsRepository.KEY_SMART_PIXELS_ENABLED, false)
        val newState = !currentState
        prefs.edit().putBoolean(SettingsRepository.KEY_SMART_PIXELS_ENABLED, newState).apply()

        updateTile()
    }

    override fun getTileLabel(): String = getString(R.string.feat_smart_pixels_title)

    override fun getTileSubtitle(): String = if (prefs.getBoolean(SettingsRepository.KEY_SMART_PIXELS_ENABLED, false)) getString(R.string.on) else getString(R.string.off)

    override fun hasFeaturePermission(): Boolean = PermissionUtils.isAccessibilityServiceEnabled(this)

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_grain_24)

    override fun getTileState(): Int = if (prefs.getBoolean(SettingsRepository.KEY_SMART_PIXELS_ENABLED, false)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
}
