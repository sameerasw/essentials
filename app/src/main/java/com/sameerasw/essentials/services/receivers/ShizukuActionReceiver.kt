package com.sameerasw.essentials.services.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository

class ShizukuActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.sameerasw.essentials.ACTION_RESTART_SHIZUKU") {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(9001)

            val settingsRepository = SettingsRepository(context)
            val token = settingsRepository.getShizukuAuthToken()

            if (token.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.toast_enter_shizuku_token),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                try {
                    val shizukuIntent = Intent("moe.shizuku.privileged.api.START").apply {
                        `package` = "moe.shizuku.privileged.api"
                        putExtra("auth", token)
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                    context.sendBroadcast(shizukuIntent)
                } catch (e: Exception) {
                    Log.e("ShizukuActionReceiver", "Failed to restart Shizuku", e)
                }
            }
        }
    }
}
