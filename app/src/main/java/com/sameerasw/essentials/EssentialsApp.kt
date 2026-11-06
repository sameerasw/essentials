/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Application Entrypoint
 * File: EssentialsApp.kt
 * Description: Main application entrypoint for EssentialsApp.kt.
 */

package com.sameerasw.essentials

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.services.receivers.SecurityReceiver
import com.sameerasw.essentials.services.widgets.BatteriesWidgetReceiver
import com.sameerasw.essentials.services.widgets.BatteryWidgetJobService
import com.sameerasw.essentials.utils.ShizukuUtils
import eu.darken.porter.bridge.PorterShizukuBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import io.sentry.android.core.SentryAndroid
import org.lsposed.hiddenapibypass.HiddenApiBypass

class EssentialsApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    private val securityReceiver = SecurityReceiver()

    private val batteryWidgetReceiver = object : BroadcastReceiver() {
        private var lastLevel = -1
        private var lastCharging: Boolean? = null

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == Intent.ACTION_BATTERY_CHANGED ||
                action == Intent.ACTION_POWER_CONNECTED ||
                action == Intent.ACTION_POWER_DISCONNECTED
            ) {
                val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                val level = if (rawLevel >= 0 && scale > 0) (rawLevel * 100 / scale) else -1
                val isCharging = when (action) {
                    Intent.ACTION_POWER_CONNECTED -> true
                    Intent.ACTION_POWER_DISCONNECTED -> false
                    else -> plugged > 0 || status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                }

                if (level != lastLevel || isCharging != lastCharging) {
                    lastLevel = level
                    lastCharging = isCharging
                    BatteriesWidgetReceiver.updateWidgets(context)
                    if (isCharging == false) {
                        BatteryWidgetJobService.schedule(context)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        com.sameerasw.essentials.data.repository.SettingsRepository(this).syncTorchRestriction()
        context = applicationContext
        com.sameerasw.essentials.utils.HapticUtil.initialize(this)

        try {
            resources?.configuration
        } catch (e: Exception) {
        }

        try {
            HiddenApiBypass.setHiddenApiExemptions("")
        } catch (_: Throwable) {
        }

        PorterShizukuBridge.start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        ShizukuUtils.initialize()
        com.sameerasw.essentials.utils.CarConnectionMonitor
            .initialize(this)
        com.sameerasw.essentials.utils.LogManager
            .init(this)

        // Init Automation
        com.sameerasw.essentials.domain.diy.DIYRepository
            .init(this)
        com.sameerasw.essentials.services.automation.AutomationManager
            .init(this)
        com.sameerasw.essentials.services.CalendarSyncManager
            .init(this)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            com.sameerasw.essentials.services.DeviceInfoSyncManager
                .init(this@EssentialsApp)
        }
        com.sameerasw.essentials.utils.ServiceUtils
            .startRequiredServices(this)

        com.sameerasw.essentials.appfunctions.AppFunctionAvailabilityManager
            .updateAvailability(this)

        initSentry()

        val intentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(securityReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(securityReceiver, intentFilter)
        }

        val batteryFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(batteryWidgetReceiver, batteryFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(batteryWidgetReceiver, batteryFilter)
        }

        BatteryWidgetJobService.schedule(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            unregisterReceiver(securityReceiver)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(batteryWidgetReceiver)
        } catch (_: Exception) {}
    }

    private fun initSentry() {
        val repository = SettingsRepository(this)
        val mode = repository.getString(SettingsRepository.KEY_SENTRY_REPORT_MODE, "auto")

        if (mode == "off") return

        SentryAndroid.init(this) { options ->
            options.dsn =
                "https://e105699467efe3a43a16bfbad3a63b33@o4510996760887296.ingest.de.sentry.io/4510996763312208"
            options.isEnabled = true

            options.setBeforeSend { event, hint ->
                try {
                    val throwable = event.throwable
                    val threadName = event.threads?.firstOrNull()?.name ?: Thread.currentThread().name
                    val message = event.message?.formatted ?: event.exceptions?.firstOrNull()?.value
                    com.sameerasw.essentials.utils.LogManager.saveCrashReport(
                        context = this@EssentialsApp,
                        threadName = threadName,
                        throwable = throwable,
                        customMessage = message,
                    )
                } catch (e: Exception) {
                    android.util.Log.e("EssentialsApp", "Failed to save crash report locally in Sentry callback", e)
                }

                Handler(Looper.getMainLooper()).post {
                    Toast
                        .makeText(
                            this@EssentialsApp,
                            R.string.sentry_crash_toast,
                            Toast.LENGTH_LONG,
                        ).show()
                }
                event
            }
        }
    }
}
