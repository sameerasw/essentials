/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: SecurityReceiver.kt
 * Description: Background service component for SecurityReceiver.kt.
 */

package com.sameerasw.essentials.services.receivers

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.MapsState
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.StatusBarManager

class SecurityReceiver : BroadcastReceiver() {
    companion object {
        const val REQUESTER_NOTIFICATION_INTERACTIONS = "DisableNotificationInteractions"
        private const val REQUESTER_QS = "DisableQsWhenLocked"
        private val TRUST_REFRESH_DELAYS_MS = listOf(500L, 1500L, 3000L)
        private val handler = Handler(Looper.getMainLooper())

        // Trust agents such as a watch unlock the device behind the lock screen, so the keyguard shows but the device isn't locked
        private fun isExtendedUnlock(context: Context): Boolean {
            val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return false
            return !keyguard.isDeviceLocked
        }

        private fun disableOnExtendedUnlock(settings: SettingsRepository) =
            settings.getBoolean(SettingsRepository.KEY_SCREEN_LOCKED_DISABLE_ON_EXTENDED_UNLOCK, false)

        private fun applyRestrictions(
            context: Context,
            settings: SettingsRepository,
        ) {
            if (settings.getBoolean(SettingsRepository.KEY_SCREEN_LOCKED_SECURITY_ENABLED, false)) {
                StatusBarManager.requestDisable(context, REQUESTER_QS, setOf(StatusBarManager.FLAG_QUICK_SETTINGS))
            }
            if (settings.getBoolean(SettingsRepository.KEY_SCREEN_LOCKED_DISABLE_NOTIFICATION_INTERACTIONS, false)) {
                StatusBarManager.requestDisable(
                    context,
                    REQUESTER_NOTIFICATION_INTERACTIONS,
                    setOf(StatusBarManager.FLAG_STATUSBAR_EXPANSION, StatusBarManager.FLAG_QUICK_SETTINGS),
                )
            }
        }

        private fun restoreRestrictions(context: Context) {
            StatusBarManager.requestRestore(context, REQUESTER_QS)
            StatusBarManager.requestRestore(context, REQUESTER_NOTIFICATION_INTERACTIONS)
        }

        // Follows trust changes while the lock screen is showing
        fun refreshForTrust(context: Context) {
            val settings = SettingsRepository(context)
            if (!disableOnExtendedUnlock(settings)) return
            val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return
            if (!keyguard.isKeyguardLocked) return
            if (isExtendedUnlock(context)) restoreRestrictions(context) else applyRestrictions(context, settings)
            StatusBarManager.reassertFlags(context)
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val settingsRepository = SettingsRepository(context)
        val isHideSystemIconsEnabled =
            settingsRepository.getBoolean(SettingsRepository.KEY_HIDE_SYSTEM_ICONS, false)
        val isHideSystemIconsLockedOnlyEnabled =
            settingsRepository.getBoolean(
                SettingsRepository.KEY_HIDE_SYSTEM_ICONS_LOCKED_ONLY,
                false,
            )

        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Maps Power Saving logic (migrated)
                if (MapsState.isEnabled && MapsState.hasNavigationNotification) {
                    ShellUtils.runCommand(
                        context,
                        "am start -n com.google.android.apps.maps/com.google.android.apps.gmm.features.minmode.MinModeActivity",
                    )
                }

                if (!(disableOnExtendedUnlock(settingsRepository) && isExtendedUnlock(context))) {
                    applyRestrictions(context, settingsRepository)
                }

                // Dynamic Hide System Icons logic
                if (isHideSystemIconsEnabled && isHideSystemIconsLockedOnlyEnabled) {
                    StatusBarManager.requestDisable(
                        context,
                        "StatusBarIconAdvancedLocked",
                        setOf(StatusBarManager.FLAG_SYSTEM_ICONS),
                    )
                }
            }

            Intent.ACTION_SCREEN_ON -> {
                if (disableOnExtendedUnlock(settingsRepository)) {
                    TRUST_REFRESH_DELAYS_MS.forEach { handler.postDelayed({ refreshForTrust(context) }, it) }
                }
            }

            Intent.ACTION_USER_PRESENT -> {
                // Restore QS and System Icons on unlock
                StatusBarManager.requestRestore(
                    context,
                    "DisableQsWhenLocked",
                )
                StatusBarManager.requestRestore(
                    context,
                    "StatusBarIconAdvancedLocked",
                )
                StatusBarManager.requestRestore(
                    context,
                    REQUESTER_NOTIFICATION_INTERACTIONS,
                )
                StatusBarManager.reassertFlags(context)
            }
        }
    }
}
