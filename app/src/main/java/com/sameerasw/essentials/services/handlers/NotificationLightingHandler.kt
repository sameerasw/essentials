package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode
import com.sameerasw.essentials.domain.model.NotificationLightingSide
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.utils.OverlayHelper

class NotificationLightingHandler(
    private val service: AccessibilityService
) {
    private var windowManager: WindowManager? = null
    private val overlayViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())

    // Config state
    private var cornerRadiusDp: Float = OverlayHelper.CORNER_RADIUS_DP.toFloat()
    private var strokeThicknessDp: Float = OverlayHelper.STROKE_DP.toFloat()
    var isPreview: Boolean = false
        private set
    private var ignoreScreenState: Boolean = false
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
    private var sweepPosition: String = "CENTER"
    private var sweepThickness: Float = 8f
    private var randomShapes: Boolean = true

    private var isAmbientShowLockScreen: Boolean = false
    private var isAmbientDisplayRequested: Boolean = false
    private var isInterrupted: Boolean = false

    // Queue for staggered playback
    private val intentQueue = java.util.ArrayDeque<Intent>()
    private var currentPackageShowing: String? = null

    fun handleIntent(intent: Intent) {
        if (intent.action == "SHOW_NOTIFICATION_LIGHTING") {
            val isPreviewIntent = intent.getBooleanExtra("is_preview", false)
            val removePreview = intent.getBooleanExtra("remove_preview", false)

            if (removePreview) {
                removeOverlay(immediate = true)
                intentQueue.clear()
                currentPackageShowing = null
                return
            }

            if (isPreviewIntent) {
                removeOverlay(immediate = true)
                intentQueue.clear()
                currentPackageShowing = null
                extractIntentExtras(intent)
                showNotificationLighting()
                return
            }

            val pkg = intent.getStringExtra("package_name")
            if (pkg != null) {
                if (pkg == currentPackageShowing) {
                    return // Skip if same app is already showing
                }
                if (intentQueue.any { it.getStringExtra("package_name") == pkg }) {
                    return // Skip if same app is already in queue
                }
            }

            intentQueue.add(intent)
            processQueue()
        }
    }

    private fun extractIntentExtras(intent: Intent) {
        cornerRadiusDp =
            intent.getFloatExtra("corner_radius_dp", OverlayHelper.CORNER_RADIUS_DP.toFloat())
        strokeThicknessDp =
            intent.getFloatExtra("stroke_thickness_dp", OverlayHelper.STROKE_DP.toFloat())
        isPreview = intent.getBooleanExtra("is_preview", false)
        ignoreScreenState = intent.getBooleanExtra("ignore_screen_state", false)
        colorMode = NotificationLightingColorMode.valueOf(
            intent.getStringExtra("color_mode") ?: "SYSTEM"
        )
        customColor = intent.getIntExtra("custom_color", 0)
        resolvedColor = if (intent.hasExtra("resolved_color")) intent.getIntExtra(
            "resolved_color",
            0
        ) else null
        pulseCount = intent.getIntExtra("pulse_count", 1)
        pulseDuration = intent.getLongExtra("pulse_duration", 3000)
        val styleName = intent.getStringExtra("style")
        edgeLightingStyle =
            if (styleName != null) NotificationLightingStyle.valueOf(styleName) else NotificationLightingStyle.STROKE
        val glowSidesArray = intent.getStringArrayExtra("glow_sides")
        glowSides = glowSidesArray?.mapNotNull {
            try {
                NotificationLightingSide.valueOf(it)
            } catch (_: Exception) {
                null
            }
        }?.toSet()
            ?: setOf(NotificationLightingSide.LEFT, NotificationLightingSide.RIGHT)
        indicatorX = intent.getFloatExtra("indicator_x", 50f)
        indicatorY = intent.getFloatExtra("indicator_y", 2f)
        indicatorScale = intent.getFloatExtra("indicator_scale", 1.0f)
        isAmbientDisplayRequested = intent.getBooleanExtra("is_ambient_display", false)
        isAmbientShowLockScreen = intent.getBooleanExtra("is_ambient_show_lock_screen", false)
        sweepPosition = intent.getStringExtra("sweep_position") ?: "CENTER"
        sweepThickness = intent.getFloatExtra("sweep_thickness", 8f)
        randomShapes = intent.getBooleanExtra("random_shapes", false)
        isInterrupted = false
    }

    private fun processQueue() {
        if (currentPackageShowing != null || intentQueue.isEmpty()) return

        val nextIntent = intentQueue.poll() ?: return
        extractIntentExtras(nextIntent)
        currentPackageShowing = nextIntent.getStringExtra("package_name")
        showNotificationLighting()
    }

    fun onScreenOn() {
        if (!isPreview) {
            val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
            if (onlyShowWhenScreenOff) {
                removeOverlay(immediate = true)
            }
        }
    }

    fun removeOverlay(immediate: Boolean = false) {
        val iterator = overlayViews.iterator()
        while (iterator.hasNext()) {
            val overlay = iterator.next()
            if (immediate) {
                try {
                    windowManager?.removeView(overlay)
                } catch (_: Exception) {}
                iterator.remove()
            } else {
                try {
                    OverlayHelper.fadeOutAndRemoveOverlay(windowManager, overlay, overlayViews)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showNotificationLighting() {
        // Optimization check is now handled by processQueue and currentPackageShowing
        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val powerManager = service.getSystemService(Context.POWER_SERVICE) as PowerManager

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY")
                    .getInt(null)
            } catch (_: Exception) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        try {
            val color = when {
                resolvedColor != null -> resolvedColor!!
                colorMode == NotificationLightingColorMode.CUSTOM -> customColor
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        service.getColor(android.R.color.system_accent1_100)
                    } else {
                        service.getColor(com.sameerasw.essentials.R.color.purple_500)
                    }
                }
            }

            val overlay = OverlayHelper.createOverlayView(
                service,
                color,
                cornerRadiusDp = cornerRadiusDp,
                style = edgeLightingStyle,
                glowSides = glowSides,
                indicatorScale = indicatorScale,
                randomShapes = randomShapes,
                strokeDp = if (edgeLightingStyle == NotificationLightingStyle.SWEEP) sweepThickness else strokeThicknessDp,
            )
            val params = OverlayHelper.createOverlayLayoutParams(overlayType)

            val isScreenOn = powerManager.isInteractive
            val showBackground =
                isAmbientDisplayRequested && !isScreenOn && !isPreview && !isAmbientShowLockScreen

            if (isAmbientDisplayRequested && !isScreenOn && !isPreview) {
                if (showBackground) {
                    val ambientOverlay = OverlayHelper.createOverlayView(
                        service,
                        color,
                        strokeDp = strokeThicknessDp,
                        cornerRadiusDp = cornerRadiusDp,
                        style = edgeLightingStyle,
                        glowSides = glowSides,
                        indicatorScale = indicatorScale,
                        randomShapes = randomShapes,
                        showBackground = true
                    )
                    val ambientParams =
                        OverlayHelper.createOverlayLayoutParams(overlayType, isTouchable = true)

                    ambientOverlay.setOnTouchListener { _, _ ->
                        isInterrupted = true
                        removeOverlay()
                        true
                    }

                    if (OverlayHelper.addOverlayView(
                            windowManager,
                            ambientOverlay,
                            ambientParams
                        )
                    ) {
                        overlayViews.add(ambientOverlay)

                        try {
                            @Suppress("DEPRECATION")
                            val wakeLock = powerManager.newWakeLock(
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                "essentials:NotificationLighting"
                            )
                            wakeLock.acquire(10000L)
                        } catch (e: Exception) {
                            Log.e("NotificationLighting", "Failed to wake screen", e)
                        }

                        handler.postDelayed({
                            if (!isInterrupted) {
                                startPulsing(ambientOverlay)
                            }
                        }, 500)
                    }
                } else {
                    if (OverlayHelper.addOverlayView(windowManager, overlay, params)) {
                        overlayViews.add(overlay)

                        try {
                            @Suppress("DEPRECATION")
                            val wakeLock = powerManager.newWakeLock(
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                "essentials:NotificationLighting"
                            )
                            wakeLock.acquire(10000L)
                        } catch (e: Exception) {
                            Log.e("NotificationLighting", "Failed to wake screen", e)
                        }

                        handler.postDelayed({
                            startPulsing(overlay)
                        }, 500)
                    }
                }
            } else {
                val prefs = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
                val onlyShowWhenScreenOff = prefs.getBoolean("edge_lighting_only_screen_off", true)
                if (onlyShowWhenScreenOff && !ignoreScreenState && !isPreview) {
                    if (isScreenOn) {
                        removeOverlay()
                        return
                    }
                }

                if (OverlayHelper.addOverlayView(windowManager, overlay, params)) {
                    overlayViews.add(overlay)
                    if (isPreview) {
                        OverlayHelper.showPreview(
                            overlay,
                            edgeLightingStyle,
                            if (edgeLightingStyle == NotificationLightingStyle.SWEEP) sweepThickness else strokeThicknessDp,
                            indicatorX = if (edgeLightingStyle == NotificationLightingStyle.SWEEP) {
                                when (sweepPosition) {
                                    "LEFT" -> 0f
                                    "RIGHT" -> 100f
                                    else -> 50f
                                }
                            } else indicatorX,
                            indicatorY,
                            indicatorScale,
                            randomShapes = randomShapes,
                            pulseDurationMillis = pulseDuration
                        ) {
                            currentPackageShowing = null
                            processQueue()
                        }
                    } else {
                        startPulsing(overlay)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPulsing(overlay: View, intent: Intent? = null) {
        OverlayHelper.pulseOverlay(
            overlay,
            maxPulses = if (isPreview) 1 else pulseCount,
            pulseDurationMillis = pulseDuration,
            style = edgeLightingStyle,
            strokeWidthDp = if (edgeLightingStyle == NotificationLightingStyle.SWEEP) sweepThickness else strokeThicknessDp,
            indicatorX = if (edgeLightingStyle == NotificationLightingStyle.SWEEP) {
                when (sweepPosition) {
                    "LEFT" -> 0f
                    "RIGHT" -> 100f
                    else -> 50f
                }
            } else indicatorX,
            indicatorY = indicatorY,
            indicatorScale = indicatorScale,
            randomShapes = randomShapes,
        ) {
            OverlayHelper.fadeOutAndRemoveOverlay(windowManager, overlay, overlayViews) {
                currentPackageShowing = null
                processQueue()
            }
        }
    }
}
