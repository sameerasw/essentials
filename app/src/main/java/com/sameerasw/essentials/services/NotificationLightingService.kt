package com.sameerasw.essentials.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import com.sameerasw.essentials.utils.OverlayHelper

/**
 * Overlay service that shows a light pulse for notifications.
 * Uses draw-over-other-apps permission.
 */
class NotificationLightingService : Service() {

    private var windowManager: WindowManager? = null
    private val overlayViews = mutableListOf<View>()
    private var cornerRadiusDp: Float = OverlayHelper.CORNER_RADIUS_DP.toFloat()
    private var strokeThicknessDp: Float = OverlayHelper.STROKE_DP.toFloat()
    private var isPreview: Boolean = false
    private var colorMode: NotificationLightingColorMode = NotificationLightingColorMode.SYSTEM
    private var customColor: Int = 0
    private var resolvedColor: Int? = null
    private var pulseCount: Int = 1
    private var pulseDuration: Long = 3000
    private var edgeLightingStyle: NotificationLightingStyle = NotificationLightingStyle.STROKE
    private var glowSides: Set<NotificationLightingSide> =
        setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
    private var indicatorX: Float = 50f
    private var indicatorY: Float = 2f
    private var indicatorScale: Float = 1.0f
    private var isAmbientDisplay: Boolean = false

    private var screenReceiver: BroadcastReceiver? = null

