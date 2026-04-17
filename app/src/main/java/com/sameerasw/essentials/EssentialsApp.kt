package com.sameerasw.essentials

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.ShizukuUtils
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

class EssentialsApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    private val securityReceiver = com.sameerasw.essentials.services.receivers.SecurityReceiver()

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        
        try {
            resources?.configuration
        } catch (e: Exception) {
            
        }

        ShizukuUtils.initialize()
        com.sameerasw.essentials.utils.LogManager.init(this)

        // Init Automation
        com.sameerasw.essentials.domain.diy.DIYRepository.init(this)
        com.sameerasw.essentials.services.automation.AutomationManager.init(this)
        com.sameerasw.essentials.services.CalendarSyncManager.init(this)

        initSentry()

        val intentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(securityReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(securityReceiver, intentFilter)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(securityReceiver)
    }

    private fun initSentry() {
        val repository = SettingsRepository(this)
        val mode = repository.getString(SettingsRepository.KEY_SENTRY_REPORT_MODE, "auto")

        if (mode == "off") return

        SentryAndroid.init(this) { options ->
            options.dsn = "https://e105699467efe3a43a16bfbad3a63b33@o4510996760887296.ingest.de.sentry.io/4510996763312208"
            options.isEnabled = true
            
            options.setBeforeSend { event, _ ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this@EssentialsApp, R.string.sentry_crash_toast, Toast.LENGTH_LONG).show()
                }
                event
            }
        }
    }

}

