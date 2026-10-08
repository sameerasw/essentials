/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: ShortcutUtil.kt
 * Description: Utility helper for ShortcutUtil.kt.
 */

package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import com.sameerasw.essentials.ShortcutHandlerActivity
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.model.NotificationApp
import com.sameerasw.essentials.ui.activities.PinnedActionActivity
import java.util.UUID

object ShortcutUtil {
    /**
     * Executes the pin app shortcut operation.
     *
     * @param context [Context] Target context.
     * @param app [NotificationApp] Target app.
     */
    fun pinAppShortcut(
        context: Context,
        app: NotificationApp,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val intent =
                    Intent(context, ShortcutHandlerActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("package_name", app.packageName)
                        // Ensure each shortcut has a unique ID/intent filter if needed,
                        // though ShortcutInfo ID handles uniqueness.
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }

                val shortcut =
                    ShortcutInfo
                        .Builder(context, app.packageName)
                        .setShortLabel(app.appName)
                        .setLongLabel(app.appName)
                        .setIcon(
                            Icon.createWithBitmap(
                                AppUtil.getShortcutIcon(
                                    context,
                                    app.packageName,
                                ),
                            ),
                        ).setIntent(intent)
                        .build()

                shortcutManager.requestPinShortcut(shortcut, null)
            }
        }
    }

    // Shortcuts carry only an id so other apps cannot use the exported activity to run arbitrary actions
    fun pinActionShortcut(
        context: Context,
        action: Action,
        label: String,
        icon: Bitmap,
        adaptive: Boolean = false,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) return false

        val id = UUID.randomUUID().toString()
        SettingsRepository(context).savePinnedAction(id, action)
        val intent =
            Intent(context, PinnedActionActivity::class.java).apply {
                this.action = Intent.ACTION_VIEW
                putExtra(PinnedActionActivity.EXTRA_SHORTCUT_ID, id)
            }
        val shortcut =
            ShortcutInfo
                .Builder(context, id)
                .setShortLabel(label)
                .setLongLabel(label)
                // Adaptive fills the launcher's icon shape instead of sitting small inside it
                .setIcon(if (adaptive) Icon.createWithAdaptiveBitmap(padToAdaptive(icon)) else Icon.createWithBitmap(icon))
                .setIntent(intent)
                .build()
        return shortcutManager.requestPinShortcut(shortcut, null)
    }

    // Launchers show only the middle 72 of an adaptive icon's 108 units, so the image goes there
    private fun padToAdaptive(icon: Bitmap): Bitmap {
        val size = icon.width * ADAPTIVE_FULL_SIZE / ADAPTIVE_VISIBLE_SIZE
        val inset = (size - icon.width) / 2f
        return createBitmap(size, size).applyCanvas { drawBitmap(icon, inset, inset, null) }
    }

    private const val ADAPTIVE_FULL_SIZE = 108
    private const val ADAPTIVE_VISIBLE_SIZE = 72

    /**
     * Executes the update launcher dynamic shortcuts operation.
     *
     * @param context [Context] Target context.
     */
    fun updateLauncherDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val repository =
                com.sameerasw.essentials.data.repository
                    .SettingsRepository(context)
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

            val shortcuts = mutableListOf<ShortcutInfo>()

            // Wallpaper
            val wallpaperIntent =
                Intent(
                    context,
                    com.sameerasw.essentials.ui.activities.WallpaperActivity::class.java,
                ).apply {
                    action = Intent.ACTION_VIEW
                }
            val wallpaperShortcut =
                ShortcutInfo
                    .Builder(context, "shortcut_wallpaper")
                    .setShortLabel(context.getString(com.sameerasw.essentials.R.string.feat_daily_wallpaper_title))
                    .setLongLabel(context.getString(com.sameerasw.essentials.R.string.feat_daily_wallpaper_title))
                    .setIcon(
                        Icon.createWithResource(
                            context,
                            com.sameerasw.essentials.R.drawable.rounded_wallpaper_24,
                        ),
                    ).setIntent(wallpaperIntent)
                    .build()
            shortcuts.add(wallpaperShortcut)

            // Dynamic shortcuts
            val pinnedKeys = repository.getPinnedFeatures()
            val featuresMap =
                com.sameerasw.essentials.domain.registry.FeatureRegistry.ALL_FEATURES
                    .associateBy { it.id }

            var count = 0
            for (key in pinnedKeys) {
                if (count >= 2) break
                val feature = featuresMap[key] ?: continue
                if (feature.id == "DailyWallpaper" || feature.id == "LiveWallpaper") continue

                val intent =
                    Intent(
                        context,
                        com.sameerasw.essentials.FeatureSettingsActivity::class.java,
                    ).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("feature", feature.id)
                    }
                val shortcut =
                    ShortcutInfo
                        .Builder(context, "shortcut_feat_${feature.id}")
                        .setShortLabel(context.getString(feature.title))
                        .setLongLabel(context.getString(feature.title))
                        .setIcon(Icon.createWithResource(context, feature.iconRes))
                        .setIntent(intent)
                        .build()
                shortcuts.add(shortcut)
                count++
            }

            shortcutManager.dynamicShortcuts = shortcuts
        }
    }
}
