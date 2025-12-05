package com.sameerasw.essentials

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.quicksettings.Tile

class CaffeinateTileService : BaseTileService() {

    override fun onTileClick() {
        val isActive = qsTile.state == Tile.STATE_ACTIVE
        if (isActive) {
            // Turn off: stop the wake lock service
            stopService(Intent(this, CaffeinateWakeLockService::class.java))
        } else {
            // Turn on: start the wake lock service
            startService(Intent(this, CaffeinateWakeLockService::class.java))
        }
    }

    override fun getTileLabel(): String = "Caffeinate"

    override fun getTileSubtitle(): String {
        return if (qsTile.state == Tile.STATE_ACTIVE) {
            "Kept awake"
        } else {
            // Show current timeout in seconds
            val timeout = getScreenOffTimeout()
            if (timeout == -1L) "Never" else "${timeout / 1000}s"
        }
    }

    override fun hasFeaturePermission(): Boolean {
        // Wake lock doesn't require permissions
        return true
    }

    override fun getTileState(): Int {
        return if (isWakeLockServiceRunning()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    private fun isWakeLockServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (CaffeinateWakeLockService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun getScreenOffTimeout(): Long {
        return try {
            Settings.System.getLong(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        } catch (_: Exception) {
            60000L
        }
    }
}
