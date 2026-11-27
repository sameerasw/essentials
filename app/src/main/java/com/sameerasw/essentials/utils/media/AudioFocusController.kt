/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Media Utilities
 * File: AudioFocusController.kt
 * Description: Toggles whether an app can take audio focus from other apps.
 */

package com.sameerasw.essentials.utils.media

import android.content.Context
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.ShellUtils

object AudioFocusController {
    private const val OP = "TAKE_AUDIO_FOCUS"

    fun setIgnored(
        context: Context,
        packageName: String,
        ignored: Boolean,
    ) {
        ShellUtils.runCommand(
            context,
            "appops set $packageName $OP ${if (ignored) "ignore" else "default"}",
            featureName = context.getString(R.string.feat_app_volume_title),
        )
    }
}
