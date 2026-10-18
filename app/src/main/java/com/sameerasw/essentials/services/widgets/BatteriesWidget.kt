/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: BatteriesWidget.kt
 * Description: Glance battery rings widget for the phone, Mac and Bluetooth devices.
 */

package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.google.gson.Gson
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.activities.BatteryDetailsActivity
import com.sameerasw.essentials.utils.BatteryRingDrawer
import com.sameerasw.essentials.utils.BluetoothBatteryUtils
import com.sameerasw.essentials.utils.BluetoothDeviceIcons
import com.sameerasw.essentials.utils.BluetoothDeviceOrder
import com.sameerasw.essentials.utils.battery.ChargeLimitUtil

class BatteriesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val settings = WidgetSettings.from(prefs)
                val items = buildItems(context, prefs, settings).take(settings.maxDevices)
                BatteryGrid(items, settings.backgroundEnabled, settings.centerItems, SettingsRepository(context).isBatteryShowPercentageEnabled())
            }
        }
    }

    private data class WidgetSettings(
        val maxDevices: Int,
        val backgroundEnabled: Boolean,
        val centerItems: Boolean,
    ) {
        companion object {
            fun from(prefs: Preferences) =
                WidgetSettings(
                    maxDevices = prefs[intPreferencesKey(SettingsRepository.KEY_BATTERY_WIDGET_MAX_DEVICES)] ?: 8,
                    backgroundEnabled = prefs[booleanPreferencesKey(SettingsRepository.KEY_BATTERY_WIDGET_BACKGROUND_ENABLED)] ?: true,
                    centerItems = prefs[booleanPreferencesKey(SettingsRepository.KEY_BATTERY_WIDGET_CENTER_ITEMS)] ?: false,
                )
        }
    }

    private class Item(
        val key: String,
        val level: Int,
        val iconRes: Int,
        val statusIconRes: Int? = null,
    )

    private fun buildItems(
        context: Context,
        prefs: Preferences,
        settings: WidgetSettings,
    ): List<Item> {
        val items = mutableListOf<Item>()

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val phoneLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val phoneCharging =
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == BatteryManager.BATTERY_STATUS_CHARGING
        items += Item(BluetoothDeviceOrder.PHONE, phoneLevel, R.drawable.rounded_mobile_24, statusIcon(phoneLevel, phoneCharging, ChargeLimitUtil.isShielded(context, phoneLevel, phoneCharging)))

        val macEnabled = prefs[booleanPreferencesKey(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)] ?: false
        val macConnected = prefs[booleanPreferencesKey(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED)] ?: false
        val macLevel = prefs[intPreferencesKey(SettingsRepository.KEY_MAC_BATTERY_LEVEL)] ?: -1
        val macCharging = prefs[booleanPreferencesKey(SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING)] ?: false
        if (macEnabled && macConnected && macLevel != -1) {
            items += Item(BluetoothDeviceOrder.MAC, macLevel, R.drawable.rounded_laptop_mac_24, statusIcon(macLevel, macCharging))
        }

        val bluetoothEnabled = prefs[booleanPreferencesKey(SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES)] ?: false
        val bluetoothJson = prefs[stringPreferencesKey(SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY)]
        if (bluetoothEnabled && !bluetoothJson.isNullOrEmpty() && bluetoothJson != "[]") {
            val devices =
                try {
                    Gson().fromJson(bluetoothJson, Array<BluetoothBatteryUtils.BluetoothDeviceBattery>::class.java).toList()
                } catch (_: Exception) {
                    emptyList()
                }
            devices.forEach { device ->
                if (device.hasParts) {
                    device.left?.let { items += Item(device.address, it, R.drawable.rounded_earbud_left_24, statusIcon(it, false)) }
                    device.right?.let { items += Item(device.address, it, R.drawable.rounded_earbud_right_24, statusIcon(it, false)) }
                    device.case?.let { items += Item(device.address, it, R.drawable.rounded_earbud_case_24, statusIcon(it, false)) }
                } else {
                    val icon = BluetoothDeviceIcons.resolve(context, device.address, device.name)
                    items += Item(device.address, device.level, icon, statusIcon(device.level, false))
                }
            }
        }
        return BluetoothDeviceOrder.apply(context, items) { it.key }
    }

    private fun statusIcon(
        level: Int,
        charging: Boolean,
        shielded: Boolean = false,
    ): Int? =
        when {
            shielded -> R.drawable.rounded_shield_filled_24
            charging -> R.drawable.rounded_bolt_filled_24
            level <= LOW_LEVEL -> R.drawable.rounded_battery_android_frame_alert_24
            else -> null
        }

    @Composable
    private fun BatteryGrid(
        items: List<Item>,
        backgroundEnabled: Boolean,
        centerItems: Boolean,
        showPercentage: Boolean,
    ) {
        val size = LocalSize.current
        val single = items.size <= 1
        val outerPadding = if (single && size.width > 120.dp) 16.dp else if (size.width < 100.dp || size.height < 100.dp) 4.dp else 8.dp
        val gap = 8.dp

        val minItemWidth = if (single) 120.dp else 72.dp
        val widthColumns = (size.width / minItemWidth).toInt().coerceIn(1, items.size.coerceAtLeast(1))
        val columns = maxOf(widthColumns, (items.size + MAX_ROWS - 1) / MAX_ROWS).coerceIn(1, MAX_COLUMNS)
        val rows = items.chunked(columns)

        val columnWidth = (size.width - outerPadding * 2) / columns
        val rowHeight = (size.height - outerPadding * 2) / rows.size.coerceAtLeast(1)
        val cell = minOf(columnWidth - gap, rowHeight - gap).coerceAtLeast(1.dp)

        val background =
            if (backgroundEnabled) {
                GlanceModifier.background(GlanceTheme.colors.widgetBackground)
            } else {
                GlanceModifier.background(android.graphics.Color.TRANSPARENT)
            }

        Column(
            modifier = GlanceModifier.fillMaxSize().then(background).padding(outerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            rows.forEach { row ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(vertical = gap / 2),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { item ->
                        Box(
                            modifier =
                                if (centerItems) {
                                    GlanceModifier.fillMaxHeight().padding(horizontal = gap / 2)
                                } else {
                                    GlanceModifier.defaultWeight().fillMaxHeight().padding(horizontal = gap / 2)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            BatteryRing(item, cell, showPercentage)
                        }
                    }
                    if (!centerItems) {
                        repeat(columns - row.size) {
                            Spacer(GlanceModifier.defaultWeight().fillMaxHeight())
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun BatteryRing(
        item: Item,
        cell: Dp,
        showPercentage: Boolean,
    ) {
        val context = LocalContext.current
        val density = context.resources.displayMetrics.density
        val bitmapPx = (cell.value * density).toInt().coerceIn(MIN_BITMAP_PX, MAX_BITMAP_PX)
        val hasStatus = item.statusIconRes != null || showPercentage
        val gap =
            if (showPercentage) {
                BatteryRingDrawer.percentGapDegrees(context, bitmapPx, "${item.level}", item.statusIconRes == R.drawable.rounded_bolt_filled_24 || item.statusIconRes == R.drawable.rounded_shield_filled_24)
            } else {
                null
            }

        val primary = GlanceTheme.colors.primary
        val error = GlanceTheme.colors.error
        val onSurface = GlanceTheme.colors.onSurface
        val surface = GlanceTheme.colors.widgetBackground
        val warning = ColorProvider(day = Color(0xFFFFC107), night = Color(0xFFFFD54F))
        val track = ColorProvider(day = Color.Black.copy(alpha = 0.1f), night = Color.Black.copy(alpha = 0.6f))

        val ringColor =
            when {
                item.level <= 10 -> error
                item.level < 20 -> warning
                else -> primary
            }
        val ringPadding = (cell * 0.1f).coerceIn(2.dp, 12.dp)
        val iconPadding = (cell * 0.28f).coerceIn(4.dp, 32.dp)

        Box(
            modifier = GlanceModifier.size(cell).clickable(actionStartActivity<BatteryDetailsActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(ringPadding).background(surface).cornerRadius(100.dp),
            ) {}
            Image(
                provider = ImageProvider(BatteryRingDrawer.drawProgressArc(item.level, android.graphics.Color.WHITE, hasStatus, bitmapPx, bitmapPx, gap)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ringColor),
                modifier = GlanceModifier.fillMaxSize(),
            )
            Image(
                provider = ImageProvider(BatteryRingDrawer.drawTrackArc(item.level, android.graphics.Color.WHITE, hasStatus, bitmapPx, bitmapPx, gap)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(track),
                modifier = GlanceModifier.fillMaxSize(),
            )
            Image(
                provider = ImageProvider(item.iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(onSurface),
                modifier = GlanceModifier.fillMaxSize().padding(iconPadding),
            )
            if (showPercentage) {
                val charging = item.statusIconRes == R.drawable.rounded_bolt_filled_24 || item.statusIconRes == R.drawable.rounded_shield_filled_24
                val layers =
                    remember(item.level, charging, bitmapPx) {
                        BatteryRingDrawer.drawPercentLayers(
                            context,
                            bitmapPx,
                            "${item.level}",
                            item.statusIconRes?.takeIf { charging }?.let { ContextCompat.getDrawable(context, it) },
                        )
                    }
                val chipWidth = cell * (layers.widthPx.toFloat() / bitmapPx)
                val chipHeight = cell * (layers.heightPx.toFloat() / bitmapPx)
                val top = (cell * 0.16f - chipHeight / 2f).coerceAtLeast(0.dp)
                Box(
                    modifier = GlanceModifier.fillMaxSize().padding(top = top),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(modifier = GlanceModifier.size(width = chipWidth, height = chipHeight), contentAlignment = Alignment.Center) {
                        layers.chip?.let {
                            Image(
                                provider = ImageProvider(it),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(ringColor),
                                modifier = GlanceModifier.fillMaxSize(),
                            )
                        }
                        Image(
                            provider = ImageProvider(layers.content),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(if (charging) surface else onSurface),
                            modifier = GlanceModifier.fillMaxSize(),
                        )
                    }
                }
            } else item.statusIconRes?.let { status ->
                val bubble = (cell * 0.28f).coerceIn(10.dp, 32.dp)
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    Box(
                        modifier = GlanceModifier.size(bubble).background(ringColor).cornerRadius(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(status),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(surface),
                            modifier = GlanceModifier.fillMaxSize().padding((bubble * 0.19f).coerceAtLeast(1.dp)),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_COLUMNS = 10
        const val MAX_ROWS = 8
        const val LOW_LEVEL = 15
        const val MIN_BITMAP_PX = 64
        const val MAX_BITMAP_PX = 256
    }
}
