package com.sameerasw.essentials.utils

import com.sameerasw.essentials.R

object DeviceImageMapper {

    /**
     * Maps the Android version to a specific logo.
     */
    fun getAndroidLogo(deviceInfo: DeviceInfo): Int {
        val osName = deviceInfo.osCodename.lowercase()
        val sdk = deviceInfo.sdkInt

        return when {
            osName.contains("android 17") || osName.contains("cinnamonbun") || sdk >= 37 -> R.drawable.android17
            osName.contains("android 16") || osName.contains("baklava") || sdk >= 36 -> R.drawable.android16
            osName.contains("android 15") || osName.contains("vanilla") || sdk >= 35 -> R.drawable.android15
            osName.contains("android 14") || osName.contains("upside") || sdk >= 34 -> R.drawable.android14
            osName.contains("android 13") || osName.contains("tiramisu") || sdk >= 33 -> R.drawable.android13
            osName.contains("android 12") || osName.contains("snow cone") || sdk >= 31 -> R.drawable.android12
            else -> 0
        }
    }
}
