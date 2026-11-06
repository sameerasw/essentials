/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: BatteriesWidgetReceiver.kt
 * Description: Background service component for BatteriesWidgetReceiver.kt.
 */

package com.sameerasw.essentials.services.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.google.gson.Gson
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.BluetoothBatteryUtils
import com.sameerasw.essentials.utils.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BatteriesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteriesWidget()

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)

        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == Intent.ACTION_CONFIGURATION_CHANGED) {
                    delay(500)
                }
                updateWidgets(context)
                BatteryWidgetJobService.schedule(context)
            } catch (e: Exception) {
                android.util.Log.e("BatteriesWidget", "Error updating widget on intent: ${intent.action}", e)
            } finally {
                try { pendingResult?.finish() } catch (_: Exception) {}
            }
        }
    }

    companion object {
        fun updateWidgets(context: Context) {
            val appContext = context.applicationContext ?: context
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val glanceAppWidgetManager = GlanceAppWidgetManager(appContext)
                    val repository = SettingsRepository(appContext)
                    val hasPerm = PermissionUtils.hasBluetoothPermission(appContext)

                    val isEnabled = repository.isBluetoothDevicesEnabled()
                    val bluetoothDevices =
                        if (isEnabled && hasPerm) {
                            BluetoothBatteryUtils.getPairedDevicesBattery(appContext)
                        } else {
                            emptyList()
                        }

                    repository.saveBluetoothDevicesBattery(bluetoothDevices)

                    val maxDevices = repository.getBatteryWidgetMaxDevices()
                    val devicesJson = Gson().toJson(bluetoothDevices)

                    val batteryStatus: Intent? = try {
                        appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    } catch (_: Exception) {
                        null
                    }
                    val rawLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
                    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val bm = appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    val fallbackLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                    val phoneLevel = if (rawLevel >= 0 && scale > 0) {
                        (rawLevel * 100 / scale)
                    } else {
                        fallbackLevel
                    }

                    val bmCharging = bm?.isCharging == true
                    val intentCharging = plugged != 0 ||
                        status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                    val phoneCharging = if (batteryStatus != null) intentCharging else bmCharging

                    val KEY_SHOW = booleanPreferencesKey(SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES)
                    val KEY_DATA = stringPreferencesKey(SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY)
                    val KEY_MAX = intPreferencesKey(SettingsRepository.KEY_BATTERY_WIDGET_MAX_DEVICES)
                    val KEY_PHONE_LEVEL = intPreferencesKey(SettingsRepository.KEY_PHONE_BATTERY_LEVEL)
                    val KEY_PHONE_CHARGING = booleanPreferencesKey(SettingsRepository.KEY_PHONE_BATTERY_IS_CHARGING)
                    val KEY_TIMESTAMP = longPreferencesKey("widget_last_update_ts")

                    val glanceIds = try {
                        glanceAppWidgetManager.getGlanceIds(BatteriesWidget::class.java)
                    } catch (_: Exception) {
                        emptyList()
                    }

                    val updatedGlanceIds = mutableSetOf<androidx.glance.GlanceId>()

                    glanceIds.forEach { glanceId ->
                        try {
                            updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                                prefs.toMutablePreferences().apply {
                                    this[KEY_SHOW] = isEnabled
                                    this[KEY_DATA] = devicesJson
                                    this[KEY_MAX] = maxDevices
                                    if (phoneLevel != -1) {
                                        this[KEY_PHONE_LEVEL] = phoneLevel
                                    }
                                    this[KEY_PHONE_CHARGING] = phoneCharging
                                    this[KEY_TIMESTAMP] = System.currentTimeMillis()
                                }
                            }
                            BatteriesWidget().update(appContext, glanceId)
                            updatedGlanceIds.add(glanceId)
                        } catch (e: Exception) {
                            android.util.Log.e("BatteriesWidget", "Error updating glanceId $glanceId", e)
                        }
                    }

                    // Fallback to iterate via AppWidgetManager
                    val appWidgetManager = AppWidgetManager.getInstance(appContext)
                    val appWidgetIds = appWidgetManager?.getAppWidgetIds(
                        ComponentName(appContext, BatteriesWidgetReceiver::class.java)
                    ) ?: intArrayOf()

                    appWidgetIds.forEach { appWidgetId ->
                        try {
                            val glanceId = glanceAppWidgetManager.getGlanceIdBy(appWidgetId)
                            if (!updatedGlanceIds.contains(glanceId)) {
                                updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                                    prefs.toMutablePreferences().apply {
                                        this[KEY_SHOW] = isEnabled
                                        this[KEY_DATA] = devicesJson
                                        this[KEY_MAX] = maxDevices
                                        if (phoneLevel != -1) {
                                            this[KEY_PHONE_LEVEL] = phoneLevel
                                        }
                                        this[KEY_PHONE_CHARGING] = phoneCharging
                                        this[KEY_TIMESTAMP] = System.currentTimeMillis()
                                    }
                                }
                                BatteriesWidget().update(appContext, glanceId)
                            }
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BatteriesWidget", "Error in updateWidgets", e)
                }

                try {
                    val requestIntent =
                        Intent("com.sameerasw.airsync.action.REQUEST_MAC_BATTERY").apply {
                            setPackage("com.sameerasw.airsync")
                        }
                    appContext.sendBroadcast(
                        requestIntent,
                        "com.sameerasw.permission.ESSENTIALS_AIRSYNC_BRIDGE",
                    )
                } catch (_: Exception) {}
            }
        }
    }
}
