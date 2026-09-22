/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Island
 * File: IslandStatusBarHider.kt
 * Description: Utility helper for IslandStatusBarHider.kt.
 */

package com.sameerasw.essentials.island.service

import android.content.Context
import com.sameerasw.essentials.utils.StatusBarManager

object IslandStatusBarHider {
    private const val REQUESTER_ID = "IslandDynamicHide"

    fun apply(
        context: Context,
        hide: Boolean,
    ) {
        if (hide) {
            StatusBarManager.requestDisable(
                context,
                REQUESTER_ID,
                setOf(
                    StatusBarManager.FLAG_SYSTEM_ICONS,
                    StatusBarManager.FLAG_NOTIFICATION_ICONS,
                    StatusBarManager.FLAG_CLOCK,
                ),
            )
        } else {
            StatusBarManager.requestRestore(context, REQUESTER_ID)
        }
    }

    fun restore(context: Context) {
        StatusBarManager.requestRestore(context, REQUESTER_ID)
    }
}
