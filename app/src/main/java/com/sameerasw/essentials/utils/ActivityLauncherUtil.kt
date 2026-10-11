/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: ActivityLauncherUtil.kt
 * Description: Lists an app's activities for the Open activity action and resolves their icons.
 */

package com.sameerasw.essentials.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.ActivityIconSource
import java.io.File
import java.io.FileOutputStream

object ActivityLauncherUtil {
    data class LaunchableActivity(
        val className: String,
        val label: String,
        val requiresRoot: Boolean,
    )

    // Non-exported or permission-guarded activities need root
    fun getActivities(
        context: Context,
        packageName: String,
    ): List<LaunchableActivity> {
        val pm = context.packageManager
        val info =
            try {
                pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            } catch (_: Exception) {
                return emptyList()
            }
        return info.activities
            .orEmpty()
            .map { activity ->
                val isGuarded =
                    activity.permission != null &&
                        context.checkSelfPermission(activity.permission) != PackageManager.PERMISSION_GRANTED
                LaunchableActivity(
                    className = activity.name,
                    label = activity.loadLabel(pm).toString().ifBlank { activity.name.substringAfterLast('.') },
                    requiresRoot = !activity.exported || isGuarded,
                )
            }.sortedWith(compareBy({ it.requiresRoot }, { it.label.lowercase() }, { it.className }))
    }

    fun shortClassName(
        packageName: String,
        className: String,
    ): String = if (className.startsWith("$packageName.")) className.removePrefix(packageName) else className

    fun loadIcon(
        context: Context,
        packageName: String,
        className: String,
        sizePx: Int,
    ): Bitmap? =
        try {
            context.packageManager
                .getActivityIcon(ComponentName(packageName, className))
                .toBitmap(sizePx, sizePx)
        } catch (_: Exception) {
            null
        }

    fun loadAppIcon(
        context: Context,
        packageName: String,
        sizePx: Int,
    ): Bitmap? =
        try {
            context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx)
        } catch (_: Exception) {
            null
        }

    // Gson sets an unknown icon source to null despite the non-null type
    @Suppress("USELESS_ELVIS")
    fun iconSourceOf(action: Action.OpenActivity): ActivityIconSource = action.iconSource ?: ActivityIconSource.ACTIVITY

    // Falls back to the activity's icon, then the app's, when the chosen one can't be loaded
    fun loadShortcutIcon(
        context: Context,
        action: Action.OpenActivity,
        sizePx: Int,
    ): Bitmap? {
        val chosen =
            when (iconSourceOf(action)) {
                ActivityIconSource.ACTIVITY -> null
                ActivityIconSource.APP -> loadAppIcon(context, action.packageName, sizePx)
                ActivityIconSource.CUSTOM -> BitmapFactory.decodeFile(action.customIconPath)?.scale(sizePx, sizePx)
            }
        return chosen
            ?: loadIcon(context, action.packageName, action.className, sizePx)
            ?: loadAppIcon(context, action.packageName, sizePx)
    }

    // Cropped to the centre square so launchers and the lock screen don't stretch it
    fun decodeSquareImage(
        context: Context,
        uri: Uri,
        sizePx: Int,
    ): Bitmap? =
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sampleSize = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= sizePx) sampleSize *= 2
            val bitmap =
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
                } ?: return null
            val side = minOf(bitmap.width, bitmap.height)
            Bitmap
                .createBitmap(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side)
                .scale(sizePx, sizePx)
        } catch (_: Exception) {
            null
        }

    fun saveCustomIcon(
        context: Context,
        bitmap: Bitmap,
        name: String,
    ): String? =
        try {
            val file = File(File(context.filesDir, CUSTOM_ICON_DIR).apply { mkdirs() }, "$name.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            file.absolutePath
        } catch (_: Exception) {
            null
        }

    fun deleteCustomIcon(path: String) {
        if (path.isNotBlank()) File(path).delete()
    }

    const val CUSTOM_ICON_SIZE_PX = 192
    private const val CUSTOM_ICON_DIR = "activity_icons"
}
