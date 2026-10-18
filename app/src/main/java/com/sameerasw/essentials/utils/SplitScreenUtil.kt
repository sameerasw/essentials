/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Windowing
 * File: SplitScreenUtil.kt
 * Description: Toggles split screen for the foreground app through the WMShell shell command.
 */

package com.sameerasw.essentials.utils

import android.content.ComponentName
import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.SplitScreenShortcutService

object SplitScreenUtil {
    private const val GESTURE_TARGETS = "accessibility_gesture_targets"
    private const val ENABLED_SERVICES = "enabled_accessibility_services"
    private const val WM_SHELL = "dumpsys activity service SystemUIService WMShell"

    // The accessibility split screen global action is rejected on current Android, so this drives WMShell directly.
    private const val TOGGLE_SCRIPT =
        "t=\$(dumpsys activity activities | grep -m1 topResumedActivity | sed -E 's/.* t([0-9]+)\\}.*/\\1/'); " +
            "[ -n \"\$t\" ] || exit 0; " +
            "if $WM_SHELL | grep -q 'isSplitVisible=true'; " +
            "then $WM_SHELL splitscreen exitSplitScreen \$t; " +
            "else $WM_SHELL splitscreen moveToSideStage \$t 0; fi >/dev/null 2>&1"

    fun toggle(context: Context) {
        HapticUtil.performStrongDoubleHaptic(context)
        ShellUtils.runCommand(context, TOGGLE_SCRIPT, context.getString(R.string.diy_action_split_screen))
    }

    fun isGestureEnabled(context: Context): Boolean? =
        readSecure(context, GESTURE_TARGETS)?.split(':')?.contains(gestureComponent(context))

    // The two-finger swipe is the system accessibility gesture, so binding our shortcut service to it is all that is needed.
    fun setGestureEnabled(
        context: Context,
        enabled: Boolean,
    ) {
        val component = gestureComponent(context)
        val services = readSecure(context, ENABLED_SERVICES).withComponent(component, enabled)
        val targets = readSecure(context, GESTURE_TARGETS).withComponent(component, enabled)
        val commands =
            buildList {
                add("settings put secure $ENABLED_SERVICES '$services'")
                add("settings put secure $GESTURE_TARGETS '$targets'")
                if (enabled) add("settings put secure accessibility_enabled 1")
            }
        ShellUtils.runCommand(context, commands.joinToString("; "), context.getString(R.string.feat_split_screen_gesture_title))
    }

    private fun gestureComponent(context: Context) =
        ComponentName(context, SplitScreenShortcutService::class.java).flattenToString()

    private fun readSecure(
        context: Context,
        key: String,
    ) = ShellUtils
        .runCommandWithOutput(context, "settings get secure $key", notifyOnError = false)
        ?.takeIf { it != "null" }

    private fun String?.withComponent(
        component: String,
        include: Boolean,
    ): String {
        val others = orEmpty().split(':').filter { it.isNotBlank() && it != component }
        return (if (include) others + component else others).joinToString(":")
    }
}
