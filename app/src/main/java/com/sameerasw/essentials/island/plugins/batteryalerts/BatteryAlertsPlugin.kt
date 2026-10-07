package com.sameerasw.essentials.island.plugins.batteryalerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.BatteryManager
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.soften
import com.sameerasw.essentials.island.ui.components.BatteryGlyph
import androidx.core.graphics.toColorInt

class BatteryAlertsPlugin : BaseIslandPlugin() {
    override val id = "battery_alerts_plugin"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_BATTERY_ALERTS,
        SettingsRepository.KEY_ISLAND_BATTERY_ALERT_CHARGING,
        SettingsRepository.KEY_ISLAND_BATTERY_ALERT_LOW,
        SettingsRepository.KEY_ISLAND_BATTERY_ALERT_CRITICAL,
        SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR,
    )

    private var level = -1
    private var isCharging = false
    private var registered = false
    private var peeking = false

    private val endPeek = Runnable {
        peeking = false
        render()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val wasCharging = isCharging
                val prevAlertState = currentAlertState()
                readBattery(intent)
                val newAlertState = currentAlertState()

                if ((isCharging && !wasCharging) || (newAlertState != prevAlertState && newAlertState.isNotEmpty())) {
                    peekAlert()
                } else if (!peeking) {
                    render()
                }
            }
        }
    }

    override fun onStart() {
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registered = true
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(endPeek)
        peeking = false
        if (registered) {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            registered = false
        }
    }

    override fun refresh() {
        readBattery(null)
        render()
    }

    private fun readBattery(intent: Intent?) {
        val batteryIntent = intent ?: context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ) ?: return

        val raw = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (raw < 0 || scale <= 0) return

        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        level = (raw * 100f / scale).toInt()
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun currentAlertState(): String {
        val allowCharging = settings.getBoolean(SettingsRepository.KEY_ISLAND_BATTERY_ALERT_CHARGING, true)
        val allowLowPower = settings.getBoolean(SettingsRepository.KEY_ISLAND_BATTERY_ALERT_LOW, true)
        val allowCritical = settings.getBoolean(SettingsRepository.KEY_ISLAND_BATTERY_ALERT_CRITICAL, true)

        val isLow = level in (SettingsRepository.ISLAND_BATTERY_CRITICAL_LEVEL + 1)..SettingsRepository.ISLAND_BATTERY_LOW_LEVEL
        val isCritical = level in 0..SettingsRepository.ISLAND_BATTERY_CRITICAL_LEVEL

        return when {
            isCharging && allowCharging -> "charging"
            isCritical && allowCritical -> "critical"
            isLow && allowLowPower -> "low"
            else -> ""
        }
    }

    private fun getBatteryColor(state: String): Color {
        fun parse(hex: String, fallback: Int) = try {
            hex.toColorInt()
        } catch (_: Exception) {
            fallback
        }

        val raw = when (state) {
            "charging" -> {
                val charge = settings.getDuoBatteryChargingColor()
                if (charge.equals("auto", ignoreCase = true)) AUTO_CHARGING else parse(charge, AUTO_CHARGING)
            }
            "low" -> {
                parse(settings.getDuoBatteryLowColor(), AndroidColor.rgb(255, 235, 59))
            }
            "critical" -> {
                parse(settings.getDuoBatteryCriticalColor(), AndroidColor.rgb(244, 67, 54))
            }
            else -> AUTO_CHARGING
        }
        return Color(soften(raw))
    }

    private fun peekAlert() {
        val c = ctx ?: return
        val duration = settings.getIslandPeekDurationMs()
        peeking = true
        render()
        c.request(PluginRequest.Peek(ITEM_KEY, duration))
        c.mainHandler.removeCallbacks(endPeek)
        c.mainHandler.postDelayed(endPeek, duration + 500L)
    }

    private fun render() {
        if (ctx == null) {
            publish(emptyList())
            return
        }

        val masterEnabled = settings.getBoolean(SettingsRepository.KEY_ISLAND_SHOW_BATTERY_ALERTS, true)
        if (!masterEnabled || !peeking || level < 0) {
            publish(emptyList())
            return
        }

        val state = currentAlertState()
        if (state.isEmpty()) {
            publish(emptyList())
            return
        }

        val alertTitleRes = when (state) {
            "charging" -> R.string.island_battery_alert_charging
            "critical" -> R.string.island_battery_alert_critical
            "low" -> R.string.island_battery_alert_low
            else -> null
        }

        if (alertTitleRes == null) {
            publish(emptyList())
            return
        }

        val alertTitle = context.getString(alertTitleRes)

        val alertColor = getBatteryColor(state)

        val item = IslandItem(
            key = ITEM_KEY,
            priority = IslandPriority.BATTERY_ALERT,
            placement = CompactPlacement.Dynamic,
            accent = alertColor,
            compactVisible = false,
            compact = listOf(
                CompactCell("alert_icon") {
                    BatteryGlyph(level = level, color = alertColor, showLevel = true)
                }
            ),
            line = LineContent(
                icon = { BatteryGlyph(level = level, color = alertColor, showLevel = false) },
                start = alertTitle,
                end = "$level%"
            )
        )

        publish(item)
    }

    companion object {
        const val ITEM_KEY = "battery_alert"
        val AUTO_CHARGING = AndroidColor.rgb(0, 230, 118)
    }
}
