package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.sameerasw.essentials.R

data class BatteryDetails(
    val level: Int,
    val scale: Int,
    val status: Int,
    val health: Int,
    val plugged: Int,
    val voltage: Int,
    val temperature: Int,
    val technology: String,
    val isPresent: Boolean,

    // Shell / sysfs / Android 14+ attributes
    val chargeFull: Long? = null,
    val chargeFullDesign: Long? = null,
    val chargeCounter: Long? = null,
    val maxChargingCurrent: Int? = null,
    val maxChargingVoltage: Int? = null,
    val chargingState: Int? = null,
    val chargingPolicy: Int? = null,
    val capacityLevel: Int? = null,
    val currentNow: Long? = null,
    val voltageNow: Long? = null,
    val powerProfile: Map<String, String>? = null,
    val batteryChargingEnforceLevel: Int? = null,

    // Android 14+ public & system APIs
    val cycleCount: Int? = null,
    val chargingStatusNew: Int? = null,
    val currentNowMa: Int? = null,
    val currentAvgMa: Int? = null,
    val remainingEnergyMwh: Long? = null,
    val chargeTimeRemainingMs: Long? = null,
    val stateOfHealth: Int? = null,
    val manufacturingDate: Long? = null,
    val firstUsageDate: Long? = null,
    val serialNumber: String? = null,
    val partStatus: Int? = null,
    val hasBatteryStatsPermission: Boolean = false,
    val thermalInfo: ThermalInfo? = null
)

object BatteryInfoUtil {

    fun getBatteryIntent(context: Context): Intent? {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        return context.registerReceiver(null, filter)
    }

    fun hasBatteryStatsPermission(context: Context): Boolean {
        return androidx.core.content.PermissionChecker.checkSelfPermission(
            context,
            "android.permission.BATTERY_STATS"
        ) == androidx.core.content.PermissionChecker.PERMISSION_GRANTED
    }

    fun getBasicDetails(context: Context): BatteryDetails {
        val intent = getBatteryIntent(context)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        val present = intent?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true) ?: true

        // Android 14+ public Extras
        val cycleCount = if (android.os.Build.VERSION.SDK_INT >= 34) {
            intent?.getIntExtra("android.os.extra.CYCLE_COUNT", -1)?.takeIf { it >= 0 }
        } else null

        val chargingStatusNew = if (android.os.Build.VERSION.SDK_INT >= 34) {
            intent?.getIntExtra("android.os.extra.CHARGING_STATUS", -1)?.takeIf { it >= 0 }
        } else null

        // BatteryManager Property queries
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val rawCurrentNow = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.takeIf { it != Int.MIN_VALUE }
        val currentNowMa = rawCurrentNow?.let { it / 1000 }

