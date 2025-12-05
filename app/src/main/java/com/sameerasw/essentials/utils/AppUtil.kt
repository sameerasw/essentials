package com.sameerasw.essentials.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.domain.model.AppSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppUtil {
    private const val TAG = "AppUtil"

    /**
     * Get all installed apps (not just launcher apps)
     */
    suspend fun getInstalledApps(context: Context): List<NotificationApp> = withContext(Dispatchers.IO) {
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
                        isEnabled = true,
                        icon = pm.getApplicationIcon(appInfo),
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
     * Merge installed apps with saved app selections, keeping user settings and adding new apps
     */
    fun mergeWithSavedApps(
        installedApps: List<NotificationApp>,
        savedSelections: List<AppSelection>
    ): List<NotificationApp> {
        val savedSelectionsMap = savedSelections.associateBy { it.packageName }

        return installedApps.map { installedApp ->
            val savedSelection = savedSelectionsMap[installedApp.packageName]
            installedApp.copy(isEnabled = savedSelection?.isEnabled ?: true)
        }.sortedBy { it.appName.lowercase() }
    }
}
