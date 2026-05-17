package com.sameerasw.essentials.services.dreams

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.NotificationListener
import java.io.File
import java.util.Random
import kotlin.math.abs

class AmbientDreamService : DreamService() {

    private var googleSansFlex: Typeface? = null
    private var googleSans: Typeface? = null

    companion object {
        var isDreaming = false
        private var googleSansFlexStatic: Typeface? = null
        private var googleSansStatic: Typeface? = null

        fun getFontFlex(context: Context): Typeface? {
            if (googleSansFlexStatic == null) {
                googleSansFlexStatic = ResourcesCompat.getFont(context, R.font.google_sans_flex)
            }
            return googleSansFlexStatic
        }

        fun getFont(context: Context): Typeface? {
            if (googleSansStatic == null) {
                googleSansStatic = ResourcesCompat.getFont(context, R.font.google_sans_flex)
            }
            return googleSansStatic
        }
    }

    // UI Elements
    private var container: FrameLayout? = null
    private var clockView: TextClock? = null
    private var backgroundImageView: ImageView? = null
    private var backgroundNextImageView: ImageView? = null
    private var backgroundScrim: View? = null
    private var notificationIconsLayout: LinearLayout? = null

    // Music UI
    private var musicContainer: FrameLayout? = null
    private var centerContainer: FrameLayout? = null
    private var clipContainer: FrameLayout? = null
    private var currentPolygon: androidx.graphics.shapes.RoundedPolygon? = null
    private var morphAnimator: android.animation.ValueAnimator? = null
    private var textContainer: LinearLayout? = null
    private var imageView: ImageView? = null
    private var nextImageView: ImageView? = null
    private var titleView: TextView? = null
    private var artistView: TextView? = null
    private var likeStatusView: ImageView? = null
    private var volumeIconView: ImageView? = null
    private var volumeStrokeView: VolumeStrokeView? = null
    private var bottomVolumeProgressView: BottomVolumeProgressView? = null

    private var currentShapePath: Path? = null

    // State
    private var isMusicMode = false
    private var trackTitle: String? = null
    private var artistName: String? = null
    private var isAlreadyLiked: Boolean = false
    private var volumePercentage: Int = 0
    private var volumeKey: Int = -1
    private var eventType: String? = null
    private var targetPackage: String? = null

