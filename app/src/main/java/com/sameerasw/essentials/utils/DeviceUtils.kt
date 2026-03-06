package com.sameerasw.essentials.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import org.json.JSONArray

data class DeviceInfo(
    val deviceName: String,
    val brand: String = Build.BRAND,
    val model: String = Build.MODEL,
    val device: String = Build.DEVICE,
    val hardware: String = Build.HARDWARE,
    val product: String = Build.PRODUCT,
    val androidVersion: String = Build.VERSION.RELEASE,
    val sdkInt: Int = Build.VERSION.SDK_INT,
    val manufacturer: String = Build.MANUFACTURER,
    val board: String = Build.BOARD,
    val display: String = Build.DISPLAY,
    val fingerprint: String = Build.FINGERPRINT,
    val totalStorage: Long,
    val availableStorage: Long,
    val totalRam: Long,
    val availableRam: Long,
    val securityPatch: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown",
    val osCodename: String = Build.VERSION.CODENAME,
    val buildTag: String = "",
    val supportedDevices: String = ""
)

object DeviceUtils {
    fun getDeviceInfo(context: Context): DeviceInfo {
        val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                ?: Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                ?: Build.MODEL
        } else {
            Settings.Secure.getString(context.contentResolver, "bluetooth_name") ?: Build.MODEL
        }

        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalStorage = stat.blockCountLong * blockSize
        val availableStorage = stat.availableBlocksLong * blockSize

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val buildId = Build.DISPLAY
        val buildInfo = findBuildInfo(context, buildId)

        val deviceSecurityPatch =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown"
        val deviceOsCodename = Build.VERSION.CODENAME
        val matchedVersion = buildInfo?.optString("version")

        val androidVersion = matchedVersion?.let {
            if (it.startsWith("Android ")) {
                val v = it.removePrefix("Android ").substringBefore(" ")
                if (v.firstOrNull()?.isDigit() == true) v else null
            } else null
        } ?: Build.VERSION.RELEASE

        return DeviceInfo(
            deviceName = deviceName,
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            totalRam = memoryInfo.totalMem,
            availableRam = memoryInfo.availMem,
            androidVersion = androidVersion,
            securityPatch = buildInfo?.optString("patch")?.takeIf { it.isNotBlank() }
                ?: deviceSecurityPatch,
            osCodename = matchedVersion?.takeIf { it.isNotBlank() } ?: deviceOsCodename,
            buildTag = buildInfo?.optString("tag") ?: "",
            supportedDevices = buildInfo?.optString("devices") ?: ""
        )
    }

    private fun findBuildInfo(context: Context, buildId: String): org.json.JSONObject? {
        return try {
            val jsonString =
                context.assets.open("android_builds.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                if (obj.getString("build_id").equals(buildId, ignoreCase = true)) {
                    return obj
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun formatSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return java.lang.String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    fun getOSName(sdkInt: Int, defaultCodename: String): String {
        // Try to determine by version string first (most accurate for betas)
        val name = defaultCodename.lowercase()
        if (name.contains("17") || name.contains("cinnamon")) return "CinnamonBun"
        if (name.contains("16") || name.contains("baklava")) return "Baklava"

        val dessert = when (sdkInt) {
            37 -> "CinnamonBun"
            36 -> "Baklava"
            35 -> "Vanilla Ice Cream"
            34 -> "Upside Down Cake"
            33 -> "Tiramisu"
            32 -> "Snow Cone v2"
            31 -> "Snow Cone"
            30 -> "R"
            29 -> "Q"
            28 -> "Pie"
            27 -> "Oreo"
            26 -> "Oreo"
            else -> null
        }

        if (dessert != null) return dessert

        // If SDK mapping fails, try to use the provided codename
        if (defaultCodename.contains("Beta") || defaultCodename.contains("Canary") || defaultCodename.contains(
                "QPR"
            )
        ) {
            return defaultCodename
        }

        return defaultCodename.takeIf { it != "REL" } ?: "Android"
    }

    fun formatHardwareSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "Unknown"
        val rawGb = sizeBytes / (1024.0 * 1024.0 * 1024.0)

        // Known standard hardware sizes in GB
        val standardSizes =
            listOf(1, 2, 3, 4, 6, 8, 10, 12, 16, 18, 24, 32, 64, 128, 256, 512, 1024, 2048)

        // Find the closest standard size that is greater than or equal to our raw GB (allowing a 10% delta for OS reservations)
        val roundedGb = standardSizes.firstOrNull { it >= rawGb * 0.9 } ?: Math.ceil(rawGb).toInt()

        return if (roundedGb >= 1024 && roundedGb % 1024 == 0) {
            "${roundedGb / 1024} TB"
        } else {
            "$roundedGb GB"
        }
    }

    fun formatSecurityPatch(patchString: String): String {
        if (patchString == "Unknown" || patchString.isBlank()) return patchString
        return try {
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val formatter =
                java.text.SimpleDateFormat("d MMMM, yyyy", java.util.Locale.getDefault())
            val date = parser.parse(patchString)
            if (date != null) formatter.format(date) else patchString
        } catch (e: Exception) {
            patchString
        }
    }

    fun isBlurProblematicDevice(): Boolean {
        // Samsung devices on One UI 7 (Android 15) or below have a broken blur implementation
        // that causes a gray screen overlay. Disable it for them. (╯°□°）╯︵ ┻━┻
        return Build.MANUFACTURER.equalsIgnoreCase("samsung") && 
                Build.VERSION.SDK_INT <= 35 // Android 15
    }

    fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        return powerManager?.isPowerSaveMode == true
    }

    private fun String.equalsIgnoreCase(other: String): Boolean {
        return this.equals(other, ignoreCase = true)
    }
}
