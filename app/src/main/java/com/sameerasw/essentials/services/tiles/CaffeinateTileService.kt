package com.sameerasw.essentials.services.tiles

import android.app.ActivityManager
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.controller.CaffeinateController
import com.sameerasw.essentials.services.CaffeinateWakeLockService

class CaffeinateTileService : BaseTileService() {
 
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateTile()
            if (CaffeinateController.isStarting.value) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        if (CaffeinateController.isStarting.value) {
            handler.removeCallbacks(refreshRunnable)
            handler.post(refreshRunnable)
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onTileClick() {
        if (CaffeinateController.isStarting.value) {
            CaffeinateController.cycleTimeout(this)
        } else {
            CaffeinateController.toggle(this)
        }
        
        // Start refreshing if needed
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun getTileLabel(): String = "Caffeinate"

    override fun getTileSubtitle(): String {
        return if (CaffeinateController.isStarting.value) {
            val timeoutStr = when (CaffeinateController.selectedTimeout.value) {
                -1 -> "∞"
                60 -> "1h"
                else -> "${CaffeinateController.selectedTimeout.value}m"
            }
            if (CaffeinateController.isActive.value) {
                "$timeoutStr"
            } else {
                getString(R.string.caffeinate_starting_in, CaffeinateController.startingTimeLeft.value) + " ($timeoutStr)"
            }
        } else if (CaffeinateController.isActive.value) {
            getString(R.string.caffeinate_active)
        } else {
            val timeout = getScreenOffTimeout()
            if (timeout == -1L) "Never" else "${timeout / 1000}s"
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return true
    }

    override fun getTileState(): Int {
        return if (CaffeinateController.isActive.value || CaffeinateController.isStarting.value) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
    }

    private fun isWakeLockServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
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
