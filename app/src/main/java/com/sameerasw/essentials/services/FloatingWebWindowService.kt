/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Overlay Windows
 * File: FloatingWebWindowService.kt
 * Description: Interactive floating web window overlay with verified universal browsing engine, dynamic Monet theme adaptation, off-screen auto-docking, and Vivo gestures.
 */

package com.sameerasw.essentials.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.animation.ValueAnimator
import android.net.http.SslError
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.theme.Pink40
import com.sameerasw.essentials.ui.theme.Pink80
import com.sameerasw.essentials.ui.theme.Purple40
import com.sameerasw.essentials.ui.theme.Purple80
import com.sameerasw.essentials.ui.theme.PurpleGrey40
import com.sameerasw.essentials.ui.theme.PurpleGrey80
import kotlin.math.max
import kotlin.math.min

class FloatingWebWindowService : Service() {

    private var windowManager: WindowManager? = null
    private var windowView: FrameLayout? = null
    private var bubbleView: FrameLayout? = null
    private var webView: WebView? = null
    private var progressBar: ProgressBar? = null
    private var titleText: TextView? = null

    private var windowParams: WindowManager.LayoutParams? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private var currentUrl: String = "https://google.com"
    private var isPrivateMode: Boolean = true
    private var isMaximized = false
    private var savedWidth = 0
    private var savedHeight = 0
    private var savedX = 0
    private var savedY = 0

    // Dynamic Monet Material 3 Color Tokens
    private var containerBgColor: Int = 0
    private var headerBgColor: Int = 0
    private var addressCapsuleBgColor: Int = 0
    private var textPrimaryColor: Int = 0
    private var accentPrimaryColor: Int = 0
    private var strokeBorderColor: Int = 0
    private var buttonBgColor: Int = 0

    companion object {
        private const val TAG = "FloatingWebWindow"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_PRIVATE_MODE = "extra_private_mode"
        private const val CHANNEL_ID = "floating_web_window_channel"
        private const val NOTIFICATION_ID = 9021

        fun start(context: Context, url: String, isPrivate: Boolean = true) {
            val intent = Intent(context, FloatingWebWindowService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_PRIVATE_MODE, isPrivate)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_URL)?.let { url ->
            currentUrl = url
            isPrivateMode = intent.getBooleanExtra(EXTRA_PRIVATE_MODE, true)
            if (windowView == null && bubbleView == null) {
                if (Settings.canDrawOverlays(this)) {
                    createFloatingWindow()
                } else {
                    Log.w(TAG, "Cannot draw overlays: permission missing")
                    stopSelf()
                }
            } else if (windowView != null) {
                webView?.loadUrl(currentUrl)
            }
        }
        return START_NOT_STICKY
    }

    private fun resolveDynamicThemeColors() {
        val isSystemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val prefs = getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val isPitchBlack = prefs.getBoolean(SettingsRepository.KEY_PITCH_BLACK_THEME_ENABLED, false)

        val fallbackDark = darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)
        val fallbackLight = lightColorScheme(primary = Purple40, secondary = PurpleGrey40, tertiary = Pink40)

