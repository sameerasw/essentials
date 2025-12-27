package com.sameerasw.essentials.services

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShizukuUtils

@RequiresApi(Build.VERSION_CODES.N)
class SoundModeTileService : TileService() {

    private var latestAudioStateUpdate: Int? = null

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                updateSoundTile()
            }
        }
    }

    private fun updateSoundTile() {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        if (latestAudioStateUpdate == audioManager.ringerMode) {
            latestAudioStateUpdate = null
            return
        }

        if (qsTile == null) {
            return
        }

        // Check if permission is granted
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val hasPermission = notificationManager.isNotificationPolicyAccessGranted

        if (!hasPermission) {
            qsTile.state = Tile.STATE_UNAVAILABLE
            qsTile.label = "Sound"
            qsTile.updateTile()
            return
        }

        when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                qsTile.label = "Sound"
                qsTile.icon = Icon.createWithResource(this, R.drawable.rounded_volume_up_24)
                qsTile.state = Tile.STATE_INACTIVE
            }

            AudioManager.RINGER_MODE_VIBRATE -> {
                qsTile.label = "Vibrate"
                qsTile.icon = Icon.createWithResource(this, R.drawable.rounded_mobile_vibrate_24)
                qsTile.state = Tile.STATE_ACTIVE
            }

            AudioManager.RINGER_MODE_SILENT -> {
                qsTile.label = "Silent"
                qsTile.icon = Icon.createWithResource(this, R.drawable.rounded_volume_off_24)
                qsTile.state = Tile.STATE_ACTIVE
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = "Mode"
        }

        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return
        }

        val prefs = getSharedPreferences("essentials_prefs", MODE_PRIVATE)
        val defaultOrder = listOf("Sound", "Vibrate", "Silent")
        val orderString = prefs.getString("sound_mode_order", defaultOrder.joinToString(",")) ?: defaultOrder.joinToString(",")
        val order = orderString.split(",")

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        val currentMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> "Sound"
            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
            AudioManager.RINGER_MODE_SILENT -> "Silent"
            else -> "Sound"
        }

        val currentIndex = order.indexOf(currentMode)
        val nextIndex = (currentIndex + 1) % order.size
        val nextMode = order[nextIndex]

        val nextRingerMode = when (nextMode) {
            "Sound" -> AudioManager.RINGER_MODE_NORMAL
            "Vibrate" -> AudioManager.RINGER_MODE_VIBRATE
            "Silent" -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        audioManager.ringerMode = nextRingerMode
        HapticUtil.performHapticForService(this)

        latestAudioStateUpdate = nextRingerMode

        updateSoundTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateSoundTile()
    }

    override fun onCreate() {
        super.onCreate()

        this.registerReceiver(
            broadcastReceiver,
            IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            this.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateSoundTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()

        if (qsTile == null) {
            return
        }

        qsTile.state = Tile.STATE_UNAVAILABLE
        qsTile.updateTile()
    }
}
