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
import androidx.compose.ui.unit.dp
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
import com.sameerasw.essentials.island.ui.components.BatteryRing
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeBatteryPlugin : BaseIslandPlugin() {
    override val id = "time_battery"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_TIME_BATTERY,
        SettingsRepository.KEY_ISLAND_TIME_SHOW_SECONDS,
        SettingsRepository.KEY_ISLAND_SHOW_BATTERY,
        SettingsRepository.KEY_ISLAND_BATTERY_STYLE,
        SettingsRepository.KEY_ISLAND_BATTERY_PERCENTAGE,
        SettingsRepository.KEY_ISLAND_BATTERY_PERCENTAGE_CONDITIONAL,
        SettingsRepository.KEY_ISLAND_BATTERY_ONLY_LOW,
        SettingsRepository.KEY_ISLAND_BATTERY_ICON_CONDITIONAL,
        SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_CHARGING_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_POWER_SAVE_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_POWER_SAVE_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR_ENABLED,
        SettingsRepository.KEY_ISLAND_BATTERY_IDLE_COLOR_ENABLED,
        SettingsRepository.KEY_ISLAND_BATTERY_IDLE_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_LOW_COLOR,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR_ENABLED,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_COLOR,
    )

    private var time = ""
    private var level = -1
    private var charging = false
    private var shielded = false
    private var powerSave = false
    private var registered = false
    private var chargePeekUntil = 0L
    private val endChargePeek = Runnable { render() }
    private val secondsTick = object : Runnable {
        override fun run() {
            readTime()
            render()
            scheduleSecondsTick()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val wasPlugged = charging || shielded
                    readBattery(intent)
                    if ((charging || shielded) && !wasPlugged && level >= 0) {
                        render()
                        peekCharging()
                        return
                    }
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> readBattery(null)
                Intent.ACTION_SCREEN_OFF -> {
                    ctx?.mainHandler?.removeCallbacks(secondsTick)
                    return
                }
                Intent.ACTION_SCREEN_ON -> {
                    readTime()
                    scheduleSecondsTick()
                }
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
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
        )
        registered = true
        scheduleSecondsTick()
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(endChargePeek)
        ctx?.mainHandler?.removeCallbacks(secondsTick)
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
        scheduleSecondsTick()
    }

    private fun scheduleSecondsTick() {
        val handler = ctx?.mainHandler ?: return
        handler.removeCallbacks(secondsTick)
        if (!registered || !settings.isIslandShowTimeBatteryEnabled() || !showSeconds()) return
        handler.postDelayed(secondsTick, 1000L - System.currentTimeMillis() % 1000L)
    }

    private fun showSeconds() = settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_SHOW_SECONDS, false)

    private fun readTime() {
        val base = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        val pattern = if (showSeconds()) "$base:ss" else base
        time = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    }

    private fun readBattery(intent: Intent?) {
        val batteryIntent = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val raw = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (raw < 0 || scale <= 0) return
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        level = (raw * 100f / scale).toInt()
        val plugged = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        shielded = com.sameerasw.essentials.utils.battery.ChargeLimitUtil.isShielded(context, level, plugged)
        charging = plugged && !shielded
        powerSave = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
    }

    private fun peekCharging() {
        val c = ctx ?: return
        if (!settings.isIslandShowBatteryEnabled()) return
        val duration = settings.getIslandPeekDurationMs()
        
        chargePeekUntil = System.currentTimeMillis() + duration
        render()
        c.request(PluginRequest.Peek(BATTERY_KEY, duration))
        c.mainHandler.removeCallbacks(endChargePeek)
        c.mainHandler.postDelayed(endChargePeek, duration + 500L)
    }

    private fun render() {
        ctx ?: return
        val showTime = settings.isIslandShowTimeBatteryEnabled()
        val showBatteryItem = settings.isIslandShowBatteryEnabled()
        if (!showTime && !showBatteryItem) {
            publish(emptyList())
            return
        }
        val timeText = time
        val batteryLevel = level
        val stateColor = stateColor()
        val displayColor = stateColor ?: idleColor()
        val iconStyle = settings.getIslandBatteryStyle() == SettingsRepository.ISLAND_BATTERY_STYLE_ICON
        val showLevel = settings.isIslandBatteryPercentageEnabled() &&
            (!settings.isIslandBatteryPercentageConditional() || stateColor != null)

        val timeItem = IslandItem(
            key = "time",
            priority = IslandPriority.TIME,
            placement = CompactPlacement.Pinned,
            needsCompanyAtCenter = settings.isIslandBatteryIconConditional(),
            compact = listOf(
                CompactCell("time") { RollingText(timeText) },
            ),
        )
        val batteryItem = IslandItem(
            key = BATTERY_KEY,
            priority = IslandPriority.BATTERY,
            priorityGroup = "battery",
            placement = CompactPlacement.Pinned,
            outlineAccent = displayColor,
            companionOnly = settings.isIslandBatteryIconConditional() && stateColor == null && System.currentTimeMillis() >= chargePeekUntil,
            compact = listOf(
                CompactCell("battery") {
                    when {
                        iconStyle -> BatteryGlyph(batteryLevel, displayColor ?: MaterialTheme.colorScheme.primary, showLevel = showLevel)
                        else -> BatteryRing(batteryLevel, displayColor ?: MaterialTheme.colorScheme.primary, showLevel = showLevel)
                    }
                },
            ),
            line = if (charging || shielded) {
                LineContent(
                    icon = {
                        when {
                            iconStyle -> BatteryGlyph(batteryLevel, displayColor ?: MaterialTheme.colorScheme.primary, showLevel = false)
                            else -> BatteryRing(batteryLevel, displayColor ?: MaterialTheme.colorScheme.primary, showLevel = false)
                        }
                    },
                    start = "$batteryLevel%",
                    end = context.getString(if (shielded) R.string.island_battery_charge_limited else R.string.island_battery_charging),
                    endSlot = if (shielded) {
                        { IslandIcon(R.drawable.rounded_shield_filled_24, size = 18.dp, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        null
                    },
                )
            } else {
                null
            },
        )
        val showBattery = showBatteryItem && batteryLevel >= 0 && (
            !settings.isIslandBatteryOnlyLowEnabled() ||
                batteryLevel <= SettingsRepository.ISLAND_BATTERY_LOW_LEVEL ||
                System.currentTimeMillis() < chargePeekUntil
            )
        publish(listOfNotNull(timeItem.takeIf { showTime }, batteryItem.takeIf { showBattery }))
    }

    private fun idleColor(): Color? {
        if (!settings.isIslandBatteryIdleColorEnabled()) return null
        val raw = try {
            AndroidColor.parseColor(settings.getIslandBatteryIdleColor())
        } catch (_: Exception) {
            AndroidColor.WHITE
        }
        return Color(soften(raw))
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
            level in 0..SettingsRepository.ISLAND_BATTERY_CRITICAL_LEVEL && settings.isDuoBatteryCriticalColorEnabled() ->
                parse(settings.getDuoBatteryCriticalColor(), AndroidColor.rgb(244, 67, 54))
            level in 0..SettingsRepository.ISLAND_BATTERY_LOW_LEVEL && settings.isDuoBatteryLowColorEnabled() ->
                parse(settings.getDuoBatteryLowColor(), AndroidColor.rgb(255, 235, 59))
            else -> return null
        }
        return Color(soften(raw))
    }

    private companion object {
        const val BATTERY_KEY = "battery"
        val AUTO_CHARGING = AndroidColor.rgb(0, 230, 118)
    }
}
