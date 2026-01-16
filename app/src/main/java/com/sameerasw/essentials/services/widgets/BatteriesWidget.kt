package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.os.BatteryManager
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.height
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.glance.Image
import androidx.glance.ImageProvider
import com.sameerasw.essentials.R

class BatteriesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                // 1. Fetch Data
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val androidLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                val prefs = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()

                // Keys
                val KEY_AIRSYNC_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)
                val KEY_MAC_LEVEL = androidx.datastore.preferences.core.intPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_MAC_BATTERY_LEVEL)
                val KEY_MAC_CONNECTED = androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED)
                val KEY_SHOW_BLUETOOTH = androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES)
                val KEY_BLUETOOTH_BATTERY = androidx.datastore.preferences.core.stringPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY)

                // State
                val isAirSyncEnabled = prefs[KEY_AIRSYNC_ENABLED] ?: false
                val macLevel = prefs[KEY_MAC_LEVEL] ?: -1
                val isMacConnected = prefs[KEY_MAC_CONNECTED] ?: false
                val isShowBluetoothEnabled = prefs[KEY_SHOW_BLUETOOTH] ?: false
                val bluetoothJson = prefs[KEY_BLUETOOTH_BATTERY]

                val showMac = isAirSyncEnabled && macLevel != -1 && isMacConnected
                val hasBluetooth = isShowBluetoothEnabled && !bluetoothJson.isNullOrEmpty() && bluetoothJson != "[]"

                // 2. Prepare List of Items to Display
                val batteryItems = mutableListOf<BatteryItemData>()

                // Android Item
                batteryItems.add(
                    BatteryItemData(
                        level = androidLevel,
                        iconRes = R.drawable.rounded_mobile_24,
                        name = "Android"
                    )
                )

                // Mac Item
                if (showMac) {
                    batteryItems.add(
                        BatteryItemData(
                            level = macLevel,
                            iconRes = R.drawable.rounded_laptop_mac_24,
                            name = "Mac"
                        )
                    )
                }

                // Bluetooth Items
                if (hasBluetooth) {
                    try {
                        val type = object : com.google.gson.reflect.TypeToken<List<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery>>() {}.type
                        val devices: List<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery> = com.google.gson.Gson().fromJson(bluetoothJson, type) ?: emptyList()

                        devices.forEach { device ->
                            val iconRes = when {
                                device.name.contains("watch", true) -> R.drawable.rounded_watch_24
                                device.name.contains("bud", true) ||
                                device.name.contains("pod", true) ||
                                device.name.contains("head", true) -> R.drawable.rounded_headphones_24
                                else -> R.drawable.rounded_bluetooth_24
                            }
                            batteryItems.add(
                                BatteryItemData(
                                    level = device.level,
                                    iconRes = iconRes,
                                    name = device.name
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // ignore parsing error
                    }
                }

                // 3. Render
                // Get dynamic theme colors from GlanceTheme
                val basePrimary = GlanceTheme.colors.primary.getColor(context).toArgb()
                val baseError = GlanceTheme.colors.error.getColor(context).toArgb()
                val onSurface = GlanceTheme.colors.onSurface.getColor(context).toArgb()
                val surfaceColor = GlanceTheme.colors.surface.getColor(context).toArgb()

                val colors = ThemeColors(
                    primary = basePrimary,
                    error = baseError,
                    warning = android.graphics.Color.parseColor("#FFC107"),
                    track = ColorUtils.setAlphaComponent(onSurface, 30),
                    surface = surfaceColor,
                    iconTint = onSurface
                )

                if (batteryItems.size > 1) {
                    // Multi-item layout
                    Row(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.surface)
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        batteryItems.forEachIndexed { index, item ->
                            BatteryItemBox(context, item, colors, modifier = GlanceModifier.defaultWeight().fillMaxHeight())

                            if (index < batteryItems.size - 1) {
                                Spacer(modifier = GlanceModifier.width(8.dp))
                            }
                        }
                    }
                } else {
                    // Single item layout (Big)
                    val item = batteryItems.firstOrNull() ?: BatteryItemData(androidLevel, R.drawable.rounded_mobile_24, "Android")

                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(GlanceTheme.colors.surface)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BatteryItemBox(context, item, colors, size = 512, modifier = GlanceModifier.fillMaxSize())
                    }
                }
            }
        }
    }

    data class BatteryItemData(val level: Int, val iconRes: Int, val name: String)

    data class ThemeColors(
        val primary: Int,
        val error: Int,
        val warning: Int,
        val track: Int,
        val surface: Int,
        val iconTint: Int
    )

    @androidx.compose.runtime.Composable
    private fun BatteryItemBox(
        context: Context,
        item: BatteryItemData,
        colors: ThemeColors,
        size: Int = 300,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val ringColor = when {
            item.level <= 10 -> colors.error
            item.level < 20 -> colors.warning
            else -> colors.primary
        }

        val icon = ContextCompat.getDrawable(context, item.iconRes)
        val bitmap = com.sameerasw.essentials.utils.BatteryRingDrawer.drawBatteryWidget(
            context, item.level, ringColor, colors.track, colors.iconTint, colors.surface, icon, size, size
        )

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "${item.name}: ${item.level}%",
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

