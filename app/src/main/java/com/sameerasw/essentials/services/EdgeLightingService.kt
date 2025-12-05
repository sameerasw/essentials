package com.sameerasw.essentials.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.utils.OverlayHelper
import kotlin.or
import kotlin.text.compareTo
import kotlin.text.toInt
import kotlin.times

/**
 * Overlay service that shows a thin inward stroke on each edge of the screen
 * for a short duration then removes it. Uses draw-over-other-apps permission.
 *
 * Notes: drawing on AOD (always-on display) is not guaranteed by Android for normal apps.
 * We implement best-effort behavior: separate edge windows with flags that allow display
 * into system bar areas and attempt to re-add the overlay on screen-off events.
 * Some OEMs/Android versions will still prevent overlays on AOD or over the nav bar.
 */
class EdgeLightingService : Service() {

    private var windowManager: WindowManager? = null
    private val overlayViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())

    private var screenReceiver: BroadcastReceiver? = null

    companion object {
        private const val CHANNEL_ID = "edge_lighting_channel"
        private const val NOTIF_ID = 24321
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Register screen on/off receiver to attempt to re-show overlay when screen state changes
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                try {
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_OFF -> {
                            // best-effort: try to show overlay when screen goes off
                            if (canDrawOverlays()) showOverlay()
                        }
                        Intent.ACTION_SCREEN_ON -> {
                            // refresh overlay if needed
                            if (canDrawOverlays()) showOverlay()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, f)
    }

    override fun onDestroy() {
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!canDrawOverlays()) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Ensure the process calls startForeground quickly when started via startForegroundService
        // to avoid RemoteServiceException (ForegroundServiceDidNotStartInTimeException).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                startForeground(NOTIF_ID, buildNotification())
            } catch (_: Exception) {
                // ignore foreground start failures; we'll continue
            }
        }

        // If accessibility service is enabled, delegate showing to it for higher elevation
        if (isAccessibilityServiceEnabled()) {
            try {
                val ai = Intent(applicationContext, ScreenOffAccessibilityService::class.java).apply { action = "SHOW_EDGE_LIGHTING" }
                // Use startService to request the accessibility service perform the elevated overlay.
                // Starting an accessibility service via startForegroundService can cause MissingForegroundServiceType
                // exceptions because the accessibility service may not declare a foregroundServiceType. startService is
                // sufficient to deliver the intent to the AccessibilityService.
                applicationContext.startService(ai)
            } catch (e: Exception) {
                // fallback to normal overlay
                showOverlay()
            }

            // We delegated to the accessibility service; stop foreground and finish quickly.
            try { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) stopForeground(true) } catch (_: Exception) {}

            // stop this service; accessibility service will show overlay
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay()

        // Remove after 5 seconds
        handler.postDelayed({ removeOverlay(); stopSelf() }, 5000)

        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.sameerasw.essentials.R.drawable.rounded_magnify_fullscreen_24)
            .setContentTitle("")
            .setContentText("")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm != null) {
                val existing = nm.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val chan = NotificationChannel(CHANNEL_ID, "Edge Lighting", NotificationManager.IMPORTANCE_LOW)
                    chan.setSound(null, null)
                    nm.createNotificationChannel(chan)
                }
            }
        }
    }

    private fun showOverlay() {
        if (overlayViews.isNotEmpty()) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        try {
            val overlay = OverlayHelper.createOverlayView(this, com.sameerasw.essentials.R.color.material_color_primary_expressive)
            val params = OverlayHelper.createOverlayLayoutParams(getOverlayType())

            if (OverlayHelper.addOverlayView(windowManager, overlay, params)) {
                overlayViews.add(overlay)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }


    private fun getOverlayType(): Int {
        // If the accessibility service is enabled, prefer the accessibility overlay type which
        // can appear above more system surfaces on some devices (Tasker-style elevation).
        return when {
            isAccessibilityServiceEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                // TYPE_ACCESSIBILITY_OVERLAY exists on recent APIs and gives AccessibilityServices
                // more privilege to display above other UI in some cases.
                try {
                    WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY").getInt(null)
                } catch (e: Exception) {
                    // Fallback if reflection fails
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else -> WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val serviceName = "${packageName}/${ScreenOffAccessibilityService::class.java.name}"
            enabledServices?.contains(serviceName) == true
        } catch (e: Exception) {
            false
        }
    }

    private fun removeOverlay() {
        OverlayHelper.removeAllOverlays(windowManager, overlayViews)

        // stop foreground if we had started one
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true)
            }
        } catch (_: Exception) { }
    }

    private fun getSystemBarHeight(name: String): Int {
        val resId = resources.getIdentifier(name, "dimen", "android")
        return if (resId > 0) resources.getDimensionPixelSize(resId) else 0
    }

    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

}