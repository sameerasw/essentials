package com.sameerasw.essentials.services.tiles

import android.database.ContentObserver
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.service.quicksettings.Tile
import android.util.Log
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.ShellUtils

class HapticsTileService : BaseTileService() {
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        contentResolver.registerContentObserver(Settings.System.getUriFor(HapticsSettings.VIBRATE_ON), false, observer)
        contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED), false, observer)
    }

    override fun onStopListening() {
        contentResolver.unregisterContentObserver(observer)
        super.onStopListening()
    }

    override fun onClick() {
        if (!hasFeaturePermission()) {
            openTilesSettings()
            return
        }
        super.onClick()
    }

    override fun getTileLabel() = getString(R.string.tile_haptics)
    override fun getTileSubtitle() = getString(if (HapticsSettings.isEnabled(this)) R.string.tile_active else R.string.tile_inactive)
    override fun hasFeaturePermission() = PermissionUtils.canWriteSystemSettings(this)
    override fun getTileIcon(): Icon =
        Icon.createWithResource(this, if (HapticsSettings.isEnabled(this)) R.drawable.rounded_mobile_vibrate_24 else R.drawable.rounded_mobile_off_24)
    override fun getTileState() = if (HapticsSettings.isEnabled(this)) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE

    override fun onTileClick() {
        if (!hasFeaturePermission()) return
        val value = if (HapticsSettings.isEnabled(this)) 0 else 1
        if (!HapticsSettings.set(this, value)) {
            Toast.makeText(this, R.string.tile_haptics_write_failed, Toast.LENGTH_SHORT).show()
            return
        }
        if (value == 1) {
            runCatching {
                getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }.onFailure { Log.w("HapticsTile", "Could not vibrate for confirmation", it) }
        }
    }
}

internal object HapticsSettings {
    const val VIBRATE_ON = "vibrate_on"

    fun isEnabled(context: android.content.Context): Boolean {
        val resolver = context.contentResolver
        val vibrate = Settings.System.getInt(resolver, VIBRATE_ON, -1)
        return if (vibrate == -1) Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1 else vibrate == 1
    }

    fun set(context: android.content.Context, value: Int): Boolean {
        val resolver = context.contentResolver
        val originalFeedback = Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1)
        if (!runCatching { Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, value) }.getOrDefault(false)) return false
        if (writeVibrateOn(context, value)) return true
        runCatching { Settings.System.putInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, originalFeedback) }
        return false
    }

    private fun writeVibrateOn(context: android.content.Context, value: Int): Boolean {
        val direct = runCatching { Settings.System.putInt(context.contentResolver, VIBRATE_ON, value) }
            .onFailure { Log.w("HapticsTile", "Direct vibrate_on write rejected", it) }
            .getOrDefault(false)
        if (direct) return true
        if (!ShellUtils.hasPermission(context)) return false
        ShellUtils.runCommandWithOutput(context, "settings put system $VIBRATE_ON $value", notifyOnError = false)
        return Settings.System.getInt(context.contentResolver, VIBRATE_ON, -1) == value
    }
}
