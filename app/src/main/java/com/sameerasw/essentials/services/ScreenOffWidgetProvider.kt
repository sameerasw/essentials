package com.sameerasw.essentials.services

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.KeyEvent
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.content.getSystemService
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.HapticFeedbackType
import com.sameerasw.essentials.domain.ScreenOffMethod
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.performHapticFeedback

class ScreenOffWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val DOUBLE_TAP_TIMEOUT = 500L // 500ms

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.screen_off_widget)

            val intent = Intent(context, ScreenOffWidgetProvider::class.java).apply {
                action = "WIDGET_CLICK"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = android.net.Uri.parse("custom://widget/$appWidgetId")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            for (appWidgetId in appWidgetIds) {
                remove("screen_off_double_tap_$appWidgetId")
                remove("screen_off_last_tap_time_$appWidgetId")
            }
        }.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "WIDGET_CLICK") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            
            val isDoubleTapRequired = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                prefs.getBoolean("screen_off_double_tap_$appWidgetId", prefs.getBoolean("screen_off_double_tap", false))
            } else {
                prefs.getBoolean("screen_off_double_tap", false)
            }

            if (isDoubleTapRequired) {
                val lastTapTimeKey = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    "screen_off_last_tap_time_$appWidgetId"
                } else {
                    "screen_off_last_tap_time"
                }
                val lastTapTime = prefs.getLong(lastTapTimeKey, 0)
                val currentTime = SystemClock.elapsedRealtime()

                if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                    // Double tap detected
                    prefs.edit().putLong(lastTapTimeKey, 0).apply()
                    triggerScreenOff(context)
                } else {
                    // First tap
                    prefs.edit().putLong(lastTapTimeKey, currentTime).apply()
                    
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    if (vibrator != null) {
                        performHapticFeedback(vibrator, HapticFeedbackType.TICK)
                    }
                }
            } else {
                // Double tap not required, trigger immediately
                triggerScreenOff(context)
            }
        }
    }

    private fun triggerScreenOff(context: Context) {
        val prefs = context.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
        val selectedScreenOffMethod = try {
            ScreenOffMethod.valueOf(prefs.getString("screen_off_method", ScreenOffMethod.ACCESSIBILITY.name) ?: ScreenOffMethod.ACCESSIBILITY.name)
        } catch (e: IllegalArgumentException) {
            ScreenOffMethod.ACCESSIBILITY
        }

        val hapticFeedbackType = try {
            HapticFeedbackType.valueOf(prefs.getString("haptic_feedback_type", HapticFeedbackType.NONE.name) ?: HapticFeedbackType.NONE.name)
        } catch (e: IllegalArgumentException) {
            HapticFeedbackType.NONE
        }

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator != null) {
            performHapticFeedback(vibrator, hapticFeedbackType)
        }

        when (selectedScreenOffMethod) {
            ScreenOffMethod.ACCESSIBILITY -> {
                if (isAccessibilityEnabled(context)) {
                    val serviceIntent =
                        Intent(context, ScreenOffAccessibilityService::class.java).apply {
                            action = "LOCK_SCREEN"
                        }
                    context.startService(serviceIntent)
                } else {
                    Toast.makeText(context, "Missing Accessibility permission, Check the app", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            ScreenOffMethod.INPUT -> {
                if (ShellUtils.hasPermission(context)) {
                    // Simulate power button press using input keyevent
                    // Requires root or Shizuku, which ShellUtils handles
                    ShellUtils.runCommand(context, "input keyevent ${KeyEvent.KEYCODE_POWER}")
                } else {
                    Toast.makeText(context, "Missing Shizuku/Root permission for Input method, Check the app", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun isAccessibilityEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains("com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService") == true
    }
}