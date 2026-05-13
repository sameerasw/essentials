package com.sameerasw.essentials.utils

import com.sameerasw.essentials.R

object DeviceImageMapper {
    /**
     * Maps a device model string to a corresponding drawable resource.
     *
     * @param model The device model (e.g., from Build.MODEL)
     * @return The resource ID of the matching drawable vector illustration.
     */
    fun getDeviceDrawable(model: String): Int {
        val m = model.lowercase()
        return when {
            // Pixel 6 series
            m.contains("pixel 6a") -> R.drawable.pixel_6a
            m.contains("pixel 6 pro") -> R.drawable.pixel_6pro
            m.contains("pixel 6") -> R.drawable.pixel_6

            // Pixel 7 series
            m.contains("pixel 7a") -> R.drawable.pixel_7a
            m.contains("pixel 7 pro") -> R.drawable.pixel_7pro
            m.contains("pixel 7") -> R.drawable.pixel_7

            // Pixel 8 series
            m.contains("pixel 8a") -> R.drawable.pixel_8a
            m.contains("pixel 8 pro") -> R.drawable.pixel_8pro

            // Pixel 9 & 10 series
            m.contains("pixel 9a") || m.contains("pixel 10a") -> R.drawable.pixel_9a_10a
            m.contains("pixel 9 pro") || m.contains("pixel 9 pro xl") ||
                    m.contains("pixel 10") || m.contains("pixel 10 pro") || m.contains("pixel 10 pro xl") ->
                R.drawable.pixel_9pro_9proxl_10_10pro_10proxl

            m.contains("pixel 9") -> R.drawable.pixel_9

            // Default fallback
            else -> 0
        }
    }

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