        val rawCurrentAvg = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)?.takeIf { it != Int.MIN_VALUE }
        val currentAvgMa = rawCurrentAvg?.let { it / 1000 }

        val rawEnergy = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)?.takeIf { it != Long.MIN_VALUE }
        val remainingEnergyMwh = rawEnergy?.let { it / 1_000_000 } // nWh to mWh

        val chargeTimeRemaining = bm?.computeChargeTimeRemaining()?.takeIf { it >= 0 }

        val hasStatsPerm = hasBatteryStatsPermission(context)

        var stateOfHealth: Int? = null
        var mfgDate: Long? = null
        var firstUseDate: Long? = null
        var serialNum: String? = null
        var partStat: Int? = null

        if (android.os.Build.VERSION.SDK_INT >= 34 && hasStatsPerm && bm != null) {
            // Property IDs: 10: SoH %, 7: Mfg date, 8: First usage date, 11: Serial number, 12: Part status
            val soh = bm.getIntProperty(10)
            if (soh in 1..100) stateOfHealth = soh

            val mfg = bm.getLongProperty(7)
            if (mfg > 0 && mfg != Long.MIN_VALUE) mfgDate = mfg

            val firstUse = bm.getLongProperty(8)
            if (firstUse > 0 && firstUse != Long.MIN_VALUE) firstUseDate = firstUse

            try {
                val getStringPropMethod = bm.javaClass.getMethod("getStringProperty", Int::class.javaPrimitiveType)
                serialNum = getStringPropMethod.invoke(bm, 11) as? String
            } catch (e: Exception) {
            }

            val part = bm.getIntProperty(12)
            if (part > 0 && part != Integer.MIN_VALUE) partStat = part
        }

        return BatteryDetails(
            level = level,
            scale = scale,
            status = status,
            health = health,
            plugged = plugged,
            voltage = voltage,
            temperature = temp,
            technology = tech,
            isPresent = present,
            cycleCount = cycleCount,
            chargingStatusNew = chargingStatusNew,
            currentNowMa = currentNowMa,
            currentAvgMa = currentAvgMa,
            remainingEnergyMwh = remainingEnergyMwh,
            chargeTimeRemainingMs = chargeTimeRemaining,
            stateOfHealth = stateOfHealth,
            manufacturingDate = mfgDate,
            firstUsageDate = firstUseDate,
            serialNumber = serialNum,
            partStatus = partStat,
            hasBatteryStatsPermission = hasStatsPerm
        )
    }

    fun fetchAdvancedDetails(context: Context, basic: BatteryDetails): BatteryDetails {
        if (!ShellUtils.hasPermission(context)) return basic

        var chargeFull: Long? = readSysfsLong(context, "/sys/class/power_supply/battery/charge_full")
        var chargeFullDesign: Long? = readSysfsLong(context, "/sys/class/power_supply/battery/charge_full_design")
        var currentNow: Long? = readSysfsLong(context, "/sys/class/power_supply/battery/current_now")
        var voltageNow: Long? = readSysfsLong(context, "/sys/class/power_supply/battery/voltage_now")

        val dumpsysOutput = ShellUtils.runCommandWithOutput(context, "dumpsys battery")
        val dumpsysMap = parseDumpsysBattery(dumpsysOutput)

        if (chargeFull == null) {
            dumpsysMap["Charge counter"]?.toLongOrNull()?.let {
                // dumpsys sometimes gives charge counter or max capacity
            }
        }

        val chargeCounter = dumpsysMap["Charge counter"]?.toLongOrNull() ?: readSysfsLong(context, "/sys/class/power_supply/battery/charge_counter")
        val maxChargingCurrent = dumpsysMap["Max charging current"]?.toIntOrNull()
        val maxChargingVoltage = dumpsysMap["Max charging voltage"]?.toIntOrNull()
        val chargingState = dumpsysMap["Charging state"]?.toIntOrNull()
        val chargingPolicy = dumpsysMap["Charging policy"]?.toIntOrNull()
        val capacityLevel = dumpsysMap["Capacity level"]?.toIntOrNull()

        val dumpsysSerial = dumpsysMap["Serial number"] ?: dumpsysMap["serial_number"] ?: dumpsysMap["Serial Number"]
        val dumpsysPart = dumpsysMap["Part status"]?.toIntOrNull() ?: dumpsysMap["part_status"]?.toIntOrNull() ?: dumpsysMap["Part Status"]?.toIntOrNull()

        val powerProfileOutput = ShellUtils.runCommandWithOutput(context, "dumpsys batterystats --power-profile")
        val powerProfileMap = parsePowerProfile(powerProfileOutput)

        val settingsOutput = ShellUtils.runCommandWithOutput(context, "dumpsys batterystats --settings")
        val enforceLevel = parseSettingsEnforceLevel(settingsOutput)

        val thermalInfo = ThermalUtil.getThermalInfo(context)

        return basic.copy(
            chargeFull = chargeFull,
            chargeFullDesign = chargeFullDesign,
            chargeCounter = chargeCounter,
            maxChargingCurrent = maxChargingCurrent,
            maxChargingVoltage = maxChargingVoltage,
            chargingState = chargingState,
            chargingPolicy = chargingPolicy,
            capacityLevel = capacityLevel,
            currentNow = currentNow,
            voltageNow = voltageNow,
            powerProfile = powerProfileMap.takeIf { it.isNotEmpty() },
            batteryChargingEnforceLevel = enforceLevel,
            serialNumber = basic.serialNumber ?: dumpsysSerial,
            partStatus = basic.partStatus ?: dumpsysPart,
            thermalInfo = thermalInfo
        )
    }

    private fun parsePowerProfile(output: String?): Map<String, String> {
        if (output.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        output.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    map[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        return map
    }

    private fun parseSettingsEnforceLevel(output: String?): Int? {
        if (output.isNullOrBlank()) return null
        output.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("battery_charging_enforce_level=")) {
                return trimmed.substringAfter("=").trim().toIntOrNull()
            }
        }
        return null
    }

    private fun readSysfsLong(context: Context, path: String): Long? {
        val out = ShellUtils.runCommandWithOutput(context, "cat $path") ?: return null
        return out.trim().toLongOrNull()
    }

    private fun parseDumpsysBattery(output: String?): Map<String, String> {
        if (output.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        output.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    map[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        return map
    }

    fun getBatteryIconRes(
        context: Context,
        level: Int,
        isCharging: Boolean,
        status: Int = BatteryManager.BATTERY_STATUS_UNKNOWN,
        health: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
        isPresent: Boolean = true,
        isPowerSave: Boolean = false
    ): Int {
        if (!isPresent || health == BatteryManager.BATTERY_HEALTH_OVERHEAT || health == BatteryManager.BATTERY_HEALTH_DEAD || health == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE || health == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE) {
            return R.drawable.battery_android_frame_alert_24px
        }
        val isChargeLimitEnabled = try {
            android.provider.Settings.Secure.getInt(context.contentResolver, "charge_optimization_mode", 0) == 1
        } catch (e: Exception) {
            false
        }
        if (isCharging && level >= 80 && isChargeLimitEnabled) {
            return R.drawable.battery_android_frame_shield_24px
        }
        if (level >= 100) {
            return R.drawable.battery_android_frame_full_24px
        }
        if (isCharging) {
            return R.drawable.battery_android_frame_bolt_24px
        }
        if (isPowerSave) {
            return R.drawable.battery_android_frame_plus_24px
        }
        return when {
            level <= 0 -> R.drawable.battery_android_0_24px
            level <= 15 -> R.drawable.battery_android_frame_1_24px
            level <= 30 -> R.drawable.battery_android_frame_2_24px
            level <= 45 -> R.drawable.battery_android_frame_3_24px
            level <= 60 -> R.drawable.battery_android_frame_4_24px
            level <= 80 -> R.drawable.battery_android_frame_5_24px
            else -> R.drawable.battery_android_frame_6_24px
        }
    }

    fun formatStatus(status: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }
    }

    fun formatHealth(health: Int): String {
        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }

    fun formatPlugged(plugged: Int): String {
        return when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            0 -> "Unplugged"
            else -> "Plugged"
        }
    }

    fun formatChargingPolicy(policy: Int?): String {
        return when (policy) {
            1 -> "Not optimized"
            2 -> "Limited capacity"
            3 -> "Adaptive charging"
            else -> policy?.toString() ?: "Unknown"
        }
    }

    fun formatChargingStatusNew(status: Int?): String {
        return when (status) {
            1 -> "Unknown"
            2 -> "Charging"
            3 -> "Discharging"
            4 -> "Not Charging"
            5 -> "Full"
            else -> status?.toString() ?: "Unknown"
        }
    }

    fun formatChargeTimeRemaining(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    fun formatDate(epochSeconds: Long): Pair<String, Boolean> {
        val date = java.util.Date(epochSeconds * 1000)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val formatted = sdf.format(date)
        
        // Suspicious / sentinel check (e.g. 2020-12-01 default or Unix epoch 1970-01-01)
        val cal = java.util.Calendar.getInstance().apply { time = date }
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) // 0-indexed, Dec = 11
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)

        val isSuspicious = (year < 2021) || (year == 2020 && month == 11 && day == 1)
        return Pair(formatted, isSuspicious)
    }

    fun formatPartStatus(status: Int?): String {
        return when (status) {
            1 -> "Original"
            2 -> "Replaced"
            0 -> "Unsupported"
            else -> status?.toString() ?: "Unknown"
        }
    }

    fun formatCapacityLevel(level: Int?): String {
        return when (level) {
            1 -> "Critical"
            2 -> "Low"
            3 -> "Normal"
            4 -> "High"
            5 -> "Full"
            0 -> "Unknown"
            -1 -> "Unsupported"
            else -> level?.toString() ?: "Unknown"
        }
    }
}
