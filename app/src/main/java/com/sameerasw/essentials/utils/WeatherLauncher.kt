package com.sameerasw.essentials.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.sameerasw.essentials.data.repository.SettingsRepository

object WeatherLauncher {
    private const val ALIAS = "com.sameerasw.essentials.WeatherLauncher"

    private fun component(context: Context) = ComponentName(context.packageName, ALIAS)

    fun isVisible(context: Context): Boolean = SettingsRepository(context).getBoolean(SettingsRepository.KEY_WEATHER_SHOW_IN_LAUNCHER, true)

    fun setVisible(context: Context, visible: Boolean) {
        SettingsRepository(context).putBoolean(SettingsRepository.KEY_WEATHER_SHOW_IN_LAUNCHER, visible)
        apply(context)
    }

    fun apply(context: Context) {
        val state = if (isVisible(context)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val target = component(context)
        val pm = context.packageManager
        if (pm.getComponentEnabledSetting(target) != state) {
            pm.setComponentEnabledSetting(target, state, PackageManager.DONT_KILL_APP)
        }
    }
}
