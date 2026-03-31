package com.sameerasw.essentials.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.essentials.domain.model.LocationAlarm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class LocationReachedRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private val _isProcessing = MutableStateFlow(false)
        val isProcessing = _isProcessing.asStateFlow()

        private val _alarmsFlow = MutableStateFlow<List<LocationAlarm>>(emptyList())
        val alarmsFlow = _alarmsFlow.asStateFlow()

        private val _activeAlarmId = MutableStateFlow<String?>(null)
        val activeAlarmId = _activeAlarmId.asStateFlow()

        private val _tempAlarm = MutableStateFlow<LocationAlarm?>(null)
        val tempAlarm = _tempAlarm.asStateFlow()

        private val _showBottomSheet = MutableStateFlow(false)
        val showBottomSheet = _showBottomSheet.asStateFlow()
    }

    fun setTempAlarm(alarm: LocationAlarm?) {
        _tempAlarm.value = alarm
    }

    fun setShowBottomSheet(show: Boolean) {
        _showBottomSheet.value = show
    }

    init {
        migrateIfNeeded()
        _alarmsFlow.value = getAlarms()
        _activeAlarmId.value = getActiveAlarmId()
    }

    private fun migrateIfNeeded() {
        if (prefs.contains("location_reached_lat") && !prefs.contains("location_reached_alarms_json")) {
            val lat = java.lang.Double.longBitsToDouble(prefs.getLong("location_reached_lat", 0L))
            val lng = java.lang.Double.longBitsToDouble(prefs.getLong("location_reached_lng", 0L))
            val radius = prefs.getInt("location_reached_radius", 1000)
            val enabled = prefs.getBoolean("location_reached_enabled", false)

            if (lat != 0.0 || lng != 0.0) {
                val migratedAlarm = LocationAlarm(
                    id = UUID.randomUUID().toString(),
                    name = "Migrated Destination",
                    latitude = lat,
                    longitude = lng,
                    radius = radius,
                    isEnabled = enabled
                )
                saveAlarms(listOf(migratedAlarm))
                if (enabled) {
                    saveActiveAlarmId(migratedAlarm.id)
                }
            }
            
            // Clear old prefs
            prefs.edit().apply {
                remove("location_reached_lat")
                remove("location_reached_lng")
                remove("location_reached_radius")
                remove("location_reached_enabled")
                apply()
            }
        }
    }

    fun setIsProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }

    fun saveAlarms(alarms: List<LocationAlarm>) {
        val json = gson.toJson(alarms)
        prefs.edit().putString("location_reached_alarms_json", json).apply()
        _alarmsFlow.value = alarms
    }

    fun getAlarms(): List<LocationAlarm> {
        val json = prefs.getString("location_reached_alarms_json", null) ?: return emptyList()
        val type = object : TypeToken<List<LocationAlarm>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveActiveAlarmId(id: String?) {
        prefs.edit().putString("location_reached_active_id", id).apply()
        _activeAlarmId.value = id
    }

    fun getActiveAlarmId(): String? {
        return prefs.getString("location_reached_active_id", null)
    }

    fun saveLastTrip(alarm: LocationAlarm?) {
        if (alarm == null) {
            prefs.edit().remove("location_reached_last_trip_json").apply()
        } else {
            val json = gson.toJson(alarm)
            prefs.edit().putString("location_reached_last_trip_json", json).apply()
        }
    }

    fun getLastTrip(): LocationAlarm? {
        val json = prefs.getString("location_reached_last_trip_json", null) ?: return null
        return try {
            gson.fromJson(json, LocationAlarm::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun saveStartDistance(distance: Float) {
        prefs.edit().putFloat("location_reached_start_dist", distance).apply()
    }

    fun getStartDistance(): Float {
        return prefs.getFloat("location_reached_start_dist", 0f)
    }

    fun saveStartTime(time: Long) {
        prefs.edit().putLong("location_reached_start_time", time).apply()
    }

    fun getStartTime(): Long {
        return prefs.getLong("location_reached_start_time", 0L)
    }

    fun updateLastTravelled(alarmId: String, timestamp: Long) {
        val alarms = getAlarms().toMutableList()
        val index = alarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            alarms[index] = alarms[index].copy(lastTravelled = timestamp)
            saveAlarms(alarms)
        }
    }
}
