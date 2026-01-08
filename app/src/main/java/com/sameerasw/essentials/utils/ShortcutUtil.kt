package com.sameerasw.essentials.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
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
                    .setIcon(Icon.createWithBitmap(app.icon.toBitmap()))
                    .setIntent(intent)
                    .build()

                shortcutManager.requestPinShortcut(shortcut, null)
            }
        }
    }
}
