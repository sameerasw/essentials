package com.sameerasw.essentials.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.BatteryRingDrawer
import com.sameerasw.essentials.utils.BluetoothBatteryUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class BatteryNotificationService : Service() {

    private lateinit var settingsRepository: SettingsRepository
    private val NOTIF_ID = 8822
    private val CHANNEL_ID = "battery_notification_channel"

    private val preferenceChangeListener = object : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED ||
                key == SettingsRepository.KEY_MAC_BATTERY_LEVEL ||
                key == SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING ||
                key == SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY ||
                key == SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES
            ) {
                updateNotification()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        createNotificationChannel()
        settingsRepository.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        
        startForeground(NOTIF_ID, buildBaseNotification(getString(R.string.feat_batteries_title), ""))
        
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
             startForeground(NOTIF_ID, buildBaseNotification(getString(R.string.feat_batteries_title), ""))
        }
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            settingsRepository.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.battery_notification_channel_name)
            val descriptionText = getString(R.string.battery_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val batteryItems = fetchBatteryData()
        
        val notification = if (batteryItems.isEmpty()) {
            buildBaseNotification(
                getString(R.string.feat_batteries_title),
                getString(R.string.battery_notification_no_devices)
            )
        } else {
            val bitmap = createCompositeBitmap(batteryItems)
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.rounded_battery_charging_60_24)
                .setLargeIcon(bitmap)
                .setStyle(NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?))
                .setContentTitle(getString(R.string.feat_batteries_title))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setSilent(true)
                .build()
        }

        startForeground(NOTIF_ID, notification)
    }

    private fun buildBaseNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.rounded_battery_charging_60_24)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun fetchBatteryData(): List<BatteryItemData> {
        val items = mutableListOf<BatteryItemData>()
        val maxDevices = settingsRepository.getBatteryWidgetMaxDevices()

        // Mac
        val isAirSyncEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)
        val macLevel = settingsRepository.getInt(SettingsRepository.KEY_MAC_BATTERY_LEVEL, -1)
        val isMacConnected = settingsRepository.getBoolean(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED)
        val macIsCharging = settingsRepository.getBoolean(SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING)

        if (isAirSyncEnabled && macLevel != -1 && isMacConnected) {
            val statusIcon = if (macIsCharging) R.drawable.rounded_flash_on_24
            else if (macLevel <= 15) R.drawable.rounded_battery_android_frame_alert_24
            else null
            items.add(BatteryItemData(macLevel, R.drawable.rounded_laptop_mac_24, "Mac", statusIcon))
        }

        // Bluetooth
        val isShowBluetoothEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES)
        val bluetoothJson = settingsRepository.getString(SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY)

        if (isShowBluetoothEnabled && !bluetoothJson.isNullOrEmpty() && bluetoothJson != "[]") {
            try {
                val type = object : TypeToken<List<BluetoothBatteryUtils.BluetoothDeviceBattery>>() {}.type
                val devices: List<BluetoothBatteryUtils.BluetoothDeviceBattery> = Gson().fromJson(bluetoothJson, type) ?: emptyList()
                devices.forEach { device ->
                    val iconRes = when {
                        device.name.contains("watch", true) || device.name.contains("gear", true) || device.name.contains("fit", true) -> R.drawable.rounded_watch_24
                        device.name.contains("bud", true) || device.name.contains("pod", true) || device.name.contains("head", true) || device.name.contains("audio", true) || device.name.contains("sound", true) -> R.drawable.rounded_headphones_24
                        else -> R.drawable.rounded_bluetooth_24
                    }
                    val statusIcon = if (device.level <= 15) R.drawable.rounded_battery_android_frame_alert_24 else null
                    items.add(BatteryItemData(device.level, iconRes, device.name, statusIcon))
                }
            } catch (_: Exception) {}
        }

        return items.take(maxDevices)
    }

    private fun createCompositeBitmap(items: List<BatteryItemData>): Bitmap {
        val itemSize = 256
        val spacing = 48
        val totalWidth = items.size * itemSize + (items.size - 1) * spacing
        val totalHeight = itemSize

        val composite = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(composite)

        val accentColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getColor(android.R.color.system_accent1_100)
        } else {
            Color.parseColor("#6200EE")
        }

        val onSurface = Color.WHITE
        val trackColor = ColorUtils.setAlphaComponent(onSurface, 40)
        val surfaceColor = Color.parseColor("#99000000") 

        items.forEachIndexed { index, item ->
            val ringColor = when {
                item.level <= 15 -> Color.parseColor("#F44336") // Red
                item.level <= 30 -> Color.parseColor("#FF9800") // Orange
                else -> accentColor
            }
            val itemBitmap = BatteryRingDrawer.drawBatteryWidget(
                this, item.level, ringColor, trackColor, onSurface, surfaceColor,
                ContextCompat.getDrawable(this, item.iconRes),
                item.statusIconRes?.let { ContextCompat.getDrawable(this, it) },
                itemSize, itemSize
            )
            canvas.drawBitmap(itemBitmap, (index * (itemSize + spacing)).toFloat(), 0f, null)
        }

        return composite
    }

    data class BatteryItemData(
        val level: Int,
        val iconRes: Int,
        val name: String,
        val statusIconRes: Int? = null
    )
}
