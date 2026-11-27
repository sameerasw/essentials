/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Media Utilities
 * File: AudioFocusController.kt
 * Description: Reads and toggles whether apps can take audio focus from other apps.
 */

package com.sameerasw.essentials.utils.media

import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.appops.AppOpsParser
import com.sameerasw.essentials.utils.ShellUtils

object AudioFocusController {
    private const val OP = "TAKE_AUDIO_FOCUS"

    fun queryIgnored(context: Context): Set<String>? {
        val output =
            ShellUtils.runCommandWithOutput(
                context,
                "appops query-op $OP ignore; appops query-op $OP deny",
                notifyOnError = false,
            ) ?: return null
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && AppOpsParser.isValidPackage(it) && '.' in it }
            .toSet()
    }

    fun setIgnored(
        context: Context,
        packageName: String,
        ignored: Boolean,
    ): Boolean {
        if (!AppOpsParser.isValidPackage(packageName)) return false
        return ShellUtils.runCommandWithOutput(
            context,
            "appops set $packageName $OP ${if (ignored) "ignore" else "default"} 2>&1",
            featureName = context.getString(R.string.feat_app_volume_title),
        ) != null
    }
}
