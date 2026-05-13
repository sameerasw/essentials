package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.services.AppDetectionService
import com.sameerasw.essentials.services.BatteryNotificationService

object ServiceUtils {

    fun startRequiredServices(context: Context) {
        val settingsRepository = SettingsRepository(context)

        startAppDetectionServiceIfNeeded(context, settingsRepository)
        startBatteryNotificationServiceIfNeeded(context, settingsRepository)
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
            isUseUsageAccess && (isAppLockEnabled || isDynamicNightLightEnabled || isHideGestureBarOnLauncherEnabled || hasAppAutomations || hasShutUpApps)

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
}
