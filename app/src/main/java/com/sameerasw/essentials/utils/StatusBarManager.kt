package com.sameerasw.essentials.utils

import android.content.Context

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
     * @param context Context to run shell commands
     * @param requesterId Unique ID of the module (e.g., "ScreenLockedSecurity")
     * @param flags Set of flags to disable
     */
    fun requestDisable(context: Context, requesterId: String, flags: Set<String>) {
        disableRequests[requesterId] = flags
        update(context)
    }

    /**
     * Restore status bar features for a specific module.
     * @param context Context to run shell commands
     * @param requesterId Unique ID of the module
     */
    fun requestRestore(context: Context, requesterId: String) {
        if (disableRequests.containsKey(requesterId)) {
            disableRequests.remove(requesterId)
            update(context)
        }
    }

    /**
     * Aggregate all active disable requests and apply the final status bar state.
     */
    private fun update(context: Context) {
        val allFlags = disableRequests.values.flatten().toSet()
        val command = if (allFlags.isEmpty()) {
            "cmd statusbar send-disable-flag none"
        } else {
            "cmd statusbar send-disable-flag ${allFlags.joinToString(" ")}"
        }
        ShellUtils.runCommand(context, command)
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
