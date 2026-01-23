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
import androidx.glance.layout.fillMaxWidth
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.glance.Image
import androidx.glance.ImageProvider
import com.sameerasw.essentials.R
import androidx.core.graphics.toColorInt

class BatteriesWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = androidx.glance.LocalSize.current
                val width = size.width
                val height = size.height

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
                
                // Add key for Mac Charging
                val KEY_MAC_IS_CHARGING = androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING)
                val macIsCharging = prefs[KEY_MAC_IS_CHARGING] ?: false

                val KEY_MAX_DEVICES = androidx.datastore.preferences.core.intPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_BATTERY_WIDGET_MAX_DEVICES)
                val maxDevices = prefs[KEY_MAX_DEVICES] ?: 8
                
                val KEY_BACKGROUND_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_BATTERY_WIDGET_BACKGROUND_ENABLED)
                val isBackgroundEnabled = prefs[KEY_BACKGROUND_ENABLED] ?: true

                // Force recomposition when theme changes
                val THEME_UPDATE_KEY = androidx.datastore.preferences.core.longPreferencesKey("theme_update_time")
                val themeLastUpdated = prefs[THEME_UPDATE_KEY]

                val showMac = isAirSyncEnabled && macLevel != -1 && isMacConnected
                val hasBluetooth = isShowBluetoothEnabled && !bluetoothJson.isNullOrEmpty() && bluetoothJson != "[]"

                // 2. Prepare List of Items to Display
                val batteryItems = mutableListOf<BatteryItemData>()

                // Android Item
                val isAndroidCharging = (batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING)

                val androidFinalStatusIcon = if (isAndroidCharging) R.drawable.rounded_flash_on_24
                                             else if (androidLevel <= 15) R.drawable.rounded_battery_android_frame_alert_24 
                                             else null

                batteryItems.add(
                    BatteryItemData(
                        level = androidLevel,
                        iconRes = R.drawable.rounded_mobile_24,
                        name = "Android",
                        statusIconRes = androidFinalStatusIcon
                    )
                )

                // Mac Item
                if (showMac) {
                    val macStatusIcon = if (macIsCharging) R.drawable.rounded_flash_on_24
                                        else if (macLevel <= 15) R.drawable.rounded_battery_android_frame_alert_24 
                                        else null
                    batteryItems.add(
                        BatteryItemData(
                            level = macLevel,
                            iconRes = R.drawable.rounded_laptop_mac_24,
                            name = "Mac",
                            statusIconRes = macStatusIcon
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
                            
                            // Bluetooth doesn't report charging usually, so just Low Battery check
                            val statusIcon = if (device.level <= 15) R.drawable.rounded_battery_android_frame_alert_24 else null
                            
                            batteryItems.add(
                                BatteryItemData(
                                    level = device.level,
                                    iconRes = iconRes,
                                    name = device.name,
                                    statusIconRes = statusIcon
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // ignore parsing error
                    }
                }

                val displayedItems = batteryItems.take(maxDevices)

                // 3. Render
                val context = androidx.glance.LocalContext.current
                val systemConfig = android.content.res.Resources.getSystem().configuration
                
                val forcedConfig = android.content.res.Configuration(context.resources.configuration)
                forcedConfig.uiMode = systemConfig.uiMode
                
                val configContext = context.createConfigurationContext(forcedConfig)
                
                val basePrimary = GlanceTheme.colors.primary.getColor(configContext).toArgb()
                val baseError = GlanceTheme.colors.error.getColor(configContext).toArgb()
                val onSurface = GlanceTheme.colors.onSurface.getColor(configContext).toArgb()
                val widgetBackgroundColor = GlanceTheme.colors.widgetBackground.getColor(configContext).toArgb()

                val colors = ThemeColors(
                    primary = basePrimary,
                    error = baseError,
                    warning = "#FFC107".toColorInt(),
                    track = ColorUtils.setAlphaComponent(onSurface, 30),
                    surface = widgetBackgroundColor,
                    iconTint = onSurface
                )

                val backgroundModifier = if (isBackgroundEnabled) {
                    GlanceModifier.background(GlanceTheme.colors.widgetBackground)
                } else {
                    GlanceModifier.background(android.graphics.Color.TRANSPARENT)
                }

                val effectivePadding = if (width < 100.dp || height < 100.dp) 4.dp else 8.dp
                val isSingleItem = displayedItems.size <= 1
                val items = displayedItems.ifEmpty { listOf(BatteryItemData(androidLevel, R.drawable.rounded_mobile_24, "Android")) }

                // Dynamic Grid Calculation
                val itemMinWidth = if (isSingleItem) 120.dp else 72.dp
                val columns = (width / itemMinWidth).toInt().coerceIn(1, items.size)
                val rows = items.chunked(columns)

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .then(backgroundModifier)
                        .padding(if (isSingleItem && width > 120.dp) 16.dp else effectivePadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rows.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEachIndexed { colIndex, item ->
                                val itemResolution = if (isSingleItem) 512 else 340
                                BatteryItemBox(
                                    configContext,
                                    item,
                                    colors,
                                    size = itemResolution,
                                    modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                                )
                                if (colIndex < rowItems.size - 1 || rowItems.size < columns) {
                                    Spacer(modifier = GlanceModifier.width(8.dp))
                                }
                            }
                            // Filler for consistent sizing
                            if (rowItems.size < columns) {
                                repeat(columns - rowItems.size) { i ->
                                    Spacer(modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                                    if (i < (columns - rowItems.size - 1)) {
                                        Spacer(modifier = GlanceModifier.width(8.dp))
                                    }
                                }
                            }
                        }
                        if (rowIndex < rows.size - 1) {
                            Spacer(modifier = GlanceModifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    data class BatteryItemData(val level: Int, val iconRes: Int, val name: String, val statusIconRes: Int? = null)

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
        val statusIcon = item.statusIconRes?.let { ContextCompat.getDrawable(context, it) }
        
        val bitmap = com.sameerasw.essentials.utils.BatteryRingDrawer.drawBatteryWidget(
            context, item.level, ringColor, colors.track, colors.iconTint, colors.surface, icon, statusIcon, size, size
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

