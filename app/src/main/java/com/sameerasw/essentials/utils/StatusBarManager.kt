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
import com.sameerasw.essentials.R
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

    private var lastAppliedCommand: String? = null
    private val lock = Any()

    private fun persistentFlags(context: Context, includeSystemIcons: Boolean): MutableSet<String> {
        val flags = mutableSetOf<String>()
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val isHideSystemIcons = prefs.getBoolean(SettingsRepository.KEY_HIDE_SYSTEM_ICONS, false)
        val isHideSystemIconsLockedOnly = prefs.getBoolean(SettingsRepository.KEY_HIDE_SYSTEM_ICONS_LOCKED_ONLY, false)
        if (includeSystemIcons && isHideSystemIcons && !isHideSystemIconsLockedOnly) flags.add(FLAG_SYSTEM_ICONS)
        if (prefs.getBoolean(SettingsRepository.KEY_HIDE_CLOCK, false)) flags.add(FLAG_CLOCK)
        if (prefs.getBoolean(SettingsRepository.KEY_HIDE_NOTIFICATION_ICONS, false)) flags.add(FLAG_NOTIFICATION_ICONS)
        if (prefs.getBoolean(SettingsRepository.KEY_HIDE_GESTURE_BAR_ENABLED, false)) flags.add(FLAG_HOME)
        return flags
    }

    private fun commandFor(flags: Set<String>) =
        if (flags.isEmpty()) {
            "cmd statusbar send-disable-flag none"
        } else {
            "cmd statusbar send-disable-flag ${flags.joinToString(" ")}"
        }

    /**
     * Aggregate all active disable requests along with persistent settings and apply the final status bar state.
     */
    fun update(context: Context) {
        synchronized(lock) {
            applyLocked(context, force = false)
        }
    }

    private fun applyLocked(context: Context, force: Boolean) {
        val allFlags = disableRequests.values.flatten().toMutableSet()
        allFlags.addAll(persistentFlags(context, includeSystemIcons = true))

        val command = commandFor(allFlags)

        if (!force && allFlags.isEmpty() && (lastAppliedCommand == null || lastAppliedCommand == commandFor(emptySet()))) {
            return
        }

        if (!ShellUtils.hasPermission(context)) {
            return
        }

        if (!force && command == lastAppliedCommand) return
        lastAppliedCommand = command

        ShellUtils.runCommand(
            context,
            command,
            featureName = context.getString(R.string.feat_statusbar_icons_title),
            notifyOnError = false,
        )
    }

    /**
     * Re-assert status bar disable flags after keyguard unlock or transition.
     * System UI can drop disable flags across keyguard transitions, so the last command is re-sent even when unchanged.
     * When system icons are hidden, a delta without them is sent first so the disable event is dispatched to SystemUI.
     */
    fun reassertFlags(context: Context) {
        if (!ShellUtils.hasPermission(context)) return

        CoroutineScope(Dispatchers.IO).launch {
            val hidesSystemIcons = synchronized(lock) {
                FLAG_SYSTEM_ICONS in persistentFlags(context, includeSystemIcons = true)
            }

            if (hidesSystemIcons) {
                delay(250)
                synchronized(lock) {
                    val tempFlags = disableRequests.values.flatten().toMutableSet()
                    tempFlags.addAll(persistentFlags(context, includeSystemIcons = false))
                    ShellUtils.runCommand(
                        context,
                        commandFor(tempFlags),
                        featureName = context.getString(R.string.feat_statusbar_icons_title),
                        notifyOnError = false,
                    )
                }
                delay(50)
            }
            synchronized(lock) { applyLocked(context, force = true) }
        }
    }

    // --- Action Commands ---

    /**
     * Open the notifications panel.
     */
    fun expandNotifications(context: Context) {
        ShellUtils.runCommand(
            context,
            "cmd statusbar expand-notifications",
            featureName = context.getString(R.string.action_expand_notifications),
        )
    }

    /**
     * Open the notifications panel and expand quick settings if present.
     */
    fun expandSettings(context: Context) {
        ShellUtils.runCommand(
            context,
            "cmd statusbar expand-settings",
            featureName = context.getString(R.string.action_expand_settings),
        )
    }

    /**
     * Collapse the notifications and settings panel.
     */
    fun collapse(context: Context) {
        ShellUtils.runCommand(
            context,
            "cmd statusbar collapse",
            featureName = context.getString(R.string.action_collapse_status_bar),
        )
    }
}
