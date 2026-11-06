package com.sameerasw.essentials.island

import android.content.Context
import android.text.format.DateFormat
import com.sameerasw.essentials.data.repository.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IslandTimeFormat {
    fun is24Hour(context: Context, settings: SettingsRepository): Boolean =
        when (settings.getString(SettingsRepository.KEY_ISLAND_TIME_FORMAT, null)) {
            SettingsRepository.ISLAND_TIME_FORMAT_12H -> false
            SettingsRepository.ISLAND_TIME_FORMAT_24H -> true
            else -> DateFormat.is24HourFormat(context)
        }

    fun leadingZero(context: Context, settings: SettingsRepository): Boolean =
        if (settings.contains(SettingsRepository.KEY_ISLAND_TIME_LEADING_ZERO)) {
            settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_LEADING_ZERO, false)
        } else {
            is24Hour(context, settings)
        }

    fun pattern(context: Context, settings: SettingsRepository, seconds: Boolean = false, period: Boolean? = null): String {
        val is24 = is24Hour(context, settings)
        val hours = if (is24) {
            if (leadingZero(context, settings)) "HH" else "H"
        } else if (leadingZero(context, settings)) {
            "hh"
        } else {
            "h"
        }
        val base = if (seconds) "$hours:mm:ss" else "$hours:mm"
        val showPeriod = !is24 && (period ?: (settings.getString(SettingsRepository.KEY_ISLAND_TIME_PERIOD, null) == SettingsRepository.ISLAND_TIME_PERIOD_AM_PM))
        return if (showPeriod) "$base a" else base
    }

    fun format(context: Context, settings: SettingsRepository, millis: Long, seconds: Boolean = false, period: Boolean? = null): String =
        SimpleDateFormat(pattern(context, settings, seconds, period), Locale.getDefault()).format(Date(millis))
}
