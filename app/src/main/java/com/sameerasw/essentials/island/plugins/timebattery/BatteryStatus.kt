package com.sameerasw.essentials.island.plugins.timebattery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.ui.graphics.Color
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.plugins.soften
import com.sameerasw.essentials.utils.battery.ChargeLimitUtil

data class BatteryStatus(
    val level: Int,
    val charging: Boolean,
    val limited: Boolean,
    val powerSave: Boolean,
)

private val AUTO_CHARGING = AndroidColor.rgb(0, 230, 118)

fun readBatteryStatus(context: Context, intent: Intent? = null): BatteryStatus? {
    val batteryIntent = intent ?: context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
    val raw = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    if (raw < 0 || scale <= 0) return null
    val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val level = (raw * 100f / scale).toInt()
    val plugged = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    val limited = ChargeLimitUtil.isShielded(context, level, plugged)
    return BatteryStatus(
        level = level,
        charging = plugged && !limited,
        limited = limited,
        powerSave = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false,
    )
}

fun batteryStateColor(settings: SettingsRepository, status: BatteryStatus): Color? {
    fun parse(hex: String, fallback: Int) = try {
        AndroidColor.parseColor(hex)
    } catch (_: Exception) {
        fallback
    }
    val charge = settings.getDuoBatteryChargingColor()
    val level = status.level
    val raw = when {
        status.charging && settings.isDuoBatteryChargingColorEnabled() ->
            if (charge.equals("auto", ignoreCase = true)) AUTO_CHARGING else parse(charge, AUTO_CHARGING)
        status.powerSave && settings.isDuoBatteryPowerSaveColorEnabled() ->
            parse(settings.getDuoBatteryPowerSaveColor(), AndroidColor.rgb(255, 152, 0))
        level in 0..settings.getDuoBatteryCriticalLevel() && settings.isDuoBatteryCriticalColorEnabled() ->
            parse(settings.getDuoBatteryCriticalColor(), AndroidColor.rgb(244, 67, 54))
        level in 0..settings.getDuoBatteryLowLevel() && settings.isDuoBatteryLowColorEnabled() ->
            parse(settings.getDuoBatteryLowColor(), AndroidColor.rgb(255, 235, 59))
        else -> return null
    }
    return Color(soften(raw))
}

fun batteryIdleColor(settings: SettingsRepository): Color? {
    if (!settings.isIslandBatteryIdleColorEnabled()) return null
    val raw = try {
        AndroidColor.parseColor(settings.getIslandBatteryIdleColor())
    } catch (_: Exception) {
        AndroidColor.WHITE
    }
    return Color(soften(raw))
}
