package com.sameerasw.essentials.utils.battery

import android.content.Context
import com.sameerasw.essentials.utils.DeviceUtils

object ChargeLimitUtil {
    private const val SHIELD_LEVEL = 78

    fun isLimitActive(context: Context): Boolean =
        DeviceUtils.isGoogleDevice() && ChargingModeUtil.getMode(context) == ChargingMode.LIMITED

    fun isShielded(
        context: Context,
        level: Int,
        charging: Boolean,
    ): Boolean = charging && level >= SHIELD_LEVEL && isLimitActive(context)
}
