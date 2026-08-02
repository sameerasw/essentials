package com.sameerasw.essentials.utils

import android.content.Context

data class ThermalItem(
    val name: String,
    val value: Float,
    val type: Int,
    val status: Int
)

data class ThermalInfo(
    val items: List<ThermalItem> = emptyList(),
    val maxCpuTemp: Float? = null,
    val maxGpuTemp: Float? = null,
    val batteryTemp: Float? = null,
    val skinTemp: Float? = null,
    val maxThrottlingStatus: Int = 0
) {
    companion object {
        const val THROTTLING_NONE = 0
        const val THROTTLING_LIGHT = 1
        const val THROTTLING_MODERATE = 2
        const val THROTTLING_SEVERE = 3
        const val THROTTLING_CRITICAL = 4
        const val THROTTLING_EMERGENCY = 5
        const val THROTTLING_SHUTDOWN = 6

        const val TYPE_UNKNOWN = 0
        const val TYPE_CPU = 1
        const val TYPE_GPU = 2
        const val TYPE_BATTERY = 3
        const val TYPE_SKIN = 4
        const val TYPE_USB_PORT = 5
        const val TYPE_POWER_AMPLIFIER = 6
        const val TYPE_BCL_VOLTAGE = 7
        const val TYPE_BCL_CURRENT = 8
        const val TYPE_BCL_PERCENTAGE = 9
        const val TYPE_NPU = 10
        const val TYPE_TPU = 11
        const val TYPE_DISPLAY = 12
        const val TYPE_MODEM = 13
        const val TYPE_SOC = 14
        const val TYPE_WIFI = 15
        const val TYPE_CAMERA = 16
        const val TYPE_FLASHLIGHT = 17
        const val TYPE_SPEAKER = 18
        const val TYPE_AMBIENT = 19
        const val TYPE_POGO = 20
    }
}

object ThermalUtil {

    fun getThermalInfo(context: Context): ThermalInfo? {
        if (!ShellUtils.hasPermission(context)) return null

        val output = ShellUtils.runCommandWithOutput(context, "dumpsys thermalservice") ?: return null
        return parseDumpsysThermalService(output)
    }

    fun parseDumpsysThermalService(output: String): ThermalInfo {
        val items = mutableListOf<ThermalItem>()
        var maxThrottling = 0

        // Format in dumpsys thermalservice:
        // Temperature{mValue=35.2, mType=3, mName=battery, mStatus=0}
        val regex = Regex("""Temperature\{mValue=([0-9.]+),\s*mType=([0-9]+),\s*mName=([^,]+),\s*mStatus=([0-9]+)\}""")

        output.lines().forEach { line ->
            regex.find(line)?.let { match ->
                val (valStr, typeStr, name, statusStr) = match.destructured
                val value = valStr.toFloatOrNull() ?: 0f
                val type = typeStr.toIntOrNull() ?: 0
                val status = statusStr.toIntOrNull() ?: 0

                if (status > maxThrottling) {
                    maxThrottling = status
                }

                items.add(ThermalItem(name = name, value = value, type = type, status = status))
            }
        }

        if (items.isEmpty()) return ThermalInfo()

        val cpuTemps = items.filter { it.type == ThermalInfo.TYPE_CPU }.map { it.value }
        val gpuTemps = items.filter { it.type == ThermalInfo.TYPE_GPU }.map { it.value }
        val batteryTemps = items.filter { it.type == ThermalInfo.TYPE_BATTERY }.map { it.value }
        val skinTemps = items.filter { it.type == ThermalInfo.TYPE_SKIN }.map { it.value }

        val maxCpu = if (cpuTemps.isNotEmpty()) cpuTemps.maxOrNull() else null
        val maxGpu = if (gpuTemps.isNotEmpty()) gpuTemps.maxOrNull() else null
        val battery = if (batteryTemps.isNotEmpty()) batteryTemps.maxOrNull() else null
        val skin = if (skinTemps.isNotEmpty()) skinTemps.maxOrNull() else null

        return ThermalInfo(
            items = items,
            maxCpuTemp = maxCpu,
            maxGpuTemp = maxGpu,
            batteryTemp = battery,
            skinTemp = skin,
            maxThrottlingStatus = maxThrottling
        )
    }
}
