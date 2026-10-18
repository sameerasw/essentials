/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: WearOS Companion
 * File: WatchEsimHandoff.kt
 * Description: Validates eSIM activation codes and hands them to the Pixel Watch app. Based on Watch eSIM Helper (Apache-2.0).
 */

package com.sameerasw.essentials.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.net.URI

data class EsimActivationCode(
    val payload: String,
    val server: String,
    val needsConfirmationCode: Boolean,
)

object WatchEsimHandoff {
    const val COMPANION_PACKAGE = "com.google.android.apps.wear.companion"
    private const val COMPANION_ACTIVITY = "$COMPANION_PACKAGE.core.application.RootActivity"
    private val FORMAT =
        Regex("^(?:LPA:)?(1\\$([0-9a-zA-Z%*+\\-./:]+)\\$([0-9A-Z\\-]*)(?:\\$([0-9.]*))?(?:\\$(1))?)$")

    fun parse(input: String?): EsimActivationCode? {
        val value = input?.trim().orEmpty()
        if (value.isEmpty() || value.length > 259) return null
        val match = FORMAT.matchEntire(value) ?: return null
        if (match.groupValues[1].length > 255) return null
        val server = match.groupValues[2]
        val uri = runCatching { URI("https://$server") }.getOrNull() ?: return null
        if (uri.host == null || uri.userInfo != null || !uri.rawPath.isNullOrEmpty() ||
            uri.rawQuery != null || uri.rawFragment != null || uri.port > 65535
        ) {
            return null
        }
        return EsimActivationCode(value, server, match.groupValues[5] == "1")
    }

    fun isCompanionAvailable(context: Context): Boolean =
        runCatching {
            val component = ComponentName(COMPANION_PACKAGE, COMPANION_ACTIVITY)
            val pm = context.packageManager
            val info = pm.getActivityInfo(component, 0)
            info.exported && info.enabled && info.applicationInfo?.enabled == true
        }.getOrDefault(false)

    fun createIntent(code: EsimActivationCode): Intent {
        val uri =
            Uri.Builder()
                .scheme("https")
                .authority("madeby.google.com")
                .path("/wear-companion/navigation")
                .appendQueryParameter("dest", "esim_download")
                .appendQueryParameter("activationCode", code.payload)
                .appendQueryParameter("carrier", "Watch plan")
                .build()
        return Intent(Intent.ACTION_VIEW, uri)
            .setComponent(ComponentName(COMPANION_PACKAGE, COMPANION_ACTIVITY))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
