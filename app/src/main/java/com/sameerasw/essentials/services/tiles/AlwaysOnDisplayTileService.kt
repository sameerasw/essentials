/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: AlwaysOnDisplayTileService.kt
 * Description: Background service component for AlwaysOnDisplayTileService.kt.
 */

package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ShellUtils

@RequiresApi(Build.VERSION_CODES.N)
class AlwaysOnDisplayTileService : BaseTileService() {
    override fun getTileLabel(): String = getString(R.string.feat_always_on_display_title)

    override fun getTileSubtitle(): String =
        when {
            isGlanceEnabled() -> getString(R.string.tile_dynamic)
            isAodEnabled() -> getString(R.string.on)
            else -> getString(R.string.off)
        }

    override fun hasFeaturePermission(): Boolean =
        com.sameerasw.essentials.utils.PermissionUtils
            .canWriteSecureSettings(this) ||
            (ShellUtils.isAvailable(this) && ShellUtils.hasPermission(this))

    override fun getTileIcon(): Icon? =
        when {
            isGlanceEnabled() -> Icon.createWithResource(this, R.drawable.outline_mobile_chat_24)
            isAodEnabled() -> Icon.createWithResource(this, R.drawable.rounded_mobile_text_2_24)
            else -> Icon.createWithResource(this, R.drawable.rounded_mobile_off_24)
        }

    override fun getTileState(): Int = if (isAodEnabled() || isGlanceEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun onTileClick() {
        when {
            isGlanceEnabled() -> {
                // Dynamic -> On
                setGlanceEnabled(false)
                setAodEnabled(true)
            }

            isAodEnabled() -> {
                // On -> Off
                setAodEnabled(false)
                setGlanceEnabled(false)
            }

            else -> {
                // Off -> Dynamic
                setGlanceEnabled(true)
                setAodEnabled(false)
            }
        }
    }

    private fun isAodEnabled(): Boolean = getSecureInt("doze_always_on", 0) == 1

    private fun setAodEnabled(enabled: Boolean) {
        putSecureInt("doze_always_on", if (enabled) 1 else 0)
    }

    private fun isGlanceEnabled(): Boolean =
        getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .getBoolean(SettingsRepository.KEY_NOTIFICATION_GLANCE_ENABLED, false)

    private fun setGlanceEnabled(enabled: Boolean) {
        getSharedPreferences("essentials_prefs", MODE_PRIVATE).edit().apply {
            putBoolean(SettingsRepository.KEY_NOTIFICATION_GLANCE_ENABLED, enabled)
            apply()
        }
    }
}
