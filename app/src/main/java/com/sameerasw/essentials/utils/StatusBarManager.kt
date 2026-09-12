/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: System UI & Status Bar Controls
 * File: StatusBarManager.kt
 * Description: Manages System UI status bar disable flags, quick settings panel expansion,
 * and lock screen security restrictions via system shell commands.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object StatusBarManager {
    // Disable request flags (Official flags from 'cmd statusbar' help)
    const val FLAG_NONE = "none"
    const val FLAG_SEARCH = "search"
    const val FLAG_HOME = "home"
    const val FLAG_RECENTS = "recents"
    const val FLAG_NOTIFICATION_PEEK = "notification-peek"
    const val FLAG_STATUSBAR_EXPANSION = "statusbar-expansion"
    const val FLAG_SYSTEM_ICONS = "system-icons"
    const val FLAG_CLOCK = "clock"
    const val FLAG_NOTIFICATION_ICONS = "notification-icons"
    const val FLAG_QUICK_SETTINGS = "quick-settings"

    private val disableRequests = mutableMapOf<String, Set<String>>()

    /**
     * Request disabling specific status bar features.
     * @param context [Context] Context to run shell commands
     * @param requesterId [String] Unique ID of the module (e.g., "ScreenLockedSecurity")
     * @param flags [Set<String>] Set of flags to disable
     */
    fun requestDisable(
        context: Context,
        requesterId: String,
        flags: Set<String>,
    ) {
        disableRequests[requesterId] = flags
        update(context)
    }

    /**
     * Restore status bar features for a specific module.
     * @param context [Context] Context to run shell commands
     * @param requesterId [String] Unique ID of the module
     */
    fun requestRestore(
        context: Context,
        requesterId: String,
    ) {
        if (disableRequests.containsKey(requesterId)) {
            disableRequests.remove(requesterId)
            update(context)
        }
    }

    /**
     * Aggregate all active disable requests along with persistent settings and apply the final status bar state.
     */
    fun update(context: Context) {
        val allFlags = disableRequests.values.flatten().toMutableSet()

        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val isHideSystemIcons = prefs.getBoolean(SettingsRepository.KEY_HIDE_SYSTEM_ICONS, false)
        val isHideSystemIconsLockedOnly = prefs.getBoolean(SettingsRepository.KEY_HIDE_SYSTEM_ICONS_LOCKED_ONLY, false)
        val isHideClock = prefs.getBoolean(SettingsRepository.KEY_HIDE_CLOCK, false)
        val isHideNotificationIcons = prefs.getBoolean(SettingsRepository.KEY_HIDE_NOTIFICATION_ICONS, false)

        if (isHideSystemIcons && !isHideSystemIconsLockedOnly) {
            allFlags.add(FLAG_SYSTEM_ICONS)
        }
        if (isHideClock) {
            allFlags.add(FLAG_CLOCK)
        }
        if (isHideNotificationIcons) {
            allFlags.add(FLAG_NOTIFICATION_ICONS)
        }

        val command =
            if (allFlags.isEmpty()) {
                "cmd statusbar send-disable-flag none"
            } else {
                "cmd statusbar send-disable-flag ${allFlags.joinToString(" ")}"
            }
        ShellUtils.runCommand(context, command)
    }

    /**
     * Re-assert status bar disable flags after keyguard unlock or transition.
     * Forces a state delta in system_server so the disable event is dispatched
     * to SystemUI to prevent system icons from unhiding.
     */
    fun reassertFlags(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
            val isHideSystemIcons = prefs.getBoolean(SettingsRepository.KEY_HIDE_SYSTEM_ICONS, false)
            val isHideSystemIconsLockedOnly = prefs.getBoolean(SettingsRepository.KEY_HIDE_SYSTEM_ICONS_LOCKED_ONLY, false)

            if (isHideSystemIcons && !isHideSystemIconsLockedOnly) {
                // Allow SystemUI keyguard dismissal animation to complete
                delay(250)

                val tempFlags = disableRequests.values.flatten().toMutableSet()
                if (prefs.getBoolean(SettingsRepository.KEY_HIDE_CLOCK, false)) tempFlags.add(FLAG_CLOCK)
                if (prefs.getBoolean(SettingsRepository.KEY_HIDE_NOTIFICATION_ICONS, false)) tempFlags.add(FLAG_NOTIFICATION_ICONS)

                val tempCmd =
                    if (tempFlags.isEmpty()) {
                        "cmd statusbar send-disable-flag none"
                    } else {
                        "cmd statusbar send-disable-flag ${tempFlags.joinToString(" ")}"
                    }

                ShellUtils.runCommand(context, tempCmd)
                delay(50)
            }
            update(context)
        }
    }

    // --- Action Commands ---

    /**
     * Open the notifications panel.
     */
    fun expandNotifications(context: Context) {
        ShellUtils.runCommand(context, "cmd statusbar expand-notifications")
    }

    /**
     * Open the notifications panel and expand quick settings if present.
     */
    fun expandSettings(context: Context) {
        ShellUtils.runCommand(context, "cmd statusbar expand-settings")
    }

    /**
     * Collapse the notifications and settings panel.
     */
    fun collapse(context: Context) {
        ShellUtils.runCommand(context, "cmd statusbar collapse")
    }
}
