package com.sameerasw.essentials.services.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.launch

class BatteriesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteriesWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_POWER_CONNECTED ||
            intent.action == Intent.ACTION_POWER_DISCONNECTED ||
            intent.action == Intent.ACTION_BATTERY_LOW ||
            intent.action == Intent.ACTION_BATTERY_OKAY ||
            intent.action == "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED" ||
            intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED ||
            intent.action == android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED ||
            intent.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED ||
            intent.action == android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            
            // Trigger update
            val glanceAppWidgetManager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
            kotlinx.coroutines.MainScope().launch {
                     // Check permissions first
                     val repository = com.sameerasw.essentials.data.repository.SettingsRepository(context)
                     val hasPerm = com.sameerasw.essentials.utils.PermissionUtils.hasBluetoothPermission(context)
                     
                     val isEnabled = repository.isBluetoothDevicesEnabled()
                     val bluetoothDevices = if (isEnabled && hasPerm) {
                         com.sameerasw.essentials.utils.BluetoothBatteryUtils.getPairedDevicesBattery(context)
                     } else {
                         emptyList()
                     }
                     
                     repository.saveBluetoothDevicesBattery(bluetoothDevices)
                     
                     val devicesJson = com.google.gson.Gson().toJson(bluetoothDevices)

                     val glanceIds = glanceAppWidgetManager.getGlanceIds(BatteriesWidget::class.java)
                     glanceIds.forEach { glanceId ->
                         androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
                            val KEY_SHOW = androidx.datastore.preferences.core.booleanPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_SHOW_BLUETOOTH_DEVICES)
                            val KEY_DATA = androidx.datastore.preferences.core.stringPreferencesKey(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_BLUETOOTH_DEVICES_BATTERY)
                            
                            prefs[KEY_SHOW] = isEnabled
                            prefs[KEY_DATA] = devicesJson
                         }
                         glanceAppWidget.update(context, glanceId)
                     }
        }

            try {
                val requestIntent = Intent("com.sameerasw.airsync.action.REQUEST_MAC_BATTERY").apply {
                    setPackage("com.sameerasw.airsync")
                }
                context.sendBroadcast(requestIntent, "com.sameerasw.permission.ESSENTIALS_AIRSYNC_BRIDGE")
            } catch (e: Exception) {
                // Ignore if AirSync not installed/found
            }
        }
    }
}
