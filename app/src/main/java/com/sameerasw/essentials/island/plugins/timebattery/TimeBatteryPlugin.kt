package com.sameerasw.essentials.island.plugins.timebattery

import com.sameerasw.essentials.island.state.IslandStillness
import com.sameerasw.essentials.island.ui.components.islandAccent

import com.sameerasw.essentials.island.ui.islandTextColor
import com.sameerasw.essentials.island.ui.IslandTextStyles
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import com.sameerasw.essentials.island.IslandTimeFormat
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.AlarmClock
import android.os.PowerManager
import android.text.format.DateFormat
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.InteractionOverrides
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
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
        SettingsRepository.KEY_ISLAND_TIME_FORMAT,
        SettingsRepository.KEY_ISLAND_TIME_PERIOD,
        SettingsRepository.KEY_ISLAND_TIME_LEADING_ZERO,
        SettingsRepository.KEY_ISLAND_TIME_DAY_SUFFIX,
        SettingsRepository.KEY_ISLAND_TIME_TAP_OPENS_CLOCK,
        SettingsRepository.KEY_ISLAND_TIME_HIDE_LOCKED,
        SettingsRepository.KEY_ISLAND_TIME_CHIP,
        SettingsRepository.KEY_ISLAND_BATTERY_HIDE_LOCKED,
        SettingsRepository.KEY_ISLAND_SHOW_BATTERY,
        SettingsRepository.KEY_ISLAND_BATTERY_STYLE,
        SettingsRepository.KEY_ISLAND_BATTERY_PERCENTAGE,
        SettingsRepository.KEY_ISLAND_BATTERY_PERCENTAGE_CONDITIONAL,
        SettingsRepository.KEY_ISLAND_BATTERY_ONLY_LOW,
        SettingsRepository.KEY_ISLAND_BATTERY_ICON_CONDITIONAL,
        SettingsRepository.KEY_ISLAND_BATTERY_PEEK_ALERTS,
        SettingsRepository.KEY_DUO_BATTERY_LOW_LEVEL,
        SettingsRepository.KEY_DUO_BATTERY_CRITICAL_LEVEL,
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
    private var day = ""
    private var level = -1
    private var charging = false
    private var shielded = false
    private var powerSave = false
    private var registered = false
    private var peekUntil = 0L
    private var peekReason: PeekReason? = null
    private val endPeek = Runnable {
        peekReason = null
        render()
    }

    private enum class PeekReason { Charging, Saver, Low, Critical }
    private val clockTick = object : Runnable {
        override fun run() {
            readTime()
            render()
            scheduleClockTick()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    val wasPlugged = charging || shielded
                    val previousSeverity = severity()
                    readBattery(intent)
                    if ((charging || shielded) && !wasPlugged && level >= 0) {
                        render()
                        peek(PeekReason.Charging)
                        return
                    }
                    val plugged = charging || shielded
                    val severity = severity()
                    if (!plugged && level >= 0 && severity > previousSeverity) {
                        render()
                        peek(if (severity == 2) PeekReason.Critical else PeekReason.Low)
                        return
                    }
                }
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                    val wasSaving = powerSave
                    readBattery(null)
                    if (powerSave && !wasSaving && level >= 0) {
                        render()
                        peek(PeekReason.Saver)
                        return
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    ctx?.mainHandler?.removeCallbacks(clockTick)
                    return
                }
                Intent.ACTION_SCREEN_ON -> {
                    readTime()
                    scheduleClockTick()
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
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
        )
        registered = true
        scheduleClockTick()
    }

    override fun onStop() {
        ctx?.mainHandler?.removeCallbacks(endPeek)
        ctx?.mainHandler?.removeCallbacks(clockTick)
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
        scheduleClockTick()
    }

    private fun scheduleClockTick() {
        val handler = ctx?.mainHandler ?: return
        handler.removeCallbacks(clockTick)
        if (!registered || !settings.isIslandShowTimeBatteryEnabled()) return
        val now = System.currentTimeMillis()
        handler.postDelayed(clockTick, if (showSeconds()) 1000L - now % 1000L else IslandStillness.msToNextMinute(now))
    }

    private fun showSeconds() = settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_SHOW_SECONDS, false) && !IslandStillness.isStill

    private fun readTime() {
        time = IslandTimeFormat.format(context, settings, System.currentTimeMillis(), seconds = showSeconds())
        day = if (settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_DAY_SUFFIX, false)) {
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
        } else {
            ""
        }
    }

    private fun readBattery(intent: Intent?) {
        val status = readBatteryStatus(context, intent) ?: return
        level = status.level
        shielded = status.limited
        charging = status.charging
        powerSave = status.powerSave
    }

    private fun severity(): Int = when {
        level < 0 -> 0
        level <= settings.getDuoBatteryCriticalLevel() -> 2
        level <= settings.getDuoBatteryLowLevel() -> 1
        else -> 0
    }

    private fun peek(reason: PeekReason) {
        val c = ctx ?: return
        if (!settings.isIslandShowBatteryEnabled() || !settings.isIslandBatteryPeekAlertsEnabled()) return
        val duration = settings.getIslandPeekDurationMs()
        peekReason = reason
        peekUntil = System.currentTimeMillis() + duration
        render()
        c.request(PluginRequest.Peek(BATTERY_KEY, duration))
        c.mainHandler.removeCallbacks(endPeek)
        c.mainHandler.postDelayed(endPeek, duration + 500L)
    }

    private fun peekLine(reason: PeekReason): Pair<String, Boolean> = when (reason) {
        PeekReason.Charging -> context.getString(if (shielded) R.string.island_battery_charge_limited else R.string.island_battery_charging) to shielded
        PeekReason.Saver -> context.getString(R.string.island_battery_state_saver) to false
        PeekReason.Low -> context.getString(R.string.island_battery_state_low) to false
        PeekReason.Critical -> context.getString(R.string.island_battery_state_critical) to false
    }

    private fun render() {
        ctx ?: return
        val locked = (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
        val showTime = settings.isIslandShowTimeBatteryEnabled() &&
            !(locked && settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_HIDE_LOCKED, false))
        val showBatteryItem = settings.isIslandShowBatteryEnabled() &&
            !(locked && settings.getBoolean(SettingsRepository.KEY_ISLAND_BATTERY_HIDE_LOCKED, false))
        if (!showTime && !showBatteryItem) {
            publish(emptyList())
            return
        }
        val timeText = time
        val dayText = day
        val batteryLevel = level
        val isCharging = charging
        val isSaving = powerSave
        val isLimited = shielded
        val stateColor = stateColor()
        val displayColor = stateColor ?: idleColor()
        val iconStyle = settings.getIslandBatteryStyle() == SettingsRepository.ISLAND_BATTERY_STYLE_ICON
        val showLevel = settings.isIslandBatteryPercentageEnabled() &&
            (!settings.isIslandBatteryPercentageConditional() || stateColor != null)

        val lineReason = when {
            System.currentTimeMillis() < peekUntil -> peekReason
            charging || shielded -> PeekReason.Charging
            else -> null
        }

        val timeItem = IslandItem(
            key = "time",
            priority = IslandPriority.TIME,
            priorityGroup = "time",
            placement = CompactPlacement.Pinned,
            needsCompanyAtCenter = settings.isIslandBatteryIconConditional(),
            chip = settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_CHIP, false),
            interactions = if (settings.getBoolean(SettingsRepository.KEY_ISLAND_TIME_TAP_OPENS_CLOCK, false)) {
                InteractionOverrides(onTap = { openClock(); true })
            } else {
                InteractionOverrides.Default
            },
            compact = listOf(
                CompactCell("time") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RollingText(timeText)
                        if (dayText.isNotEmpty()) {
                            Text(
                                dayText,
                                style = IslandTextStyles.compact.copy(fontSize = 8.sp, color = islandTextColor(0.7f)),
                                modifier = Modifier.padding(start = 2.dp),
                            )
                        }
                    }
                },
            ),
        )
        val batteryItem = IslandItem(
            key = BATTERY_KEY,
            priority = IslandPriority.BATTERY,
            priorityGroup = "battery",
            placement = CompactPlacement.Pinned,
            outlineAccent = displayColor,
            companionOnly = settings.isIslandBatteryIconConditional() && stateColor == null && System.currentTimeMillis() >= peekUntil,
            compact = listOf(
                CompactCell("battery") {
                    when {
                        iconStyle -> BatteryGlyph(batteryLevel, displayColor ?: islandAccent(), showLevel = showLevel, charging = isCharging, powerSave = isSaving, limited = isLimited)
                        else -> BatteryRing(batteryLevel, displayColor ?: islandAccent(), showLevel = showLevel, charging = isCharging, powerSave = isSaving, limited = isLimited)
                    }
                },
            ),
            line = lineReason?.let { reason ->
                val (label, showShield) = peekLine(reason)
                LineContent(
                    icon = {
                        when {
                            iconStyle -> BatteryGlyph(batteryLevel, displayColor ?: islandAccent(), showLevel = false, charging = isCharging, powerSave = isSaving, limited = isLimited)
                            else -> BatteryRing(batteryLevel, displayColor ?: islandAccent(), showLevel = false, charging = isCharging, powerSave = isSaving, limited = isLimited)
                        }
                    },
                    start = "$batteryLevel%",
                    end = label,
                    endSlot = if (showShield) {
                        { IslandIcon(R.drawable.rounded_shield_filled_24, size = 18.dp, tint = islandAccent()) }
                    } else {
                        null
                    },
                )
            },
        )
        val showBattery = showBatteryItem && batteryLevel >= 0 && (
            !settings.isIslandBatteryOnlyLowEnabled() ||
                batteryLevel <= settings.getDuoBatteryLowLevel() ||
                System.currentTimeMillis() < peekUntil
            )
        publish(listOfNotNull(timeItem.takeIf { showTime }, batteryItem.takeIf { showBattery }))
    }

    private fun openClock() {
        try {
            context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    private fun idleColor(): Color? = batteryIdleColor(settings)

    private fun stateColor(): Color? = batteryStateColor(settings, BatteryStatus(level, charging, shielded, powerSave))

    private companion object {
        const val BATTERY_KEY = "battery"
    }
}
