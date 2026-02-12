package com.sameerasw.essentials.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sameerasw.essentials.domain.model.LocationAlarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationReachedRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)

    companion object {
        private val _isProcessing = MutableStateFlow(false)
        val isProcessing = _isProcessing.asStateFlow()

        private val _alarmFlow = MutableStateFlow<LocationAlarm?>(null)
        val alarmFlow = _alarmFlow.asStateFlow()
    }

    init {
        if (_alarmFlow.value == null) {
            _alarmFlow.value = getAlarm()
        }
    }

    fun setIsProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }

    fun saveAlarm(alarm: LocationAlarm) {
        prefs.edit().apply {
            putLong("location_reached_lat", java.lang.Double.doubleToRawLongBits(alarm.latitude))
            putLong("location_reached_lng", java.lang.Double.doubleToRawLongBits(alarm.longitude))
            putInt("location_reached_radius", alarm.radius)
            putBoolean("location_reached_enabled", alarm.isEnabled)
            apply()
        }
        _alarmFlow.value = alarm
    }

    fun getAlarm(): LocationAlarm {
        val lat = java.lang.Double.longBitsToDouble(
            prefs.getLong(
                "location_reached_lat",
                java.lang.Double.doubleToRawLongBits(0.0)
            )
        )
        val lng = java.lang.Double.longBitsToDouble(
            prefs.getLong(
                "location_reached_lng",
                java.lang.Double.doubleToRawLongBits(0.0)
            )
        )
        val radius = prefs.getInt("location_reached_radius", 1000)
        val enabled = prefs.getBoolean("location_reached_enabled", false)
        return LocationAlarm(lat, lng, radius, enabled)
    }

    fun saveStartDistance(distance: Float) {
        prefs.edit().putFloat("location_reached_start_dist", distance).apply()
    }

    fun getStartDistance(): Float {
        return prefs.getFloat("location_reached_start_dist", 0f)
    }
}
