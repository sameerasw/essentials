package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class AirSyncBridgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.sameerasw.essentials.action.UPDATE_MAC_BATTERY") {
            val pendingResult = goAsync()
            val level = intent.getIntExtra("level", -1)
            val isCharging = intent.getBooleanExtra("isCharging", false)
            val lastUpdated = intent.getLongExtra("lastUpdated", System.currentTimeMillis())
            val isConnected = intent.getBooleanExtra("isConnected", true)

            android.util.Log.d(
                "AirSyncBridge",
                "Received Mac status: level=$level, connected=$isConnected"
            )

            val repository = SettingsRepository(context)
            if (repository.getBoolean(SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)) {
                repository.putInt(SettingsRepository.KEY_MAC_BATTERY_LEVEL, level)
                repository.putBoolean(SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING, isCharging)
                repository.putLong(SettingsRepository.KEY_MAC_BATTERY_LAST_UPDATED, lastUpdated)
                repository.putBoolean(SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED, isConnected)

                // Trigger widget update directly
                val glanceAppWidgetManager =
                    androidx.glance.appwidget.GlanceAppWidgetManager(context)
                // Use IO dispatcher to avoid main thread jank/timeouts
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        // Define keys matching BatteriesWidget
                        val KEY_AIRSYNC_ENABLED =
                            androidx.datastore.preferences.core.booleanPreferencesKey(
                                SettingsRepository.KEY_AIRSYNC_CONNECTION_ENABLED)
                        val KEY_MAC_LEVEL =
                            androidx.datastore.preferences.core.intPreferencesKey(SettingsRepository.KEY_MAC_BATTERY_LEVEL)
                        val KEY_MAC_CONNECTED =
                            androidx.datastore.preferences.core.booleanPreferencesKey(
                                SettingsRepository.KEY_AIRSYNC_MAC_CONNECTED)

                        val glanceIds =
                            glanceAppWidgetManager.getGlanceIds(com.sameerasw.essentials.services.widgets.BatteriesWidget::class.java)

                        android.util.Log.d(
                            "AirSyncBridge",
                            "Found ${glanceIds.size} widgets to update"
                        )

                        glanceIds.forEach { glanceId ->
                            androidx.glance.appwidget.state.updateAppWidgetState(
                                context,
                                glanceId
                            ) { prefs ->
                                prefs[KEY_AIRSYNC_ENABLED] = true
                                prefs[KEY_MAC_LEVEL] = level
                                prefs[KEY_MAC_CONNECTED] = isConnected
                                // Add charging state
                                val KEY_MAC_IS_CHARGING =
                                    androidx.datastore.preferences.core.booleanPreferencesKey(
                                        SettingsRepository.KEY_MAC_BATTERY_IS_CHARGING)
                                prefs[KEY_MAC_IS_CHARGING] = isCharging
                            }

                            android.util.Log.d(
                                "AirSyncBridge",
                                "Triggering update for glanceId: $glanceId"
                            )
                            com.sameerasw.essentials.services.widgets.BatteriesWidget()
                                .update(context, glanceId)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("AirSyncBridge", "Error updating widget", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            } else {
                pendingResult.finish()
            }
        }
    }
}
