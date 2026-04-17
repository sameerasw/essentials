package com.sameerasw.essentials.services.receivers

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.MapsState
import com.sameerasw.essentials.utils.ShellUtils

class SecurityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settingsRepository = SettingsRepository(context)
        val isDisableQsEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_DISABLE_QS_WHEN_LOCKED, false)

        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Maps Power Saving logic (migrated)
                if (MapsState.isEnabled && MapsState.hasNavigationNotification) {
                    ShellUtils.runCommand(
                        context,
                        "am start -n com.google.android.apps.maps/com.google.android.apps.gmm.features.minmode.MinModeActivity"
                    )
                }

                // New Disable QS logic
                if (isDisableQsEnabled) {
                    ShellUtils.runCommand(context, "cmd statusbar send-disable-flag quick-settings")
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                // Restore QS access on unlock
                ShellUtils.runCommand(context, "cmd statusbar send-disable-flag none")
            }
        }
    }
}
