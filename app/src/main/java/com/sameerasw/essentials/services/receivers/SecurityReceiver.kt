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
        val isDisableQsEnabled = settingsRepository.getBoolean(SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED, false)

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
                    com.sameerasw.essentials.utils.StatusBarManager.requestDisable(
                        context,
                        "DisableQsWhenLocked",
                        setOf(com.sameerasw.essentials.utils.StatusBarManager.FLAG_QUICK_SETTINGS)
                    )
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                // Restore QS access on unlock
                com.sameerasw.essentials.utils.StatusBarManager.requestRestore(context, "DisableQsWhenLocked")
            }
        }
    }
}
