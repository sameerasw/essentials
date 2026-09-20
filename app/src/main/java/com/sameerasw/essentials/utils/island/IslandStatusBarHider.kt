/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Island
 * File: IslandStatusBarHider.kt
 * Description: Utility helper for IslandStatusBarHider.kt.
 */

package com.sameerasw.essentials.utils.island

import android.content.Context
import com.sameerasw.essentials.utils.isClockHiddenInSettings
import com.sameerasw.essentials.utils.sendStatusBarDisableFlags

object IslandStatusBarHider {
    private const val FLAG_SYSTEM_ICONS = "system-icons"
    private const val FLAG_NOTIFICATION_ICONS = "notification-icons"
    private const val FLAG_CLOCK = "clock"

    private var isHidden = false

    fun apply(
        context: Context,
        hide: Boolean,
    ) {
        if (hide == isHidden) return

        val flags =
            if (hide) {
                buildSet {
                    add(FLAG_SYSTEM_ICONS)
                    add(FLAG_NOTIFICATION_ICONS)
                    if (!isClockHiddenInSettings(context)) add(FLAG_CLOCK)
                }
            } else {
                emptySet()
            }

        if (sendStatusBarDisableFlags(context, flags)) {
            isHidden = hide
        }
    }

    fun restore(context: Context) {
        if (!isHidden) return
        if (sendStatusBarDisableFlags(context, emptySet())) {
            isHidden = false
        }
    }

    fun reset() {
        isHidden = false
    }
}