    companion object {
        private const val CHANNEL_ID = "edge_lighting_channel"
        private const val NOTIF_ID = 24321
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                startForeground(NOTIF_ID, buildNotification())
            } catch (_: Exception) {
                // ignore foreground start failures on certain OEMs
            }
        }

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
                } catch (_: Exception) {
                }
            }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, f)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NotificationLightingSvc", "onStartCommand: action=${intent?.action}")
        // Accessibility service Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!canDrawOverlays() || !isAccessibilityServiceEnabled()) {
                stopSelf()
                return START_NOT_STICKY
            }
        } else {
            if (!canDrawOverlays()) {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Get corner radius from intent, default to OverlayHelper.CORNER_RADIUS_DP
        cornerRadiusDp =
            intent?.getFloatExtra("corner_radius_dp", OverlayHelper.CORNER_RADIUS_DP.toFloat())
                ?: OverlayHelper.CORNER_RADIUS_DP.toFloat()
        strokeThicknessDp =
            intent?.getFloatExtra("stroke_thickness_dp", OverlayHelper.STROKE_DP.toFloat())
                ?: OverlayHelper.STROKE_DP.toFloat()
        isPreview = intent?.getBooleanExtra("is_preview", false) ?: false
        val colorModeName = intent?.getStringExtra("color_mode")
        colorMode = NotificationLightingColorMode.valueOf(
            colorModeName ?: NotificationLightingColorMode.SYSTEM.name
        )
        customColor = intent?.getIntExtra("custom_color", 0) ?: 0
        resolvedColor = if (intent?.hasExtra("resolved_color") == true) intent.getIntExtra(
            "resolved_color",
            0
        ) else null
        pulseCount = intent?.getIntExtra("pulse_count", 1) ?: 1
        pulseDuration = intent?.getLongExtra("pulse_duration", 3000L) ?: 3000L
        val styleName = intent?.getStringExtra("style")
        edgeLightingStyle =
            if (styleName != null) NotificationLightingStyle.valueOf(styleName) else NotificationLightingStyle.STROKE
        val glowSidesArray = intent?.getStringArrayExtra("glow_sides")
        glowSides = glowSidesArray?.mapNotNull {
            try {
                NotificationLightingSide.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }?.toSet()
            ?: setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
        indicatorX = intent?.getFloatExtra("indicator_x", 50f) ?: 50f
        indicatorY = intent?.getFloatExtra("indicator_y", 2f) ?: 2f
        indicatorScale = intent?.getFloatExtra("indicator_scale", 1.0f) ?: 1.0f
        isAmbientDisplay = intent?.getBooleanExtra("is_ambient_display", false) ?: false
        val ignoreScreenState = intent?.getBooleanExtra("ignore_screen_state", false) ?: false
        val removePreview = intent?.getBooleanExtra("remove_preview", false) ?: false

        if (removePreview) {
            // If accessibility service is enabled, delegate to it
            if (isAccessibilityServiceEnabled()) {
                try {
                    val ai = Intent(
                        applicationContext,
                        ScreenOffAccessibilityService::class.java
                    ).apply {
                        action = "SHOW_NOTIFICATION_LIGHTING"
                        putExtra("remove_preview", true)
                    }
                    applicationContext.startService(ai)
                } catch (_: Exception) {
                }
            }

            // Remove local preview as well
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }


        // If accessibility service is enabled, delegate showing to it for higher elevation
        if (isAccessibilityServiceEnabled()) {
            try {
                val ai =
                    Intent(applicationContext, ScreenOffAccessibilityService::class.java).apply {
                        action = "SHOW_NOTIFICATION_LIGHTING"
                        putExtra("corner_radius_dp", cornerRadiusDp)
                        putExtra("stroke_thickness_dp", strokeThicknessDp)
                        putExtra("is_preview", isPreview)
                        putExtra("ignore_screen_state", ignoreScreenState)
                        putExtra("color_mode", intent?.getStringExtra("color_mode"))
                        putExtra("custom_color", intent?.getIntExtra("custom_color", 0) ?: 0)
                        putExtra("pulse_count", pulseCount)
                        putExtra("pulse_duration", pulseDuration)
                        putExtra("style", edgeLightingStyle.name)
                        putExtra("glow_sides", glowSides.map { it.name }.toTypedArray())
                        putExtra("indicator_x", indicatorX)
                        putExtra("indicator_y", indicatorY)
                        putExtra("indicator_scale", indicatorScale)
                        if (intent?.hasExtra("resolved_color") == true) {
                            putExtra("resolved_color", intent.getIntExtra("resolved_color", 0))
                        }
                        putExtra(
                            "is_ambient_display",
                            intent?.getBooleanExtra("is_ambient_display", false) ?: false
                        )
                        putExtra(
                            "is_ambient_show_lock_screen",
                            intent?.getBooleanExtra("is_ambient_show_lock_screen", false) ?: false
                        )
                    }
                // Use startService to request the accessibility service perform the elevated overlay.
                // Starting an accessibility service via startForegroundService can cause MissingForegroundServiceType
                // exceptions because the accessibility service may not declare a foregroundServiceType. startService is
                // sufficient to deliver the intent to the AccessibilityService.
                applicationContext.startService(ai)
            } catch (_: Exception) {
                // If delegation fails, stop - don't fall back
                stopSelf()
                return START_NOT_STICKY
            }

            // We delegated to the accessibility service; stop foreground and finish quickly.
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) stopForeground(
                    STOP_FOREGROUND_REMOVE
                )
            } catch (_: Exception) {
            }

            // stop this service; accessibility service will show overlay
            stopSelf()
            return START_NOT_STICKY
        }
        showOverlay()
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
                    val chan = NotificationChannel(
                        CHANNEL_ID,
                        "Notification Lighting",
                        NotificationManager.IMPORTANCE_LOW
                    )
                    chan.setSound(null, null)
                    nm.createNotificationChannel(chan)
                }
            }
        }
    }

    private fun showOverlay() {
        // For preview mode, remove existing overlays first to update with new corner radius
        if (isPreview && overlayViews.isNotEmpty()) {
            removeOverlay()
        }

        if (overlayViews.isNotEmpty()) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        try {
            val color = when {
                resolvedColor != null -> resolvedColor!!
                colorMode == NotificationLightingColorMode.CUSTOM -> customColor
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getColor(android.R.color.system_accent1_100)
                    } else {
                        getColor(com.sameerasw.essentials.R.color.purple_500)
                    }
                }
            }

            val overlay = OverlayHelper.createOverlayView(
                this,
                color,
                strokeDp = strokeThicknessDp,
                cornerRadiusDp = cornerRadiusDp,
                style = edgeLightingStyle,
                glowSides = glowSides,
                indicatorScale = indicatorScale,
                showBackground = isAmbientDisplay
            )
            val params = OverlayHelper.createOverlayLayoutParams(getOverlayType())

            if (OverlayHelper.addOverlayView(windowManager, overlay, params)) {
                overlayViews.add(overlay)
                if (isPreview) {
                    // For preview mode, show static preview
                    OverlayHelper.showPreview(
                        overlay,
                        edgeLightingStyle,
                        strokeThicknessDp,
                        indicatorX,
                        indicatorY,
                        indicatorScale
                    )
                } else {
                    // Normal mode: pulse the overlay
                    OverlayHelper.pulseOverlay(
                        overlay,
                        maxPulses = pulseCount,
                        pulseDurationMillis = pulseDuration,
                        style = edgeLightingStyle,
                        strokeWidthDp = strokeThicknessDp,
                        indicatorX = indicatorX,
                        indicatorY = indicatorY,
                        indicatorScale = indicatorScale
                    ) {
                        // When pulsing completes, remove the overlay
                        OverlayHelper.fadeOutAndRemoveOverlay(
                            windowManager,
                            overlay,
                            overlayViews
                        ) {
                            // When all overlays are removed, stop foreground
                            if (overlayViews.isEmpty()) {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        stopForeground(true)
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun getOverlayType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ supports TYPE_ACCESSIBILITY_OVERLAY for AOD visibility
            if (isAccessibilityServiceEnabled()) {
                try {
                    WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY")
                        .getInt(null)
                } catch (_: Exception) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                }
            } else {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0-11: Always use TYPE_APPLICATION_OVERLAY for stability
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
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
        } catch (_: Exception) {
            false
        }
    }

    private fun removeOverlay() {
        // Use fade-out animation for each overlay view
        overlayViews.forEach { view ->
            OverlayHelper.fadeOutAndRemoveOverlay(windowManager, view, overlayViews) {
                // When all overlays are removed, stop foreground
                if (overlayViews.isEmpty()) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            stopForeground(true)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(this)
    }

}