package com.sameerasw.essentials.services.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.utils.AutoUpdateManagerHelper
import com.sameerasw.essentials.utils.UpdateNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DownloadUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.sameerasw.essentials.ACTION_DOWNLOAD_UPDATE") {
            val downloadUrl = intent.getStringExtra("download_url") ?: return
            val version = intent.getStringExtra("version") ?: ""

            val pendingResult = goAsync()
            val helper = AutoUpdateManagerHelper(context)
            val cleanVersion = version.replace(Regex("[^a-zA-Z0-9]"), "_")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    helper.downloadAndInstallApk(
                        apkUrl = downloadUrl,
                        apkName = "Essentials_$cleanVersion",
                        onProgressUpdate = { progress ->
                            UpdateNotificationHelper.showDownloadProgressNotification(context, version, progress)
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    UpdateNotificationHelper.cancelNotification(context)
                    pendingResult.finish()
                }
            }
        }
    }
}
