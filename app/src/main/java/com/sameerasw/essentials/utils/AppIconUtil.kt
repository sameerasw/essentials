/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: AppIconUtil.kt
 * Description: Utility to safely switch app launcher icon aliases.
 */

package com.sameerasw.essentials.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.AppIcon

object AppIconUtil {
    fun mainIntent(context: Context): android.content.Intent =
        context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: android.content.Intent(context, com.sameerasw.essentials.MainActivity::class.java)

    fun setAppIcon(context: Context, targetIcon: AppIcon) {
        val packageManager = context.packageManager

        val targetComponent = ComponentName(context.packageName, targetIcon.aliasName)
        packageManager.setComponentEnabledSetting(
            targetComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

        for (icon in AppIcon.entries) {
            if (icon != targetIcon) {
                val component = ComponentName(context.packageName, icon.aliasName)
                packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }

    fun islandAppIcon(context: Context, packageName: String): Drawable {
        val pm = context.packageManager
        val icon = pm.getApplicationIcon(packageName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return icon
        if (!SettingsRepository(context).getBoolean(SettingsRepository.KEY_ISLAND_THEMED_APP_ICONS, false)) return icon
        val launcher = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage(packageName)
        val candidates = listOf(icon) + (
            try {
                pm.queryIntentActivities(launcher, 0).mapNotNull { runCatching { it.loadIcon(pm) }.getOrNull() }
            } catch (_: Exception) {
                emptyList()
            }
            )
        val withMonochrome = candidates.firstOrNull { (it as? AdaptiveIconDrawable)?.monochrome != null }
        return themed(context, withMonochrome ?: icon) ?: icon
    }

    enum class ThemedMode { MONO, DUO }

    private val themedModes = java.util.Collections.synchronizedMap(java.util.WeakHashMap<Bitmap, ThemedMode>())

    fun themedMode(bitmap: Bitmap): ThemedMode? = themedModes[bitmap]

    private fun themed(context: Context, icon: Drawable): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val adaptive = icon as? AdaptiveIconDrawable
        val monochrome = (adaptive?.monochrome ?: adaptive?.foreground)?.mutate() ?: return duotone(context, icon)
        monochrome.setTint(android.graphics.Color.WHITE)
        val canvasSize = 432
        val full = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
        monochrome.setBounds(0, 0, canvasSize, canvasSize)
        monochrome.draw(Canvas(full))
        val start = canvasSize * SAFE_ZONE_START / ADAPTIVE_SIZE
        val side = canvasSize * (SAFE_ZONE_END - SAFE_ZONE_START) / ADAPTIVE_SIZE
        val bitmap = Bitmap.createBitmap(full, start, start, side, side)
        themedModes[bitmap] = ThemedMode.MONO
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun duotone(context: Context, icon: Drawable): Drawable {
        val gray =
            ColorMatrix(
                floatArrayOf(
                    0.213f, 0.715f, 0.072f, 0f, 0f,
                    0.213f, 0.715f, 0.072f, 0f, 0f,
                    0.213f, 0.715f, 0.072f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        val size = 216
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val drawable = icon.constantState?.newDrawable()?.mutate() ?: icon.mutate()
        drawable.colorFilter = ColorMatrixColorFilter(gray)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        themedModes[bitmap] = ThemedMode.DUO
        return BitmapDrawable(context.resources, bitmap)
    }

    private const val ADAPTIVE_SIZE = 108
    private const val SAFE_ZONE_START = 21
    private const val SAFE_ZONE_END = 87
}
