package com.sameerasw.essentials.utils

import AutoUpdaterManager
import android.content.Context
import com.example.autoupdater.UpdateFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoUpdateManagerHelper(private val context: Context) {
    private val autoUpdaterManager = AutoUpdaterManager(context)

    suspend fun checkForUpdate(jsonUrl: String): UpdateFeatures? = withContext(Dispatchers.IO) {
        try {
            autoUpdaterManager.checkForUpdate(jsonUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadAndInstallApk(
        apkUrl: String,
        apkName: String,
        onProgressUpdate: (Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            autoUpdaterManager.downloadapk(
                context = context,
                apkUrl = apkUrl,
                apkName = apkName,
                onProgressUpdate = onProgressUpdate
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCurrentAppVersionName(): String {
        return autoUpdaterManager.getCurrentAppVersionName()
    }
}