        val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isSystemDark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
        } else {
            if (isSystemDark) fallbackDark else fallbackLight
        }

        if (isPitchBlack && isSystemDark) {
            containerBgColor = Color.BLACK
            headerBgColor = Color.parseColor("#FF0A0A0E")
            addressCapsuleBgColor = Color.parseColor("#FF16161C")
            textPrimaryColor = scheme.onSurface.toArgb()
            accentPrimaryColor = scheme.primary.toArgb()
            strokeBorderColor = Color.parseColor("#33FFFFFF")
            buttonBgColor = Color.parseColor("#20FFFFFF")
        } else {
            containerBgColor = scheme.surfaceContainer.toArgb()
            headerBgColor = scheme.surfaceContainerHigh.toArgb()
            addressCapsuleBgColor = scheme.surfaceContainerHighest.toArgb()
            textPrimaryColor = scheme.onSurface.toArgb()
            accentPrimaryColor = scheme.primary.toArgb()
            strokeBorderColor = scheme.outlineVariant.copy(alpha = 0.35f).toArgb()
            buttonBgColor = scheme.surfaceBright.copy(alpha = 0.5f).toArgb()
        }
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Browser Window",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Active floating browser overlay"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Private Web Preview")
            .setContentText("Tap controls to minimize, expand, or switch to full screen")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun createFloatingWindow() {
        val wm = windowManager ?: return
        resolveDynamicThemeColors()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        savedWidth = (screenWidth * 0.90f).toInt()
        savedHeight = (screenHeight * 0.74f).toInt()

        val paramsType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        windowParams = WindowManager.LayoutParams(
            savedWidth,
            savedHeight,
            paramsType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - savedWidth) / 2
            y = (screenHeight - savedHeight) / 3
        }

        // Root Window Container with Elevated Material 3 Surface and Rounded Outline
        val root = FrameLayout(this)
        val bgDrawable = GradientDrawable().apply {
            setColor(containerBgColor)
            cornerRadius = dpToPx(28f)
            setStroke(dpToPx(1.5f).toInt(), strokeBorderColor)
        }
        root.background = bgDrawable
        root.elevation = dpToPx(16f)
        root.clipToOutline = true

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }

        // 1. Material 3 Top App Bar Container (Header)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(headerBgColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        // M3 Drag Handle Capsule at Top Center
        val dragHandleContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(14f).toInt(),
            )
        }
        val dragPill = View(this).apply {
            val pillWidth = dpToPx(36f).toInt()
            val pillHeight = dpToPx(4f).toInt()
            layoutParams = FrameLayout.LayoutParams(pillWidth, pillHeight).apply {
                gravity = Gravity.CENTER
            }
            background = GradientDrawable().apply {
                setColor(accentPrimaryColor)
                alpha = 80
                cornerRadius = dpToPx(2f)
            }
        }
        dragHandleContainer.addView(dragPill)
        header.addView(dragHandleContainer)

        // Actions & Address Bar Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(8f).toInt(), 0, dpToPx(8f).toInt(), dpToPx(8f).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        // Back Button
        val btnBack = createM3IconButton(R.drawable.rounded_arrow_back_24) {
            if (webView?.canGoBack() == true) {
                webView?.goBack()
            }
        }
        actionsRow.addView(btnBack)

        // Reload Button
        val btnReload = createM3IconButton(R.drawable.rounded_refresh_24) {
            webView?.reload()
        }
        actionsRow.addView(btnReload)

        // M3 Address Capsule (SearchBar pill container)
        val addressCapsule = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(addressCapsuleBgColor)
                cornerRadius = dpToPx(18f)
            }
            setPadding(dpToPx(10f).toInt(), dpToPx(6f).toInt(), dpToPx(10f).toInt(), dpToPx(6f).toInt())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f).apply {
                setMargins(dpToPx(6f).toInt(), 0, dpToPx(6f).toInt(), 0)
            }
        }

        // Privacy Lock / Shield Icon
        val lockIcon = ImageView(this).apply {
            val drawable = ContextCompat.getDrawable(context, R.drawable.rounded_shield_lock_24)
                ?: ContextCompat.getDrawable(context, android.R.drawable.ic_lock_lock)
            setImageDrawable(drawable)
            setColorFilter(accentPrimaryColor)
            layoutParams = LinearLayout.LayoutParams(dpToPx(16f).toInt(), dpToPx(16f).toInt()).apply {
                setMargins(0, 0, dpToPx(6f).toInt(), 0)
            }
        }
        addressCapsule.addView(lockIcon)

        // Title / Host Text
        titleText = TextView(this).apply {
            text = formatDisplayUrl(currentUrl)
            setTextColor(textPrimaryColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MIDDLE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        addressCapsule.addView(titleText)
        actionsRow.addView(addressCapsule)

        // Minimize Button
        val btnMinimize = createM3IconButton(R.drawable.rounded_remove_24) {
            minimizeToBubble()
        }
        actionsRow.addView(btnMinimize)

        // Maximize / Restore Toggle
        val btnMaximize = createM3IconButton(R.drawable.rounded_open_in_full_24) {
            toggleMaximize()
        }
        actionsRow.addView(btnMaximize)

        // Switch to Full Stack Native Browser
        val btnExternal = createM3IconButton(R.drawable.rounded_open_in_browser_24) {
            switchToFullStackBrowser()
        }
        actionsRow.addView(btnExternal)

        // Close Button
        val btnClose = createM3IconButton(R.drawable.rounded_close_24, isDestructive = true) {
            closeWindow()
        }
        actionsRow.addView(btnClose)

        header.addView(actionsRow)
        mainLayout.addView(header)

        // Dynamic Header Dragging & >50% Off-Screen Auto-Docking
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                toggleMaximize()
                return true
            }
        })

        header.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            val p = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - initialTouchX).toInt()
                    p.y = initialY + (event.rawY - initialTouchY).toInt()
                    wm.updateViewLayout(root, p)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val is50PercentOffLeft = p.x < -(p.width * 0.45f)
                    val is50PercentOffRight = p.x > (screenWidth - p.width * 0.55f)

                    if (is50PercentOffLeft) {
                        minimizeToBubble(dockSideLeft = true)
                    } else if (is50PercentOffRight) {
                        minimizeToBubble(dockSideLeft = false)
                    } else {
                        val clampedX = p.x.coerceIn(dpToPx(8f).toInt(), max(dpToPx(8f).toInt(), screenWidth - p.width - dpToPx(8f).toInt()))
                        val clampedY = p.y.coerceIn(dpToPx(24f).toInt(), max(dpToPx(24f).toInt(), screenHeight - p.height - dpToPx(24f).toInt()))
                        p.x = clampedX
                        p.y = clampedY
                        savedX = clampedX
                        savedY = clampedY
                        wm.updateViewLayout(root, p)
                    }
                    true
                }
                else -> false
            }
        }

        // 2. M3 Linear Loading Progress Bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = View.VISIBLE
            androidx.core.graphics.BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                accentPrimaryColor,
                androidx.core.graphics.BlendModeCompat.SRC_IN,
            )?.let { progressDrawable?.colorFilter = it }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(3f).toInt(),
            )
        }
        mainLayout.addView(progressBar)

        // 3. Embedded WebView Container with Universal Compatibility & Privacy Sandbox
        val webContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f,
            )
        }

        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val p = windowParams ?: return false
                val factor = detector.scaleFactor

                val newWidth = (p.width * factor).toInt().coerceIn(dpToPx(280f).toInt(), screenWidth)
                val newHeight = (p.height * factor).toInt().coerceIn(dpToPx(360f).toInt(), screenHeight)

                p.width = newWidth
                p.height = newHeight
                savedWidth = newWidth
                savedHeight = newHeight

                wm.updateViewLayout(root, p)
                return true
            }
        })

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                allowContentAccess = false
                allowFileAccess = false
                setGeolocationEnabled(false)
                userAgentString = userAgentString.replace("; wv", "")
            }

            val cm = CookieManager.getInstance()
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(this, false)

            setOnTouchListener { _, event ->
                if (event.pointerCount >= 2) {
                    scaleGestureDetector.onTouchEvent(event)
                    true
                } else {
                    false
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url ?: return false
                    val scheme = url.scheme?.lowercase() ?: return false
                    return if (scheme == "http" || scheme == "https") {
                        false
                    } else {
                        try {
                            val intent = Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                component = null
                                selector = null
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.w(TAG, "Cannot handle custom scheme: $scheme", e)
                        }
                        true
                    }
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                    Log.w(TAG, "SSL Certificate error: $error, aborting connection for security.")
                    handler?.cancel()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    url?.let {
                        titleText?.text = formatDisplayUrl(it)
                        currentUrl = it
                    }
                    progressBar?.visibility = View.GONE
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (newProgress < 100) {
                        progressBar?.visibility = View.VISIBLE
                        progressBar?.progress = newProgress
                    } else {
                        progressBar?.visibility = View.GONE
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrBlank()) {
                        titleText?.text = title
                    }
                }
            }
            loadUrl(currentUrl)
        }
        webContainer.addView(webView)

        // 4. Vivo Bottom-Right Corner Resize Grip Handle
        val resizeHandleRight = FrameLayout(this).apply {
            val handleSize = dpToPx(36f).toInt()
            layoutParams = FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            val handleIcon = ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_resize_handle_bottom_right))
                setColorFilter(accentPrimaryColor)
                alpha = 0.65f
                val iconSize = dpToPx(16f).toInt()
                layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, dpToPx(6f).toInt(), dpToPx(6f).toInt())
                }
            }
            addView(handleIcon)
        }

        var resizeInitialWidth = 0
        var resizeInitialHeight = 0
        var resizeTouchX = 0f
        var resizeTouchY = 0f

        resizeHandleRight.setOnTouchListener { _, event ->
            val p = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resizeInitialWidth = p.width
                    resizeInitialHeight = p.height
                    resizeTouchX = event.rawX
                    resizeTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - resizeTouchX).toInt()
                    val deltaY = (event.rawY - resizeTouchY).toInt()

                    val newWidth = max(dpToPx(280f).toInt(), min(screenWidth, resizeInitialWidth + deltaX))
                    val newHeight = max(dpToPx(360f).toInt(), min(screenHeight, resizeInitialHeight + deltaY))

                    p.width = newWidth
                    p.height = newHeight
                    savedWidth = newWidth
                    savedHeight = newHeight

                    wm.updateViewLayout(root, p)
                    true
                }
                else -> false
            }
        }
        webContainer.addView(resizeHandleRight)

        // 5. Vivo Bottom-Left Corner Resize Grip Handle
        val resizeHandleLeft = FrameLayout(this).apply {
            val handleSize = dpToPx(36f).toInt()
            layoutParams = FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.START
            }
            val handleIcon = ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.rounded_resize_handle_bottom_left))
                setColorFilter(accentPrimaryColor)
                alpha = 0.65f
                val iconSize = dpToPx(16f).toInt()
                layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.BOTTOM or Gravity.START
                    setMargins(dpToPx(6f).toInt(), 0, 0, dpToPx(6f).toInt())
                }
            }
            addView(handleIcon)
        }

        var resizeInitialX = 0
        resizeHandleLeft.setOnTouchListener { _, event ->
            val p = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    resizeInitialX = p.x
                    resizeInitialWidth = p.width
                    resizeInitialHeight = p.height
                    resizeTouchX = event.rawX
                    resizeTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - resizeTouchX).toInt()
                    val deltaY = (event.rawY - resizeTouchY).toInt()

                    val newWidth = max(dpToPx(280f).toInt(), min(screenWidth, resizeInitialWidth - deltaX))
                    val newHeight = max(dpToPx(360f).toInt(), min(screenHeight, resizeInitialHeight + deltaY))

                    p.x = resizeInitialX + (resizeInitialWidth - newWidth)
                    p.width = newWidth
                    p.height = newHeight
                    savedWidth = newWidth
                    savedHeight = newHeight

                    wm.updateViewLayout(root, p)
                    true
                }
                else -> false
            }
        }
        webContainer.addView(resizeHandleLeft)

        // 6. Vivo Bottom Bar with Swipe-Up To Minimize to Bubble
        val bottomBar = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(20f).toInt(),
            ).apply {
                gravity = Gravity.BOTTOM
            }
            val centerPill = View(context).apply {
                val pillWidth = dpToPx(48f).toInt()
                val pillHeight = dpToPx(4f).toInt()
                layoutParams = FrameLayout.LayoutParams(pillWidth, pillHeight).apply {
                    gravity = Gravity.CENTER
                }
                background = GradientDrawable().apply {
                    setColor(accentPrimaryColor)
                    alpha = 70
                    cornerRadius = dpToPx(2f)
                }
            }
            addView(centerPill)
        }

        var bottomTouchStartY = 0f
        bottomBar.setOnTouchListener { _, event ->
            val p = windowParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    bottomTouchStartY = event.rawY
                    resizeInitialHeight = p.height
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = (event.rawY - bottomTouchStartY).toInt()
                    if (deltaY > 0) {
                        val newHeight = min(screenHeight, resizeInitialHeight + deltaY)
                        p.height = newHeight
                        savedHeight = newHeight
                        wm.updateViewLayout(root, p)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = bottomTouchStartY - event.rawY
                    if (deltaY >= dpToPx(60f)) {
                        minimizeToBubble()
                    }
                    true
                }
                else -> false
            }
        }
        webContainer.addView(bottomBar)

        mainLayout.addView(webContainer)
        root.addView(mainLayout)

        windowView = root

        root.alpha = 0f
        root.scaleX = 0.88f
        root.scaleY = 0.88f
        root.translationY = dpToPx(16f)
        wm.addView(windowView, windowParams)

        root.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(220)
            .setInterpolator(PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f))
            .start()
    }

    private fun createM3IconButton(iconRes: Int, isDestructive: Boolean = false, onClick: () -> Unit): FrameLayout {
        val size = dpToPx(34f).toInt()
        val frame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(dpToPx(2f).toInt(), 0, dpToPx(2f).toInt(), 0)
            }

            val normalBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (isDestructive) Color.parseColor("#26FF897D") else buttonBgColor)
            }

            val rippleColor = if (isDestructive) Color.parseColor("#4DFFB4AB") else Color.parseColor("#33D0BCFF")
            background = RippleDrawable(android.content.res.ColorStateList.valueOf(rippleColor), normalBg, null)

            val iv = ImageView(context).apply {
                val drawable = ContextCompat.getDrawable(context, iconRes)
                setImageDrawable(drawable)
                val tint = if (isDestructive) Color.parseColor("#FFFFB4AB") else textPrimaryColor
                setColorFilter(tint)
                val iconSize = dpToPx(18f).toInt()
                layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
            }
            addView(iv)
            setOnClickListener { onClick() }
        }
        return frame
    }

    private fun formatDisplayUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            uri.host ?: url
        } catch (_: Exception) {
            url
        }
    }

    private fun getCircularAppIconBitmap(context: Context, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val path = Path().apply {
            addCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Path.Direction.CW)
        }
        canvas.clipPath(path)

        val drawable = try {
            context.packageManager.getApplicationIcon(context.packageName)
        } catch (_: Exception) {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        }
        drawable?.setBounds(0, 0, sizePx, sizePx)
        drawable?.draw(canvas)
        return bitmap
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun minimizeToBubble(dockSideLeft: Boolean = true) {
        val wm = windowManager ?: return
        val root = windowView ?: return

        val targetX = if (dockSideLeft) -dpToPx(60f) else dpToPx(60f)
        root.animate()
            .alpha(0f)
            .scaleX(0.2f)
            .scaleY(0.2f)
            .translationX(targetX)
            .translationY(dpToPx(40f))
            .setDuration(180)
            .setInterpolator(AccelerateInterpolator(1.6f))
            .withEndAction {
                wm.removeView(root)
                windowView = null
                createBubbleView(dockSideLeft)
            }
            .start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBubbleView(dockSideLeft: Boolean = true) {
        val wm = windowManager ?: return
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val bubbleSize = dpToPx(56f).toInt()

        val startX = if (dockSideLeft) dpToPx(16f).toInt() else screenWidth - bubbleSize - dpToPx(16f).toInt()

        bubbleParams = WindowManager.LayoutParams(
            bubbleSize,
            bubbleSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = startX
            y = dpToPx(120f).toInt()
        }

        val bubble = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(headerBgColor)
                setStroke(dpToPx(2.5f).toInt(), accentPrimaryColor)
            }
            background = bg
            elevation = dpToPx(14f)
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true

            // Perfectly Circular App Logo Icon
            val icon = ImageView(context).apply {
                val iconSizePx = dpToPx(44f).toInt()
                val circularAppIcon = getCircularAppIconBitmap(context, iconSizePx)
                setImageBitmap(circularAppIcon)
                layoutParams = FrameLayout.LayoutParams(iconSizePx, iconSizePx).apply {
                    gravity = Gravity.CENTER
                }
            }
            addView(icon)

            // Private Mode Mini Shield Indicator Badge
            if (isPrivateMode) {
                val badge = ImageView(context).apply {
                    val shield = ContextCompat.getDrawable(context, R.drawable.rounded_shield_lock_24)
                    setImageDrawable(shield)
                    setColorFilter(accentPrimaryColor)
                    val badgeBg = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(containerBgColor)
                        setStroke(dpToPx(1f).toInt(), accentPrimaryColor)
                    }
                    background = badgeBg
                    val badgeSize = dpToPx(18f).toInt()
                    layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                        gravity = Gravity.BOTTOM or Gravity.END
                        setMargins(0, 0, dpToPx(2f).toInt(), dpToPx(2f).toInt())
                    }
                    setPadding(dpToPx(2f).toInt(), dpToPx(2f).toInt(), dpToPx(2f).toInt(), dpToPx(2f).toInt())
                }
                addView(badge)
            }
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        bubble.setOnTouchListener { _, event ->
            val p = bubbleParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    bubble.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 8 || Math.abs(deltaY) > 8) {
                        isClick = false
                    }
                    p.x = initialX + deltaX
                    p.y = initialY + deltaY
                    wm.updateViewLayout(bubble, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    bubble.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    if (isClick) {
                        bubble.animate()
                            .alpha(0f)
                            .scaleX(1.35f)
                            .scaleY(1.35f)
                            .setDuration(150)
                            .withEndAction {
                                wm.removeView(bubble)
                                bubbleView = null
                                createFloatingWindow()
                            }
                            .start()
                    } else {
                        val currentX = p.x
                        val snapLeft = currentX < screenWidth / 2
                        val targetX = if (snapLeft) dpToPx(16f).toInt() else screenWidth - bubbleSize - dpToPx(16f).toInt()

                        ValueAnimator.ofInt(currentX, targetX).apply {
                            duration = 180
                            interpolator = DecelerateInterpolator(1.6f)
                            addUpdateListener { va ->
                                p.x = va.animatedValue as Int
                                wm.updateViewLayout(bubble, p)
                            }
                            start()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        bubble.alpha = 0f
        bubble.scaleX = 0.3f
        bubble.scaleY = 0.3f
        bubbleView = bubble
        wm.addView(bubbleView, bubbleParams)

        bubble.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220)
            .setInterpolator(OvershootInterpolator(1.35f))
            .start()
    }

    private fun toggleMaximize() {
        val wm = windowManager ?: return
        val v = windowView ?: return
        val p = windowParams ?: return
        val displayMetrics = resources.displayMetrics

        val startX = p.x
        val startY = p.y
        val startWidth = p.width
        val startHeight = p.height

        val endX: Int
        val endY: Int
        val endWidth: Int
        val endHeight: Int

        if (!isMaximized) {
            savedX = p.x
            savedY = p.y
            savedWidth = p.width
            savedHeight = p.height

            endX = 0
            endY = 0
            endWidth = displayMetrics.widthPixels
            endHeight = displayMetrics.heightPixels
            isMaximized = true
        } else {
            endX = savedX
            endY = savedY
            endWidth = savedWidth
            endHeight = savedHeight
            isMaximized = false
        }

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { va ->
                val f = va.animatedFraction
                p.x = (startX + (endX - startX) * f).toInt()
                p.y = (startY + (endY - startY) * f).toInt()
                p.width = (startWidth + (endWidth - startWidth) * f).toInt()
                p.height = (startHeight + (endHeight - startHeight) * f).toInt()
                wm.updateViewLayout(v, p)
            }
            start()
        }
    }

    private fun switchToFullStackBrowser() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
            closeWindow()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch to full stack browser", e)
        }
    }

    private fun closeWindow() {
        val wm = windowManager
        val root = windowView
        if (root != null) {
            root.animate()
                .alpha(0f)
                .scaleX(0.85f)
                .scaleY(0.85f)
                .translationY(dpToPx(16f))
                .setDuration(150)
                .setInterpolator(AccelerateInterpolator(1.8f))
                .withEndAction {
                    cleanupAndStop()
                }
                .start()
        } else {
            cleanupAndStop()
        }
    }

    private fun cleanupAndStop() {
        val wm = windowManager
        try {
            webView?.clearCache(true)
            webView?.clearHistory()
            webView?.clearFormData()
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().removeAllCookies(null)

            windowView?.let { wm?.removeView(it) }
            bubbleView?.let { wm?.removeView(it) }
            windowView = null
            bubbleView = null
            webView?.destroy()
            webView = null
        } catch (e: Exception) {
            Log.w(TAG, "Error closing overlay window", e)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        closeWindow()
        super.onDestroy()
    }
}
