package com.sameerasw.essentials.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import com.sameerasw.essentials.domain.model.AppSelection
import com.sameerasw.essentials.domain.model.NotificationApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUtil {
    private const val TAG = "AppUtil"

    // Cache for extracted brand colors
    private val colorCache = mutableMapOf<String, Int>()

    // Cache for app icons to prevent repeated system calls
    private val iconCache = mutableMapOf<String, Bitmap>()

    // Target size for app icons to balance quality and performance
    private const val ICON_SIZE = 64

    /**
     * Get all installed apps (not just launcher apps)
     */
    suspend fun getInstalledApps(context: Context): List<NotificationApp> =
        withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager

                // Get all installed applications
                val allApps = pm.getInstalledApplications(0)
                    .filter { appInfo ->
                        // Filter out our own app
                        !appInfo.packageName.contains("essentials")
                    }

                val apps = allApps.mapNotNull { appInfo ->
                    try {
                        // More accurate system app detection
                        val flags = appInfo.flags
                        val isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                        val app = NotificationApp(
                            packageName = appInfo.packageName,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            isEnabled = false,
                            icon = getLowQualityIcon(context, appInfo.packageName).asImageBitmap(),
                            isSystemApp = isSystemApp,
                            lastUpdated = System.currentTimeMillis()
                        )
                        app
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading app ${appInfo.packageName}: ${e.message}")
                        null
                    }
                }


                // Log some examples
                apps.filter { !it.isSystemApp }.take(5).map { it.appName }
                apps.filter { it.isSystemApp }.take(5).map { it.appName }

                apps.sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting installed apps: ${e.message}")
                emptyList()
            }
        }

    /**
     * Get specific apps by package names (more efficient for picked lists)
     */
    suspend fun getAppsByPackageNames(
        context: Context,
        packageNames: List<String>
    ): List<NotificationApp> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val apps = packageNames.mapNotNull { packageName ->
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val flags = appInfo.flags
                    val isSystemApp = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                            (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

                    NotificationApp(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        isEnabled = false,
                        icon = getLowQualityIcon(context, packageName).asImageBitmap(),
                        isSystemApp = isSystemApp,
                        lastUpdated = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error loading app $packageName: ${e.message}")
                    null
                }
            }
            apps.sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting apps by package name: ${e.message}")
            emptyList()
        }
    }

    /**
     * Merge installed apps with saved app selections, keeping user settings and adding new apps
     */
    fun mergeWithSavedApps(
        installedApps: List<NotificationApp>,
        savedSelections: List<AppSelection>,
        defaultEnabled: Boolean = false
    ): List<NotificationApp> {
        val savedSelectionsMap = savedSelections.associateBy { it.packageName }

        return installedApps.map { installedApp ->
            val savedSelection = savedSelectionsMap[installedApp.packageName]
            installedApp.copy(isEnabled = savedSelection?.isEnabled ?: defaultEnabled)
        }.sortedBy { it.appName.lowercase() }
    }

    /**
     * Extracts the brand color from an app's icon using the Palette API.
     * Uses internal cache for efficiency.
     */
    fun getAppBrandColor(context: Context, packageName: String, callback: (Int) -> Unit) {
        // Check cache first
        colorCache[packageName]?.let {
            callback(it)
            return
        }

        try {
            context.packageManager
            // Extract bitmap from drawable, handling AdaptiveIcons
            val bitmap = getLowQualityIcon(context, packageName)

            // Generate palette asynchronously
            Palette.from(bitmap).generate { palette ->
                val color = palette?.getVibrantColor(Color.TRANSPARENT)
                    ?: palette?.getDominantColor(Color.GRAY)
                    ?: Color.GRAY

                // Cache the result
                colorCache[packageName] = color
                callback(color)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting color for $packageName: ${e.message}")
            callback(Color.GRAY)
        }
    }

    /**
     * Helper to load and scale an app icon to a lower resolution for better performance.
     */
    private fun getLowQualityIcon(context: Context, packageName: String): Bitmap {
        // Check cache first
        iconCache[packageName]?.let { return it }

        val drawable = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            context.packageManager.defaultActivityIcon
        }

        val bitmap = when (drawable) {
            is BitmapDrawable -> {
                val b = drawable.bitmap
                if (b.width > ICON_SIZE || b.height > ICON_SIZE) {
                    Bitmap.createScaledBitmap(b, ICON_SIZE, ICON_SIZE, true)
                } else {
                    b
                }
            }

            else -> {
                val bmp = createBitmap(ICON_SIZE, ICON_SIZE)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, ICON_SIZE, ICON_SIZE)
                drawable.draw(canvas)
                bmp
            }
        }

        // Cache the result
        iconCache[packageName] = bitmap
        return bitmap
    }

    fun getAppVersion(context: Context, packageName: String): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: Exception) {
            null
        }
    }
}
