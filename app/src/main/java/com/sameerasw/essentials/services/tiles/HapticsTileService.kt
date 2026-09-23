package com.sameerasw.essentials.services.tiles

import android.app.PendingIntent
import android.content.Intent
import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.service.quicksettings.Tile
import android.util.Log
import android.widget.Toast
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

class HapticsTileService : BaseTileService() {
    private val vibrationSetting = "vibrate_on"
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        contentResolver.registerContentObserver(Settings.System.getUriFor(vibrationSetting), false, observer)
        contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED), false, observer)
    }

    override fun onStopListening() {
        contentResolver.unregisterContentObserver(observer)
        super.onStopListening()
    }

    override fun onClick() {
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        super.onClick()
    }

    override fun getTileLabel() = getString(R.string.tile_haptics)
    override fun getTileSubtitle() = getString(if (isEnabled()) R.string.tile_active else R.string.tile_inactive)
    override fun hasFeaturePermission() = PermissionUtils.canWriteSystemSettings(this)
    override fun getTileIcon(): Icon = Icon.createWithResource(this, if (isEnabled()) R.drawable.rounded_mobile_vibrate_24 else R.drawable.rounded_mobile_off_24)
    override fun getTileState() = if (isEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun onTileClick() {
        if (!hasFeaturePermission()) return
        val resolver = contentResolver
        try {
            val originalVibration = if (isEnabled()) 1 else 0
            val originalFeedback = Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 0)
            val value = 1 - originalVibration
            try {
                check(Settings.System.putInt(resolver, vibrationSetting, value)) { "Could not write the vibration setting" }
                check(Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, value)) { "Could not write the touch feedback setting" }
            } catch (e: Exception) {
                runCatching { Settings.System.putInt(resolver, vibrationSetting, originalVibration) }
                runCatching { Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, originalFeedback) }
                throw e
            }
            if (value == 1) {
                runCatching {
                    getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }.onFailure { Log.w("HapticsTile", "Could not vibrate for confirmation", it) }
            }
        } catch (e: Exception) {
            Log.e("HapticsTile", "Could not change haptics", e)
            Toast.makeText(this, R.string.tile_haptics_write_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun isEnabled() = Settings.System.getInt(contentResolver, vibrationSetting, 0) == 1
}
