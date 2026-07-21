package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.services.AppDetectionService
import com.sameerasw.essentials.services.BatteryNotificationService

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sameerasw.essentials.services.AppUpdateWorker
import java.util.concurrent.TimeUnit

object ServiceUtils {

    fun startRequiredServices(context: Context) {
        val settingsRepository = SettingsRepository(context)

        startAppDetectionServiceIfNeeded(context, settingsRepository)
        startBatteryNotificationServiceIfNeeded(context, settingsRepository)
        schedulePeriodicAppUpdateCheck(context, settingsRepository)
    }

    private fun startAppDetectionServiceIfNeeded(
        context: Context,
        settingsRepository: SettingsRepository
    ) {
        val isAppLockEnabled =
            settingsRepository.getBoolean(SettingsRepository.KEY_APP_LOCK_ENABLED)
        val isDynamicNightLightEnabled =
            settingsRepository.getBoolean(SettingsRepository.KEY_DYNAMIC_NIGHT_LIGHT_ENABLED)
        val isHideGestureBarOnLauncherEnabled =
            settingsRepository.getBoolean(SettingsRepository.KEY_HIDE_GESTURE_BAR_ON_LAUNCHER_ENABLED)
        val isUseUsageAccess =
            settingsRepository.getBoolean(SettingsRepository.KEY_USE_USAGE_ACCESS)

        val hasAppAutomations = DIYRepository.automations.value.any {
            it.isEnabled && it.type == Automation.Type.APP
        }

        val shutUpConfigs = settingsRepository.loadShutUpConfigs()
        val hasShutUpApps = shutUpConfigs.any { it.isEnabled }

        val shouldRun =
            (isUseUsageAccess && (isAppLockEnabled || isDynamicNightLightEnabled || isHideGestureBarOnLauncherEnabled || hasAppAutomations)) || hasShutUpApps

        val intent = Intent(context, AppDetectionService::class.java)
        if (shouldRun) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (AppDetectionService.isRunning) {
            context.stopService(intent)
        }
    }

    private fun startBatteryNotificationServiceIfNeeded(
        context: Context,
        settingsRepository: SettingsRepository
    ) {
        val isBatteryNotificationEnabled = settingsRepository.isBatteryNotificationEnabled()

        val intent = Intent(context, BatteryNotificationService::class.java)
        if (isBatteryNotificationEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun schedulePeriodicAppUpdateCheck(
        context: Context,
        settingsRepository: SettingsRepository
    ) {
        val isAutoUpdateEnabled = settingsRepository.getBoolean(
            SettingsRepository.KEY_AUTO_UPDATE_ENABLED,
            true
        )
        if (isAutoUpdateEnabled) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(
                12, TimeUnit.HOURS
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "app_update_check_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
