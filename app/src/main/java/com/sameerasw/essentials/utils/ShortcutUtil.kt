package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.sameerasw.essentials.ShortcutHandlerActivity
import com.sameerasw.essentials.domain.model.NotificationApp

object ShortcutUtil {
    fun pinAppShortcut(context: Context, app: NotificationApp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                val intent = Intent(context, ShortcutHandlerActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("package_name", app.packageName)
                    // Ensure each shortcut has a unique ID/intent filter if needed, 
                    // though ShortcutInfo ID handles uniqueness.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }

                val shortcut = ShortcutInfo.Builder(context, app.packageName)
                    .setShortLabel(app.appName)
                    .setLongLabel(app.appName)
                    .setIcon(
                        Icon.createWithBitmap(
                            AppUtil.getShortcutIcon(
                                context,
                                app.packageName
                            )
                        )
                    )
                    .setIntent(intent)
                    .build()

                shortcutManager.requestPinShortcut(shortcut, null)
            }
        }
    }

    fun updateLauncherDynamicShortcuts(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val repository = com.sameerasw.essentials.data.repository.SettingsRepository(context)
            val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

            val shortcuts = mutableListOf<ShortcutInfo>()

            // Wallpaper
            val wallpaperIntent = Intent(context, com.sameerasw.essentials.ui.activities.WallpaperActivity::class.java).apply {
                action = Intent.ACTION_VIEW
            }
            val wallpaperShortcut = ShortcutInfo.Builder(context, "shortcut_wallpaper")
                .setShortLabel(context.getString(com.sameerasw.essentials.R.string.feat_daily_wallpaper_title))
                .setLongLabel(context.getString(com.sameerasw.essentials.R.string.feat_daily_wallpaper_title))
                .setIcon(Icon.createWithResource(context, com.sameerasw.essentials.R.drawable.rounded_wallpaper_24))
                .setIntent(wallpaperIntent)
                .build()
            shortcuts.add(wallpaperShortcut)

            // Dynamic shortcuts
            val pinnedKeys = repository.getPinnedFeatures()
            val featuresMap = com.sameerasw.essentials.domain.registry.FeatureRegistry.ALL_FEATURES.associateBy { it.id }

            var count = 0
            for (key in pinnedKeys) {
                if (count >= 2) break 
                val feature = featuresMap[key] ?: continue
                if (feature.id == "DailyWallpaper" || feature.id == "LiveWallpaper") continue

                val intent = Intent(context, com.sameerasw.essentials.FeatureSettingsActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("feature", feature.id)
                }
                val shortcut = ShortcutInfo.Builder(context, "shortcut_feat_${feature.id}")
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
