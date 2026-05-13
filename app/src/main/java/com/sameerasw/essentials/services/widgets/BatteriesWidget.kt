package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.os.BatteryManager
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import com.sameerasw.essentials.R

class BatteriesWidget : GlanceAppWidget() {
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                val size = androidx.glance.LocalSize.current
                val width = size.width
                val height = size.height

                // 1. Fetch Data
                val batteryManager =
                    context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val androidLevel =
                    batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

                val prefs =
                    androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()

                // Keys
                val KEY_AIRSYNC_ENABLED =
                    androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)
                val KEY_MAC_LEVEL =
                    androidx.datastore.preferences.core.intPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_MAC_BATTERY_LEVEL)
                val KEY_MAC_CONNECTED =
                    androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED)
                val KEY_SHOW_BLUETOOTH =
                    androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES)
                val KEY_BLUETOOTH_BATTERY =
                    androidx.datastore.preferences.core.stringPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY)

                // State
                val isAirSyncEnabled = prefs[KEY_AIRSYNC_ENABLED] ?: false
                val macLevel = prefs[KEY_MAC_LEVEL] ?: -1
                val isMacConnected = prefs[KEY_MAC_CONNECTED] ?: false
                val isShowBluetoothEnabled = prefs[KEY_SHOW_BLUETOOTH] ?: false
                val bluetoothJson = prefs[KEY_BLUETOOTH_BATTERY]

                // Add key for Mac Charging
                val KEY_MAC_IS_CHARGING =
                    androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING)
                val macIsCharging = prefs[KEY_MAC_IS_CHARGING] ?: false

                val KEY_MAX_DEVICES =
                    androidx.datastore.preferences.core.intPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_BATTERY_WIDGET_MAX_DEVICES)
                val maxDevices = prefs[KEY_MAX_DEVICES] ?: 8

                val KEY_BACKGROUND_ENABLED =
                    androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_BATTERY_WIDGET_BACKGROUND_ENABLED)
                val isBackgroundEnabled = prefs[KEY_BACKGROUND_ENABLED] ?: true

                // Force recomposition when theme changes
                val THEME_UPDATE_KEY =
                    androidx.datastore.preferences.core.longPreferencesKey("theme_update_time")
                prefs[THEME_UPDATE_KEY]

                val showMac = isAirSyncEnabled && macLevel != -1 && isMacConnected
                val hasBluetooth =
                    isShowBluetoothEnabled && !bluetoothJson.isNullOrEmpty() && bluetoothJson != "[]"

                // 2. Prepare List of Items to Display
                val batteryItems = mutableListOf<BatteryItemData>()

                // Android Item
                val isAndroidCharging =
                    (batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING)

                val androidStatusIcon = when {
                    isAndroidCharging -> R.drawable.rounded_flash_on_24
                    androidLevel <= 15 -> R.drawable.rounded_battery_android_frame_alert_24
                    else -> null
                }

                batteryItems.add(
                    BatteryItemData(
                        level = androidLevel,
                        iconRes = R.drawable.rounded_mobile_24,
                        name = "Android",
                        statusIconRes = androidStatusIcon
                    )
                )

                // Mac Item
                if (showMac) {
                    val macStatusIcon = when {
                        macIsCharging -> R.drawable.rounded_flash_on_24
                        macLevel <= 15 -> R.drawable.rounded_battery_android_frame_alert_24
                        else -> null
                    }
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
                        val gson = com.google.gson.Gson()
                        val devices: List<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery> =
                            gson.fromJson(
                                bluetoothJson,
                                Array<com.sameerasw.essentials.utils.BluetoothBatteryUtils.BluetoothDeviceBattery>::class.java
                            ).toList()

                        devices.forEach { device ->
                            val iconRes = when {
                                device.name.contains("watch", true) -> R.drawable.rounded_watch_24
                                device.name.contains("bud", true) ||
                                        device.name.contains("pod", true) ||
                                        device.name.contains("momentum", true) ||
                                        device.name.contains("head", true) -> R.drawable.rounded_headphones_24
                                else -> R.drawable.rounded_bluetooth_24
                            }

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
                    } catch (_: Exception) {}
                }

                val displayedItems = batteryItems.take(maxDevices)

                // 3. Render
                val context = androidx.glance.LocalContext.current
                val systemConfig = android.content.res.Resources.getSystem().configuration
                val configContext = context.createConfigurationContext(
                    android.content.res.Configuration(context.resources.configuration).apply {
                        uiMode = systemConfig.uiMode
                    }
                )

                val colors = ThemeColors(
                    primary = GlanceTheme.colors.primary.getColor(configContext).toArgb(),
                    error = GlanceTheme.colors.error.getColor(configContext).toArgb(),
                    warning = "#FFC107".toColorInt(),
                    track = ColorUtils.setAlphaComponent(GlanceTheme.colors.onSurface.getColor(configContext).toArgb(), 30),
                    surface = GlanceTheme.colors.widgetBackground.getColor(configContext).toArgb(),
                    iconTint = GlanceTheme.colors.onSurface.getColor(configContext).toArgb()
                )

                val backgroundModifier = if (isBackgroundEnabled) {
                    GlanceModifier.background(GlanceTheme.colors.widgetBackground)
                } else {
                    GlanceModifier.background(android.graphics.Color.TRANSPARENT)
                }

                val isSingleItem = displayedItems.size <= 1
                val effectivePadding = if (width < 100.dp || height < 100.dp) 4.dp else 8.dp
                val outerPadding = if (isSingleItem && width > 120.dp) 16.dp else effectivePadding
                val spacing = 8.dp

                // Dynamic Grid Calculation
                val itemMinWidth = if (isSingleItem) 120.dp else 72.dp
                val columns = (width / itemMinWidth).toInt().coerceIn(1, displayedItems.size.coerceAtLeast(1))
                val rows = displayedItems.chunked(columns)
                
                val availableWidth = (width - (outerPadding * 2) - (spacing * (columns - 1))).coerceAtLeast(1.dp)
                val availableHeight = (height - (outerPadding * 2) - (rows.size.let { if (it > 1) (it - 1) * spacing.value.dp else 0.dp })).coerceAtLeast(1.dp)
                
                val itemWidth = availableWidth / columns
                val rowHeight = availableHeight / rows.size.coerceAtLeast(1)
                val boxSize = if (itemWidth < rowHeight) itemWidth else rowHeight

                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .then(backgroundModifier)
                        .padding(outerPadding),
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
                                Box(
                                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BatteryItemBox(
                                        context = configContext,
                                        item = item,
                                        colors = colors,
                                        itemSize = boxSize,
                                        sizePx = itemResolution,
                                        modifier = GlanceModifier.size(boxSize)
                                    )
                                }
                                if (colIndex < rowItems.size - 1 || rowItems.size < columns) {
                                    Spacer(modifier = GlanceModifier.width(spacing))
                                }
                            }
                            // Filler for consistent sizing
                            if (rowItems.size < columns) {
                                repeat(columns - rowItems.size) { i ->
                                    Spacer(modifier = GlanceModifier.defaultWeight().fillMaxHeight())
                                    if (i < (columns - rowItems.size - 1)) {
                                        Spacer(modifier = GlanceModifier.width(spacing))
                                    }
                                }
                            }
                        }
                        if (rowIndex < rows.size - 1) {
                            Spacer(modifier = GlanceModifier.height(spacing))
                        }
                    }
                }
            }
        }
    }

    data class BatteryItemData(
        val level: Int,
        val iconRes: Int,
        val name: String,
        val statusIconRes: Int? = null
    )

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
        itemSize: androidx.compose.ui.unit.Dp,
        sizePx: Int = 340,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val ringColor = when {
            item.level <= 10 -> colors.error
            item.level < 20 -> colors.warning
            else -> colors.primary
        }

        val padding = if (itemSize > 100.dp) 12.dp else 8.dp
        val iconPadding = if (itemSize > 100.dp) 32.dp else 24.dp

        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            // 1. Background Circle
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(androidx.compose.ui.graphics.Color(colors.surface))
                    .cornerRadius(100.dp)
            ) {}

            // 2. Battery Ring (Arcs)
            val ringBitmap = com.sameerasw.essentials.utils.BatteryRingDrawer.drawBatteryRing(
                context,
                item.level,
                ringColor,
                colors.track,
                item.statusIconRes != null,
                sizePx,
                sizePx
            )
            Image(
                provider = ImageProvider(ringBitmap),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize()
            )

            // 3. Center Device Icon
            Image(
                provider = ImageProvider(item.iconRes),
                contentDescription = null,
                colorFilter = androidx.glance.ColorFilter.tint(
                    androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(colors.iconTint))
                ),
                modifier = GlanceModifier.fillMaxSize().padding(iconPadding)
            )

            // 4. Status Indicator Bubble (at the top)
            if (item.statusIconRes != null) {
                val bubbleSize = if (itemSize > 100.dp) 32.dp else 24.dp
                val bubbleIconPadding = if (itemSize > 100.dp) 6.dp else 4.dp
                
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(bubbleSize)
                            .background(androidx.compose.ui.graphics.Color(ringColor))
                            .cornerRadius(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(item.statusIconRes),
                            contentDescription = null,
                            colorFilter = androidx.glance.ColorFilter.tint(
                                androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color(colors.surface))
                            ),
                            modifier = GlanceModifier.fillMaxSize().padding(bubbleIconPadding)
                        )
                    }
                }
            }
        }
    }
}

