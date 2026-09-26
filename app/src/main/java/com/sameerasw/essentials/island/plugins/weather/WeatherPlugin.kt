package com.sameerasw.essentials.island.plugins.weather

import com.sameerasw.essentials.island.plugins.brief.BriefPlugin
import com.sameerasw.essentials.island.model.InteractionOverrides
import com.sameerasw.essentials.utils.DeviceUtils
import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.weather.WeatherFormat
import com.sameerasw.essentials.weather.WeatherRepository
import com.sameerasw.essentials.weather.model.WeatherState
import com.sameerasw.essentials.weather.work.WeatherScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherPlugin : BaseIslandPlugin() {
    override val id = "weather"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_WEATHER,
        SettingsRepository.KEY_ISLAND_WEATHER_MODE,
        SettingsRepository.KEY_ISLAND_WEATHER_PEEK_ALERTS,
        SettingsRepository.KEY_WEATHER_PROVIDER,
        SettingsRepository.KEY_WEATHER_API_KEY,
        SettingsRepository.KEY_WEATHER_LOCATION_MODE,
        SettingsRepository.KEY_WEATHER_MANUAL_LOCATION,
        SettingsRepository.KEY_WEATHER_UNITS,
        SettingsRepository.KEY_ISLAND_WEATHER_EFFECTS,
        SettingsRepository.KEY_ISLAND_WEATHER_HAPTICS,
        SettingsRepository.KEY_WEATHER_REFRESH_MINUTES,
    )

    private var observer: Job? = null
    private var sourceSignature: String? = null
    private var state = WeatherState()

    override fun onStart() {
        val c = ctx!!
        observer = c.scope.launch {
            WeatherRepository.ensureLoaded(context)
            WeatherRepository.state.collect { next ->
                state = next
                render()
                announceAlerts()
            }
        }
    }

    override fun onStop() {
        observer?.cancel()
        observer = null
    }

    override fun refresh() {
        val c = ctx ?: return
        if (!settings.isIslandShowWeatherEnabled()) {
            WeatherScheduler.cancel(context)
            sourceSignature = null
            render()
            return
        }
        WeatherScheduler.schedule(context, settings.getWeatherRefreshMinutes())
        val signature = listOf(
            settings.getWeatherProvider(),
            settings.getWeatherApiKey(),
            settings.getWeatherLocationMode(),
            settings.getWeatherManualLocation()?.toString(),
        ).joinToString("|")
        val sourceChanged = sourceSignature != null && sourceSignature != signature
        sourceSignature = signature
        if (sourceChanged || WeatherRepository.isStale(context)) {
            c.scope.launch { WeatherRepository.refresh(context, force = sourceChanged) }
        }
        render()
    }

    private fun announceAlerts() {
        val c = ctx ?: return
        if (!settings.isIslandShowWeatherEnabled() || !settings.isIslandWeatherPeekAlertsEnabled()) return
        val severe = state.snapshot?.activeAlerts()?.filter { it.severity.isSevere }.orEmpty()
        if (severe.isEmpty()) return
        c.scope.launch {
            val fresh = WeatherRepository.takeUnnotifiedAlerts(context, severe.map { it.id })
            if (fresh.isNotEmpty()) c.request(PluginRequest.Peek(ITEM_KEY, settings.getIslandPeekDurationMs()))
        }
    }

    private fun render() {
        val snapshot = state.snapshot
        if (ctx == null || !settings.isIslandShowWeatherEnabled() || snapshot == null) {
            publish(null)
            return
        }
        val unit = WeatherFormat.unitFor(settings.getWeatherUnits())
        val mode = settings.getIslandWeatherMode()
        val effects = settings.isIslandWeatherEffectsEnabled() && !DeviceUtils.isPowerSaveMode(context)
        val haptics = settings.isIslandWeatherHapticsEnabled()
        val alert = snapshot.activeAlerts().filter { it.severity.isSevere }.maxByOrNull { it.severity.ordinal }
        val temperature = WeatherFormat.temperature(snapshot.tempC, unit)
        val icon = if (alert != null) R.drawable.rounded_warning_24 else WeatherFormat.icon(snapshot.condition, snapshot.isDay)
        val compactVisible = when (mode) {
            SettingsRepository.ISLAND_WEATHER_MODE_COMPACT -> true
            SettingsRepository.ISLAND_WEATHER_MODE_ALERTS -> alert != null
            else -> false
        }
        val lineEnd = if (alert != null) {
            alert.expiresMillis?.let { context.getString(R.string.weather_alert_until, formatTime(it)) } ?: temperature
        } else {
            listOf(temperature, snapshot.conditionText).filter { it.isNotBlank() }.joinToString(" · ")
        }
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.WEATHER,
                priorityOverride = IslandPriority.WEATHER_ALERT.takeIf { alert != null },
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("weather.icon") {
                        val tint = if (alert != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        IslandIcon(icon, size = 18.dp, tint = tint)
                    },
                    CompactCell("weather.temp") { RollingText(temperature) },
                ),
                line = LineContent(
                    icon = {
                        val tint = if (alert != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        IslandIcon(icon, tint = tint)
                    },
                    start = alert?.event ?: snapshot.locationName,
                    end = lineEnd,
                ),
                expanded = ExpandedContent { scope ->
                    WeatherExpanded(
                        unit = unit,
                        scope = scope,
                        effects = effects,
                        haptics = haptics,
                        onRefresh = { ctx?.scope?.launch { WeatherRepository.refresh(context, force = true) } },
                    )
                },
                interactions = InteractionOverrides(
                    onTap = {
                        settings.isIslandBriefEnabled().also { brief ->
                            if (brief) ctx?.request?.invoke(PluginRequest.Expand(BriefPlugin.ITEM_KEY))
                        }
                    },
                ),
                compactVisible = compactVisible,
            ),
        )
    }

    private fun formatTime(millis: Long): String {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
    }

    companion object {
        const val ITEM_KEY = "weather"
    }
}
