package com.sameerasw.essentials.island.plugins.timebattery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.BatteryManager
import android.os.PowerManager
import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.soften
import com.sameerasw.essentials.island.ui.components.BatteryRing
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.utils.battery.BatteryInfoUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeBatteryPlugin : BaseIslandPlugin() {
    override val id = "time_battery"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_TIME_BATTERY,
        SettingsRepository.KEY_ISLAND_BATTERY_STYLE,
        SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_POWER_SAVE_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_POWER_SAVE_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR,
    )

    private var time = ""
    private var level = -1
    private var charging = false
    private var powerSave = false
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> readBattery(intent)
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> readBattery(null)
                else -> readTime()
            }
            render()
        }
    }

    override fun onStart() {
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
        )
        registered = true
    }

    override fun onStop() {
        if (registered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
            registered = false
        }
    }

    override fun refresh() {
        ctx ?: return
        readTime()
        readBattery(null)
        render()
    }

    private fun readTime() {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        time = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    private fun readBattery(intent: Intent?) {
        val batteryIntent = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val raw = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (raw < 0 || scale <= 0) return
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        level = (raw * 100f / scale).toInt()
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        powerSave = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
    }

    private fun render() {
        ctx ?: return
        if (!settings.isIslandShowTimeBatteryEnabled()) {
            publish(emptyList())
            return
        }
        val timeText = time
        val batteryLevel = level
        val stateColor = stateColor()
        val iconStyle = settings.getIslandBatteryStyle() == SettingsRepository.ISLAND_BATTERY_STYLE_ICON
        val iconRes = BatteryInfoUtil.getBatteryIconRes(context, batteryLevel, charging, isPowerSave = powerSave)

        val timeItem = IslandItem(
            key = "time",
            priority = IslandPriority.TIME,
            placement = CompactPlacement.Pinned,
            compact = listOf(
                CompactCell("time") { RollingText(timeText) },
            ),
        )
        val batteryItem = IslandItem(
            key = "battery",
            priority = IslandPriority.BATTERY,
            placement = CompactPlacement.Pinned,
            compact = listOf(
                CompactCell("battery") {
                    if (iconStyle) {
                        IslandIcon(iconRes, tint = stateColor ?: Color.White)
                    } else {
                        BatteryRing(batteryLevel, stateColor ?: MaterialTheme.colorScheme.primary)
                    }
                },
            ),
        )
        publish(if (batteryLevel >= 0) listOf(timeItem, batteryItem) else listOf(timeItem))
    }

    private fun stateColor(): Color? {
        fun parse(hex: String, fallback: Int) = try {
            AndroidColor.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
        val charge = settings.getDuoBatteryChargingColor()
        val raw = when {
            charging && settings.isDuoBatteryChargingColorEnabled() ->
                if (charge.equals("auto", ignoreCase = true)) AUTO_CHARGING else parse(charge, AUTO_CHARGING)
            powerSave && settings.isDuoBatteryPowerSaveColorEnabled() ->
                parse(settings.getDuoBatteryPowerSaveColor(), AndroidColor.rgb(255, 152, 0))
            level in 0..CRITICAL_LEVEL && settings.isDuoBatteryCriticalColorEnabled() ->
                parse(settings.getDuoBatteryCriticalColor(), AndroidColor.rgb(244, 67, 54))
            level in 0..LOW_LEVEL && settings.isDuoBatteryLowColorEnabled() ->
                parse(settings.getDuoBatteryLowColor(), AndroidColor.rgb(255, 235, 59))
            else -> return null
        }
        return Color(soften(raw))
    }

    private companion object {
        const val LOW_LEVEL = 20
        const val CRITICAL_LEVEL = 10
        val AUTO_CHARGING = AndroidColor.rgb(0, 230, 118)
    }
}
