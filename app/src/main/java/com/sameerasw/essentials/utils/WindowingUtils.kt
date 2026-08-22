/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Windowing & Multi-Window
 * File: WindowingUtils.kt
 * Description: Clean helper utility for launching standard and private floating web overlay windows.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.sameerasw.essentials.services.FloatingWebWindowService

object WindowingUtils {
    private const val TAG = "WindowingUtils"

    /**
     * Checks if floating overlay window mode is supported on this device.
     */
    fun isFloatingModeSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val pm = context.packageManager
        val isWatch = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WATCH)
        val isTv = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
        val isAutomotive = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_AUTOMOTIVE)
        if (isWatch || isTv || isAutomotive) return false

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        if (am?.isLowRamDevice == true) return false

        return true
    }

    /**
     * Launches the built-in Interactive Floating Web Window Overlay with optional Private Incognito mode.
     */
    fun launchOverlayWindow(context: Context, uri: Uri, isPrivate: Boolean = false): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                FloatingWebWindowService.start(context, uri.toString(), isPrivate)
                true
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch overlay window", e)
            false
        }
    }
}