    private var currentController: android.media.session.MediaController? = null

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        updateActiveSession(sessions)
    }

    private val mediaCallback = object : android.media.session.MediaController.Callback() {
        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            handler.post {
                val title = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                val artist = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)

                trackTitle = title
                artistName = artist

                currentController?.let { isAlreadyLiked = checkIsLiked(it) }

                val artBitmap = extractBitmap(metadata)
                updateMetadata(artBitmap)
            }
        }

        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
            handler.post {
                val isPlaying = state?.state == android.media.session.PlaybackState.STATE_PLAYING
                if (isPlaying && !isMusicMode) {
                    switchToMusicMode()
                } else if (!isPlaying && isMusicMode) {
                    switchToClockMode()
                }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private val volumeHideRunnable = Runnable {
        volumeStrokeView?.animate()?.alpha(0f)?.setDuration(500)?.start()
        bottomVolumeProgressView?.animate()?.alpha(0f)?.setDuration(500)?.start()
    }

    private val burnInProtectionRunnable = object : Runnable {
        override fun run() {
            if (isDetached) return

            // Revert to clock if Android Auto is running
            if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(this@AmbientDreamService)) {
                switchToClockMode()
            }

            shiftUi()
            handler.postDelayed(this, 60000) // Every minute
        }
    }

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            if (isDetached) return

            // Revert to clock if Android Auto is running
            if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(this@AmbientDreamService)) {
                switchToClockMode()
                return
            }

            handler.postDelayed(this, 1000L)
        }
    }

    private var isDetached = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (isDetached) return
            if (intent?.action == "SHOW_AMBIENT_GLANCE") {
                handleIntent(intent)
            }
        }
    }

    private var volumeReceiver: BroadcastReceiver? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false
        isFullscreen = true

        googleSansFlex = getFontFlex(this)
        googleSans = getFont(this)

        isDreaming = true
        setupUI()

        // Register receiver
        val filter = IntentFilter("SHOW_AMBIENT_GLANCE")
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)

        // Register Media Session Listener
        try {
            val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = android.content.ComponentName(
                this,
                com.sameerasw.essentials.services.NotificationListener::class.java
            )
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionListener, componentName)
            updateActiveSession(mediaSessionManager.getActiveSessions(componentName))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request initial state
        val requestIntent = Intent("com.sameerasw.essentials.ACTION_REQUEST_AMBIENT_GLANCE").apply {
            setPackage(packageName)
        }
        sendBroadcast(requestIntent)

        // Also do direct check
        checkDirectly()

        // Start burn-in protection
        handler.post(burnInProtectionRunnable)
    }

    private fun setupUI() {
        container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        setupContentUI(container!!)

        setContentView(container)
    }

    private fun setupContentUI(parentInfo: FrameLayout) {
        val mode = getAlbumArtMode()

        // 0. Background for Fill mode
        backgroundImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = if (mode == "fill" && isMusicMode) 0.7f else 0f
            setImageDrawable(ColorDrawable(Color.DKGRAY))
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.CLAMP))
            }
        }
        backgroundNextImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.CLAMP))
            }
        }
        backgroundScrim = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Radial gradient for vignette effect (dark edges, clear center)
            background = android.graphics.drawable.GradientDrawable().apply {
                gradientType = android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT
                colors = intArrayOf(0x00000000, 0xFF000000.toInt())
                gradientRadius = resources.displayMetrics.widthPixels.toFloat() * 1.1f
                setGradientCenter(0.5f, 0.5f)
            }
            alpha = if (mode == "fill" && isMusicMode) 1f else 0f
        }
        parentInfo.addView(backgroundImageView)
        parentInfo.addView(backgroundNextImageView)
        parentInfo.addView(backgroundScrim)

        // 1. Clock at top
        clockView = object : TextClock(this) {
            override fun onDetachedFromWindow() {
                try {
                    super.onDetachedFromWindow()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }.apply {
            val isFill = mode == "fill" && isMusicMode
            format12Hour = if (isFill) "hh\nmm" else "hh\nmm"
            format24Hour = if (isFill) "HH\nmm" else "HH\nmm"
            setLineSpacing(0f, 0.8f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            typeface = googleSansFlex
            alpha = 0.8f
        }
        parentInfo.addView(clockView)

        // 2. Center Content (Art + Volume Stroke)
        centerContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }

        val size = dpToPx(320f)

        currentPolygon = com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getRandomPolygon()
        currentShapePath =
            com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getRandomShapePath(size.toFloat())

        // Container for clipping
        clipContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        currentShapePath?.let { outline.setPath(it) }
                    } else {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            }
            clipToOutline = true
        }

        imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageDrawable(ColorDrawable(Color.DKGRAY))
        }

        nextImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
        }

        // Dark overlay
        val scrim = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x40000000)
        }

        clipContainer?.addView(imageView)
        clipContainer?.addView(nextImageView)
        clipContainer?.addView(scrim)
        centerContainer?.addView(clipContainer)

        // Volume Icon
        val iconSize = dpToPx(56f)
        volumeIconView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            setColorFilter(Color.WHITE)
            alpha = 0f // Hidden initially
        }
        centerContainer?.addView(volumeIconView)

        // Volume Stroke
        volumeStrokeView = VolumeStrokeView(this, currentShapePath!!, 0).apply {
            layoutParams =
                FrameLayout.LayoutParams(size + dpToPx(20f), size + dpToPx(20f), Gravity.CENTER)
            setColor(Color.GRAY)
            alpha = 0f // Hidden by default, only shown on volume change
        }
        centerContainer?.addView(volumeStrokeView)

        parentInfo.addView(centerContainer)

        // 3. Bottom Text Content
        textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dpToPx(120f)
            }
        }

        titleView = TextView(this).apply {
            textSize = 22f
            typeface = googleSansFlex
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams =
                LinearLayout.LayoutParams(dpToPx(280f), LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply {
                        setMargins(dpToPx(24f), 0, dpToPx(24f), dpToPx(4f))
                    }
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        artistView = TextView(this).apply {
            textSize = 15f
            typeface = googleSansFlex
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams =
                LinearLayout.LayoutParams(dpToPx(240f), LinearLayout.LayoutParams.WRAP_CONTENT)
                    .apply {
                        setMargins(dpToPx(24f), 0, dpToPx(24f), 0)
                    }
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        textContainer?.addView(titleView)
        textContainer?.addView(artistView)

        // Like Status Icon
        likeStatusView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply {
                topMargin = dpToPx(16f)
            }
            setColorFilter(Color.WHITE)
            setImageResource(R.drawable.rounded_favorite_24)
            alpha = 0.8f
        }
        textContainer?.addView(likeStatusView)

        parentInfo.addView(textContainer)

        if (volumeReceiver == null) {
            volumeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (isDetached) return
                    if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                        val audioManager =
                            context?.getSystemService(AUDIO_SERVICE) as? android.media.AudioManager
                        audioManager?.let {
                            val current =
                                it.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            val max = it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val perc = (current.toFloat() / max.toFloat() * 100).toInt()

                            if (isMusicMode) {
                                val isFill = getAlbumArtMode() == "fill"
                                if (isFill) {
                                    bottomVolumeProgressView?.updatePercentage(perc)
                                    bottomVolumeProgressView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                                } else {
                                    volumeStrokeView?.setColor(Color.WHITE)
                                    volumeStrokeView?.updatePercentage(perc)
                                    volumeStrokeView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                                }

                                handler.removeCallbacks(volumeHideRunnable)
                                handler.postDelayed(volumeHideRunnable, 3000)

                                handler.removeCallbacks(revertToMusicRunnable)
                                handler.postDelayed(revertToMusicRunnable, 5000)
                            }
                        }
                    }
                }
            }
            registerReceiver(
                volumeReceiver,
                IntentFilter("android.media.VOLUME_CHANGED_ACTION"),
                RECEIVER_EXPORTED
            )
        }

        // 4. Notification Icons at bottom
        notificationIconsLayout = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dpToPx(60f)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            dividerDrawable = android.graphics.drawable.GradientDrawable().apply {
                setSize(dpToPx(12f), 0)
            }
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        }
        parentInfo.addView(notificationIconsLayout)

        val audioManager = getSystemService(AUDIO_SERVICE) as? android.media.AudioManager
        val initialPerc = audioManager?.let {
            val current = it.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
            val max = it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
            (current.toFloat() / max.toFloat() * 100).toInt()
        } ?: 0

        bottomVolumeProgressView = BottomVolumeProgressView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(240f),
                dpToPx(20f),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dpToPx(30f)
            }
            updatePercentage(initialPerc)
            alpha = 0f
        }
        parentInfo.addView(bottomVolumeProgressView)

        // Hide music elements initially
        centerContainer?.alpha = 0f
        textContainer?.alpha = 0f

        // Clock is visible in Idle Mode (Screensaver default)
        clockView?.alpha = 1f
        clockView?.textSize = 80f // Large for idle
        clockView?.layoutParams = (clockView?.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = Gravity.CENTER
            topMargin = 0
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isDetached = true
        isDreaming = false
        unregisterReceiver(receiver)
        if (volumeReceiver != null) unregisterReceiver(volumeReceiver)

        // Clear active unread notifications from the screensaver on dismissal
        com.sameerasw.essentials.services.NotificationListener.clearUnreadNotifications()

        // Unregister Media Session Listener
        try {
            val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener)
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        handler.removeCallbacksAndMessages(null)

        // Cancel all View animators
        clockView?.animate()?.cancel()
        centerContainer?.animate()?.cancel()
        textContainer?.animate()?.cancel()
        volumeStrokeView?.cleanup()
        bottomVolumeProgressView?.cleanup()
        bottomVolumeProgressView = null
    }

    private fun handleIntent(intent: Intent) {
        if (isDetached) return

        // Skip if Android Auto is running
        if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(this)) {
            switchToClockMode()
            return
        }

        val isPlaying = intent.getBooleanExtra("is_playing", true)
        if (!isPlaying) {
            switchToClockMode()
            return
        }

        if (!isMusicMode) {
            val mediaSessionManager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = android.content.ComponentName(
                this,
                NotificationListener::class.java
            )
            val sessions = try {
                mediaSessionManager.getActiveSessions(componentName)
            } catch (e: Exception) {
                emptyList()
            }
            val anyPlaying = sessions.any { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
            if (!anyPlaying) {
                switchToClockMode()
                return
            }
        }

        eventType = intent.getStringExtra("event_type")
        targetPackage = intent.getStringExtra("package_name")
        val newTitle = intent.getStringExtra("track_title")
        val newArtist = intent.getStringExtra("artist_name")

        val metadataChanged = (newTitle != trackTitle)

        trackTitle = newTitle
        artistName = newArtist

        if (intent.hasExtra("is_already_liked")) {
            isAlreadyLiked = intent.getBooleanExtra("is_already_liked", false)
        }
        volumePercentage = intent.getIntExtra("volume_percentage", 0)
        volumeKey = intent.getIntExtra("volume_key_code", -1)

        // Read unread packages
        val unreadPackages = intent.getStringArrayListExtra("unread_packages") ?: ArrayList()

        if (trackTitle != null) {
            switchToMusicMode()
            if (metadataChanged || eventType == "track_change" || eventType == "notification_update" || eventType == "play_pause") {
                updateMetadata()
            }
            updateNotificationIcons(unreadPackages)

            if (eventType == "volume") {
                val isFill = getAlbumArtMode() == "fill"
                if (isFill) {
                    bottomVolumeProgressView?.updatePercentage(volumePercentage)
                    bottomVolumeProgressView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                } else {
                    if (volumeKey == 24) {
                        volumeIconView?.setImageResource(R.drawable.rounded_volume_up_24)
                    } else if (volumeKey == 25) {
                        volumeIconView?.setImageResource(R.drawable.rounded_volume_down_24)
                    }
                    volumeIconView?.animate()?.alpha(1f)?.setDuration(200)?.start()

                    volumeStrokeView?.setColor(Color.WHITE)
                    volumeStrokeView?.updatePercentage(volumePercentage)
                    volumeStrokeView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                }

                // Show and schedule hide
                handler.removeCallbacks(volumeHideRunnable)
                handler.postDelayed(volumeHideRunnable, 3000)

                handler.removeCallbacks(revertToMusicRunnable)
                handler.postDelayed(revertToMusicRunnable, 5000)
            }

            // Like status animation & refresh
            likeStatusView?.setImageResource(if (isAlreadyLiked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24)
            if (eventType == "like") {
                likeStatusView?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(150)
                    ?.withEndAction {
                        likeStatusView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
                    }?.start()
            } else if (eventType == "like_update") {
                updateMetadata()
            }
        } else {
            switchToClockMode()
            updateNotificationIcons(unreadPackages)
        }
    }

    private fun updateNotificationIcons(packages: List<String>) {
        notificationIconsLayout?.let { layout ->
            layout.removeAllViews()
            packages.distinct().forEach { pkg ->
                try {
                    val icon = packageManager.getApplicationIcon(pkg)
                    val imageView = ImageView(this).apply {
                        layoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(28f), dpToPx(28f))
                        setImageDrawable(icon)
                        alpha = 0.8f
                    }
                    layout.addView(imageView)
                } catch (_: Exception) {
                }
            }
        }
    }

    private val revertToMusicRunnable = Runnable {
        eventType = "play_pause"
        volumeIconView?.animate()?.alpha(0f)?.setDuration(200)?.start()
        volumeStrokeView?.setColor(Color.GRAY)
    }

    private fun updateActiveSession(sessions: List<android.media.session.MediaController>?) {
        if (isDetached) return
        val playingSession =
            sessions?.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }

        if (playingSession != null) {
            if (currentController?.sessionToken != playingSession.sessionToken) {
                currentController?.unregisterCallback(mediaCallback)
                currentController = playingSession
                currentController?.registerCallback(mediaCallback)

                // Initial UI update for new controller
                val metadata = currentController?.metadata
                trackTitle = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                artistName = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                isAlreadyLiked = checkIsLiked(playingSession)

                val artBitmap = extractBitmap(metadata)
                if (playingSession.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) {
                    switchToMusicMode()
                }
                updateMetadata(artBitmap)
                updateNotificationIcons(com.sameerasw.essentials.services.NotificationListener.getUnreadPackages())
            }
        } else {
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
            switchToClockMode()
            updateNotificationIcons(com.sameerasw.essentials.services.NotificationListener.getUnreadPackages())
        }
    }

    private fun checkDirectly() {
        if (isDetached) return
        try {
            val mediaSessionManager =
                getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = getMediaSessions(mediaSessionManager)
            updateActiveSession(sessions)
        } catch (_: Exception) {
            switchToClockMode()
        }
    }

    private fun getMediaSessions(manager: MediaSessionManager): List<android.media.session.MediaController> {
        val componentName = android.content.ComponentName(
            this,
            com.sameerasw.essentials.services.NotificationListener::class.java
        )
        return try {
            manager.getActiveSessions(componentName)
        } catch (e: SecurityException) {
            // Fallback for Android 16+ or restricted environments
            try {
                mutableListOf<android.media.session.MediaController>()
                (getSystemService(android.app.NotificationManager::class.java))?.activeNotifications
                    ?: emptyArray()

                emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun switchToMusicMode() {
        if (isMusicMode) return

        // Skip if Android Auto is running
        if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(this)) {
            return
        }
        isMusicMode = true

        val mode = getAlbumArtMode()
        val isFill = mode == "fill"

        // Move Clock based on mode
        clockView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            if (isFill) {
                clockView?.format12Hour = "hh\nmm"
                clockView?.format24Hour = "HH\nmm"
                clockView?.setLineSpacing(0f, 0.8f)
                applyClockFontVariations(clockView!!, true)
                clockView?.layoutParams = (clockView?.layoutParams as FrameLayout.LayoutParams).apply {
                    gravity = Gravity.CENTER
                    topMargin = -dpToPx(40f)
                }
            } else {
                clockView?.textSize = 24f
                clockView?.format12Hour = "hh:mm"
                clockView?.format24Hour = "HH:mm"
                clockView?.setLineSpacing(0f, 1f)
                applyClockFontVariations(clockView!!, false)
                clockView?.layoutParams = (clockView?.layoutParams as FrameLayout.LayoutParams).apply {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = dpToPx(100f)
                }
            }
            clockView?.requestLayout()
            clockView?.animate()?.alpha(0.8f)?.setDuration(150)?.start()
        }?.start()

        centerContainer?.animate()?.alpha(if (isFill) 0f else 1f)?.setDuration(300)?.start()
        textContainer?.animate()?.alpha(1f)?.setDuration(300)?.start()
        backgroundImageView?.animate()?.alpha(if (isFill) 0.7f else 0f)?.setDuration(300)?.start()
        backgroundScrim?.animate()?.alpha(if (isFill) 1f else 0f)?.setDuration(300)?.start()

        handler.post(progressUpdateRunnable)
    }

    private fun switchToClockMode() {
        if (!isMusicMode) return
        isMusicMode = false

        // Move Clock to Center
        clockView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            clockView?.textSize = 80f
            clockView?.format12Hour = "hh\nmm"
            clockView?.format24Hour = "HH\nmm"
            clockView?.setLineSpacing(0f, 0.8f)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                clockView?.fontVariationSettings = null
            }
            clockView?.layoutParams = (clockView?.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.CENTER
                topMargin = 0
            }
            clockView?.requestLayout()
            clockView?.animate()?.alpha(1f)?.setDuration(150)?.start()
        }?.start()

        centerContainer?.animate()?.alpha(0f)?.setDuration(300)?.start()
        textContainer?.animate()?.alpha(0f)?.setDuration(300)?.start()
        backgroundImageView?.animate()?.alpha(0f)?.setDuration(300)?.start()
        backgroundNextImageView?.animate()?.alpha(0f)?.setDuration(300)?.start()
        backgroundScrim?.animate()?.alpha(0f)?.setDuration(300)?.start()
        bottomVolumeProgressView?.animate()?.alpha(0f)?.setDuration(300)?.start()
    }

    private fun updateMetadata(directBitmap: android.graphics.Bitmap? = null) {
        titleView?.text = trackTitle ?: "Unknown Track"
        artistView?.text = artistName ?: "Unknown Artist"
        likeStatusView?.setImageResource(if (isAlreadyLiked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24)

        // Update Dynamic Shape with Morphing
        val size = dpToPx(320f).toFloat()
        val randomEnabled = isRandomShapesEnabled()
        val mode = getAlbumArtMode()
        val isFill = mode == "fill"

        val newPolygon = com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getPolygon(
            "${trackTitle}_${artistName}",
            randomEnabled
        )

        if (currentPolygon != null && currentPolygon != newPolygon) {
            val morph = androidx.graphics.shapes.Morph(currentPolygon!!, newPolygon)
            morphAnimator?.cancel()
            morphAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 800
                interpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
                addUpdateListener { animator ->
                    val progress = animator.animatedValue as Float
                    currentShapePath?.let { path ->
                        com.sameerasw.essentials.utils.AmbientMusicShapeHelper.updatePathFromMorph(
                            morph, progress, size, path, progress * 360f
                        )
                        volumeStrokeView?.updatePath(path)
                        clipContainer?.invalidateOutline()
                    }

                    nextImageView?.alpha = progress
                    if (isFill) {
                        backgroundNextImageView?.alpha = progress * 0.7f
                        backgroundImageView?.alpha = (1f - progress) * 0.7f
                    }
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        imageView?.setImageDrawable(nextImageView?.drawable)
                        if (isFill) {
                            backgroundImageView?.setImageDrawable(backgroundNextImageView?.drawable)
                            backgroundImageView?.alpha = 0.7f
                        } else {
                            backgroundImageView?.alpha = 0f
                        }
                        nextImageView?.alpha = 0f
                        backgroundNextImageView?.alpha = 0f
                    }
                })
                start()
            }
        } else {
            currentShapePath = com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getShapePath(
                "${trackTitle}_${artistName}",
                size,
                randomEnabled
            )
            volumeStrokeView?.updatePath(currentShapePath!!)
            clipContainer?.invalidateOutline()

            // Update background alpha if not morphing
            backgroundImageView?.alpha = if (isFill) 1f else 0f
            backgroundNextImageView?.alpha = 0f
        }
        currentPolygon = newPolygon

        updateAlbumArt(directBitmap)

        // Handle Background Alpha for Fill mode
        backgroundImageView?.animate()?.alpha(if (isFill) 0.7f else 0f)?.setDuration(500)?.start()
        backgroundScrim?.animate()?.alpha(if (isFill) 1f else 0f)?.setDuration(500)?.start()
        centerContainer?.animate()?.alpha(if (isFill) 0f else 1f)?.setDuration(500)?.start()
        textContainer?.animate()?.alpha(1f)?.setDuration(500)?.start()

        // Update Clock Layout based on mode
        clockView?.let { clock ->
            clock.animate().alpha(0f).setDuration(250).withEndAction {
                clock.format12Hour = if (isFill) "hh\nmm" else "hh:mm"
                clock.format24Hour = if (isFill) "HH\nmm" else "HH:mm"
                applyClockFontVariations(clock, isFill)
                (clock.layoutParams as FrameLayout.LayoutParams).apply {
                    gravity = if (isFill) Gravity.CENTER else Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    topMargin = if (isFill) -dpToPx(40f) else dpToPx(100f)
                }
                clock.requestLayout()
                clock.animate().alpha(0.8f).setDuration(250).start()
            }.start()
        }
    }

    private fun applyBitmaps(bitmap: android.graphics.Bitmap) {
        val isFill = getAlbumArtMode() == "fill"
        nextImageView?.setImageBitmap(bitmap)
        if (isFill) {
            backgroundNextImageView?.setImageBitmap(bitmap)
        }
        if (morphAnimator?.isRunning != true) {
            imageView?.setImageBitmap(bitmap)
            if (isFill) {
                backgroundImageView?.setImageBitmap(bitmap)
            }
        }
    }

    private fun updateAlbumArt(directBitmap: android.graphics.Bitmap? = null, retryCount: Int = 0) {
        if (directBitmap != null) {
            applyBitmaps(directBitmap)
            return
        }

        val title = trackTitle
        val artist = artistName
        if (title == null) return

        try {
            val hashToUse = kotlin.math.abs("${title}_${artist}".hashCode().toLong())

            // 1. Try Memory Cache first (Instant)
            val cachedBitmap = com.sameerasw.essentials.services.NotificationListener.getCachedBitmap(hashToUse)
            if (cachedBitmap != null) {
                applyBitmaps(cachedBitmap)
                return
            }

            // 2. Try Disk Cache
            val artFile = File(cacheDir, "art_$hashToUse.png")
            if (artFile.exists()) {
                Thread {
                    try {
                        val bitmap = BitmapFactory.decodeFile(artFile.absolutePath)
                        if (bitmap != null) {
                            handler.post {
                                if (!isDetached && trackTitle == title && artistName == artist) {
                                    applyBitmaps(bitmap)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
                return
            }

            // 3. Fallback to temp_album_art
            val tempFile = File(cacheDir, "temp_album_art.png")
            if (tempFile.exists()) {
                Thread {
                    try {
                        val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                        if (bitmap != null) {
                            handler.post {
                                if (!isDetached && trackTitle == title && artistName == artist) {
                                    applyBitmaps(bitmap)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }.start()
                return
            }

            // 4. Retry or placeholder
            if (retryCount < 10) {
                handler.postDelayed({ updateAlbumArt(null, retryCount + 1) }, 500)
            } else {
                val placeholder = ColorDrawable(Color.DKGRAY)
                nextImageView?.setImageDrawable(placeholder)
                backgroundNextImageView?.setImageDrawable(placeholder)
                if (morphAnimator?.isRunning != true) {
                    imageView?.setImageDrawable(placeholder)
                    backgroundImageView?.setImageDrawable(placeholder)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun extractBitmap(metadata: android.media.MediaMetadata?): android.graphics.Bitmap? {
        var bitmap = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
        if (bitmap == null) {
            bitmap = metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
        }
        if (bitmap == null) {
            // In DreamService we don't have easy access to SBN, so we rely on metadata URI if available
            val artUri = metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ART_URI)
            if (artUri != null) {
                try {
                    val stream = contentResolver.openInputStream(android.net.Uri.parse(artUri))
                    bitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                } catch (_: Exception) {}
            }
        }
        return bitmap
    }

    private fun checkIsLiked(session: android.media.session.MediaController): Boolean {
        try {
            val pbState = session.playbackState ?: return false
            for (action in pbState.customActions) {
                val name = action.name.toString()
                if (name.contains("Unlike", true) || name.contains(
                        "Unheart",
                        true
                    ) || name.contains("Remove", true)
                ) return true
            }
            val metadata = session.metadata
            if (metadata != null) {
                val rating =
                    metadata.getRating(android.media.MediaMetadata.METADATA_KEY_USER_RATING)
                if (rating != null && (rating.hasHeart() || rating.isThumbUp)) return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun shiftUi() {
        val maxShiftPx = dpToPx(15f).toFloat()
        val random = Random()

        fun getRandomShift() = (random.nextFloat() * 2 * maxShiftPx) - maxShiftPx

        clockView?.animate()?.translationY(getRandomShift())?.setDuration(1000)?.start()
        centerContainer?.animate()?.translationY(getRandomShift())?.setDuration(1000)?.start()
        textContainer?.animate()?.translationY(getRandomShift())?.setDuration(1000)?.start()
    }

    private fun createScallopPath(
        width: Float,
        height: Float,
        count: Int,
        depth: Float
    ): Path {
        val path = Path()
        val radius = width / 2f
        val centerX = width / 2f
        val centerY = height / 2f

        val outerR = radius

        val steps = 360
        for (i in 0..steps) {
            val theta = Math.toRadians(i.toDouble()) - Math.PI / 2
            val r = outerR * (1.0 - (depth / 2.0) * (1.0 - Math.cos(count * theta)))

            val x = centerX + r * Math.cos(theta)
            val y = centerY + r * Math.sin(theta)

            if (i == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()
        return path
    }

    private fun getAlbumArtMode(): String {
        val prefs = getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val selectedMode = prefs.getString(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ALBUM_ART_MODE, "default") ?: "default"

        val forceFillWhileCharging = prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_FORCE_FILL_WHILE_CHARGING, false)
        if (forceFillWhileCharging) {
            val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
            if (isCharging) return "fill"
        }

        return selectedMode
    }

    private fun isRandomShapesEnabled(): Boolean {
        val prefs = getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_RANDOM_SHAPES, false)
    }

    private fun applyClockFontVariations(clock: android.widget.TextClock, isFill: Boolean) {
        val prefs = getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        
        if (isFill) {
            val size = prefs.getInt(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_CLOCK_SIZE, 80)
            clock.textSize = size.toFloat()

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val weight = prefs.getInt(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WEIGHT, 400)
                val width = prefs.getInt(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_CLOCK_WIDTH, 100)
                val roundness = prefs.getInt(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_CLOCK_ROUNDNESS, 50)
                clock.fontVariationSettings = "'wght' $weight, 'wdth' $width, 'ROND' $roundness"
            }
        } else {
            // Fixed styling for standard modes
            clock.textSize = 24f 
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                clock.fontVariationSettings = "'wght' 500, 'wdth' 100, 'ROND' 50"
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        val metrics = resources.displayMetrics
        return (dp * metrics.density).toInt()
    }

    // Copy of Inner Class
    private inner class VolumeStrokeView(
        context: Context,
        private var petalPath: Path,
        private val percentage: Int
    ) : View(context) {
        private var currentPercentage: Float = percentage.toFloat()
        private var animator: android.animation.ValueAnimator? = null

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(6f).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private var pathMeasure = PathMeasure(petalPath, false)
        private val progressPath = Path()
        private var isDetached = false

        fun updatePath(newPath: Path) {
            this.petalPath = newPath
            this.pathMeasure = PathMeasure(newPath, false)
            invalidate()
        }

        fun cleanup() {
            isDetached = true
            animator?.cancel()
        }

        fun updatePercentage(newPercentage: Int) {
            animator?.cancel()
            animator =
                android.animation.ValueAnimator.ofFloat(currentPercentage, newPercentage.toFloat())
                    .apply {
                        duration = 300
                        interpolator = android.view.animation.DecelerateInterpolator()
                        addUpdateListener {
                            currentPercentage = it.animatedValue as Float
                            invalidate()
                        }
                        start()
                    }
        }

        fun setColor(color: Int) {
            val startColor = paint.color
            android.animation.ValueAnimator.ofArgb(startColor, color).apply {
                duration = 200
                addUpdateListener {
                    paint.color = it.animatedValue as Int
                    invalidate()
                }
                start()
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (isDetached) return
            super.onDraw(canvas)
            val length = pathMeasure.length
            val end = length * (currentPercentage / 100f)
            progressPath.reset()
            pathMeasure.getSegment(0f, end, progressPath, true)

            val offset = dpToPx(10f).toFloat()
            canvas.save()
            canvas.translate(offset, offset)
            canvas.drawPath(progressPath, paint)
            canvas.restore()
        }
    }

    private inner class BottomVolumeProgressView(context: Context) : View(context) {
        private var currentPercentage: Float = 0f
        private var animator: android.animation.ValueAnimator? = null
        private var waveAnimator: android.animation.ValueAnimator? = null
        private var phaseShift = 0f

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(3f).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x33FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(3f).toFloat()
            strokeCap = Paint.Cap.ROUND
        }

        private val path = Path()
        private val trackPath = Path()

        init {
            waveAnimator = android.animation.ValueAnimator.ofFloat(0f, (2 * Math.PI).toFloat()).apply {
                duration = 1500
                repeatCount = android.animation.ValueAnimator.INFINITE
                interpolator = android.view.animation.LinearInterpolator()
                addUpdateListener {
                    phaseShift = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun updatePercentage(newPercentage: Int) {
            animator?.cancel()
            animator = android.animation.ValueAnimator.ofFloat(currentPercentage, newPercentage.toFloat()).apply {
                duration = 300
                interpolator = android.view.animation.DecelerateInterpolator()
                addUpdateListener {
                    currentPercentage = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun cleanup() {
            animator?.cancel()
            waveAnimator?.cancel()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()
            val centerY = h / 2f

            // Track (straight line)
            trackPath.reset()
            trackPath.moveTo(0f, centerY)
            trackPath.lineTo(w, centerY)
            canvas.drawPath(trackPath, trackPaint)

            // Progress Wavy Line
            val progressWidth = w * (currentPercentage / 100f)
            if (progressWidth > 0f) {
                path.reset()
                path.moveTo(0f, centerY)

                val waveLength = dpToPx(24f).toFloat()
                val amplitude = dpToPx(3f).toFloat()
                val frequency = (2 * Math.PI) / waveLength

                var x = 0f
                val step = dpToPx(1f).toFloat()
                while (x <= progressWidth) {
                    val y = centerY + amplitude * kotlin.math.sin(frequency * x - phaseShift).toFloat()
                    path.lineTo(x, y)
                    x += step
                }
                canvas.drawPath(path, paint)
            }
        }
    }
}
