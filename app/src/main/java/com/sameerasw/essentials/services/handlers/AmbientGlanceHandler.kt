package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
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
import android.graphics.PixelFormat
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import java.io.File
import java.util.Random

class AmbientGlanceHandler(
    private val service: AccessibilityService
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var volumeStrokeView: VolumeStrokeView? = null
    private var volumeIconView: ImageView? = null
    private var bottomVolumeProgressView: BottomVolumeProgressView? = null
    private var likeStatusView: ImageView? = null
    private var volumeReceiver: BroadcastReceiver? = null
    private val handler = Handler(Looper.getMainLooper())

    private val volumeHideRunnable = Runnable {
        volumeStrokeView?.animate()?.alpha(0f)?.setDuration(500)?.start()
        bottomVolumeProgressView?.animate()?.alpha(0f)?.setDuration(500)?.start()
    }

    private val temporaryHideRunnable = Runnable {
        if (overlayView != null) {
            // Check if media is still playing
            val mediaSessionManager =
                service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName =
                android.content.ComponentName(service, ScreenOffAccessibilityService::class.java)
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            val isPlaying =
                sessions.any { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }

            if (isPlaying) {
                overlayView?.animate()?.alpha(1f)?.setDuration(500)?.start()
            } else {
                fadeOutAndRemove()
            }
        }
    }

    private var notificationIconsLayout: android.widget.LinearLayout? = null
    private var clockView: android.widget.TextClock? = null
    private var centerContainer: FrameLayout? = null
    private var clipContainer: FrameLayout? = null
    private var textContainer: LinearLayout? = null
    private var backgroundImageView: ImageView? = null
    private var backgroundNextImageView: ImageView? = null
    private var backgroundScrim: View? = null

    private var currentShapePath: Path? = null
    private var currentPolygon: androidx.graphics.shapes.RoundedPolygon? = null
    private var morphAnimator: android.animation.ValueAnimator? = null

    private var imageView: ImageView? = null
    private var nextImageView: ImageView? = null
    private var titleView: TextView? = null
    private var artistView: TextView? = null

    private val burnInProtectionRunnable = object : Runnable {
        override fun run() {
            if (overlayView == null || !isDockedMode) return

            // Dismiss if Android Auto is running
            if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(service)) {
                fadeOutAndRemove()
                return
            }

            // Randomly shift elements by a few pixels (-15dp to +15dp)
            val maxShiftPx = dpToPx(15f).toFloat()
            val random = Random()

            fun getRandomShift() = (random.nextFloat() * 2 * maxShiftPx) - maxShiftPx

            clockView?.animate()?.translationY(getRandomShift())?.setDuration(1000)?.start()
            centerContainer?.animate()?.translationY(getRandomShift())?.setDuration(1000)?.start()
            textContainer?.animate()?.translationY(getRandomShift())?.setDuration(1000)?.start()

            handler.postDelayed(this, 60000L) // Once every minute
        }
    }

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            if (overlayView == null || isDetached) return

            // Dismiss if Android Auto is running
            if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(service)) {
                fadeOutAndRemove()
                return
            }

            // Dismiss if music stops/pauses
            val mediaSessionManager =
                service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName =
                android.content.ComponentName(service, ScreenOffAccessibilityService::class.java)
            val sessions = mediaSessionManager.getActiveSessions(componentName)
            val anyPlaying =
                sessions.any { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }

            if (!anyPlaying) {
                fadeOutAndRemove()
                return
            }

            handler.postDelayed(this, 1000L)
        }
    }

    private var isDetached = false

    private val revertToMusicRunnable = Runnable {
        if (overlayView != null && isDockedMode) {
            eventType = EVENT_PLAY_PAUSE // Switch back to music view
            volumeIconView?.animate()?.alpha(0f)?.setDuration(200)?.start()
            volumeStrokeView?.setColor(Color.GRAY)
            handler.post(progressUpdateRunnable)
        }
    }

    private val DISPLAY_DURATION = 5000L

    private var eventType: String? = null
    private var trackTitle: String? = null
    private var artistName: String? = null
    private var isAlreadyLiked: Boolean = false
    private var isDockedMode: Boolean = false
    private var volumePercentage: Int = 0
    private var volumeKey: Int = -1
    private var artHash: Long = -1L

    companion object {
        const val EVENT_LIKE = "like"
        const val EVENT_PLAY_PAUSE = "play_pause"
        const val EVENT_TRACK_CHANGE = "track_change"
        const val EVENT_VOLUME = "volume"
    }

    private val hideRunnable = Runnable {
        fadeOutAndRemove()
    }

    fun handleIntent(intent: Intent) {
        if (intent.action == "HIDE_AMBIENT_GLANCE_TEMPORARILY") {
            if (overlayView != null && overlayView?.alpha != 0f) {
                overlayView?.animate()?.alpha(0f)?.setDuration(500)?.start()
                handler.removeCallbacks(temporaryHideRunnable)
                handler.postDelayed(temporaryHideRunnable, 7000)
            }
            return
        }

        if (intent.action == "SHOW_AMBIENT_GLANCE") {
            // Skip if Android Auto is running
            if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(service)) {
                return
            }

            val isPlaying = intent.getBooleanExtra("is_playing", true)
            if (!isPlaying) {
                if (overlayView != null) fadeOutAndRemove()
                return
            }

            if (overlayView == null) {
                val mediaSessionManager =
                    service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val componentName =
                    android.content.ComponentName(service, NotificationListener::class.java)
                val sessions = try {
                    mediaSessionManager.getActiveSessions(componentName)
                } catch (e: Exception) {
                    emptyList()
                }
                val anyPlaying =
                    sessions.any { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
                if (!anyPlaying) {
                    return
                }
            }
            eventType = intent.getStringExtra("event_type")
            val newTitle = intent.getStringExtra("track_title")
            val newArtist = intent.getStringExtra("artist_name")

            val metadataChanged = (newTitle != trackTitle)

            trackTitle = newTitle
            artistName = newArtist

            if (intent.hasExtra("is_already_liked")) {
                isAlreadyLiked = intent.getBooleanExtra("is_already_liked", false)
            }
            val unreadPackages = intent.getStringArrayListExtra("unread_packages") ?: ArrayList()
            isDockedMode = intent.getBooleanExtra("is_docked_mode", false)
            volumePercentage = intent.getIntExtra("volume_percentage", 0)
            volumeKey = intent.getIntExtra("volume_key_code", -1)
            val newArtHash = if (intent.hasExtra("art_hash")) intent.getLongExtra("art_hash", -1L) else -1L
            if (newArtHash != -1L) artHash = newArtHash

            if (overlayView != null) {
                // If song changed while visible, refresh entire overlay or just content
                if (eventType == EVENT_TRACK_CHANGE || metadataChanged || eventType == "notification_update") {
                    // Reset to Music Mode
                    volumeIconView?.animate()?.alpha(0f)?.setDuration(200)?.start()
                    volumeStrokeView?.setColor(Color.GRAY)
                    handler.removeCallbacks(revertToMusicRunnable)

                    updateMetadata()
                    updateNotificationIcons(unreadPackages)

                    // Restart progress
                    handler.removeCallbacks(progressUpdateRunnable)
                    handler.post(progressUpdateRunnable)
                    return
                }

                handler.removeCallbacks(hideRunnable)

                // Volume key icon update helper
                if (eventType == EVENT_VOLUME) {
                    val isFill = getAlbumArtMode() == "fill"
                    if (isFill) {
                        bottomVolumeProgressView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                    } else {
                        if (volumeKey == 24) volumeIconView?.setImageResource(R.drawable.rounded_volume_up_24)
                        else if (volumeKey == 25) volumeIconView?.setImageResource(R.drawable.rounded_volume_down_24)
                        volumeIconView?.animate()?.alpha(1f)?.setDuration(200)?.start()
                        volumeStrokeView?.setColor(Color.WHITE)
                        volumeStrokeView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                    }
                    handler.removeCallbacks(volumeHideRunnable)
                    handler.postDelayed(volumeHideRunnable, 3000)
                }

                // Update like status
                likeStatusView?.setImageResource(if (isAlreadyLiked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24)

                if (eventType == EVENT_LIKE) {
                    likeStatusView?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(150)
                        ?.withEndAction {
                            likeStatusView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)
                                ?.start()
                        }?.start()
                } else if (eventType == "like_update") {
                    updateMetadata()
                }

                // If volume changed, pause progress update
                if (eventType == EVENT_VOLUME) {
                    handler.removeCallbacks(progressUpdateRunnable)
                    handler.removeCallbacks(revertToMusicRunnable)

                    val isFill = getAlbumArtMode() == "fill"
                    if (isFill) {
                        bottomVolumeProgressView?.updatePercentage(volumePercentage)
                    } else {
                        volumeStrokeView?.updatePercentage(volumePercentage)
                    }

                    if (isDockedMode) {
                        handler.postDelayed(revertToMusicRunnable, DISPLAY_DURATION)
                    }
                }

                if (!isDockedMode) {
                    handler.postDelayed(hideRunnable, DISPLAY_DURATION)
                }
                return
            }

            showOverlay()
            updateNotificationIcons(unreadPackages)
        }
    }

    private fun updateMetadata() {
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

        updateAlbumArt()

        // Handle Background Alpha for Fill mode
        backgroundImageView?.animate()?.alpha(if (isFill) 0.7f else 0f)?.setDuration(500)?.start()
        backgroundScrim?.animate()?.alpha(if (isFill) 1f else 0f)?.setDuration(500)?.start()
        centerContainer?.animate()?.alpha(if (isFill) 0f else 1f)?.setDuration(500)?.start()

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

    private fun applyClockFontVariations(clock: android.widget.TextClock, isFill: Boolean) {
        val prefs = service.getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        
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

    private fun updateNotificationIcons(packages: List<String>) {
        notificationIconsLayout?.let { layout ->
            layout.removeAllViews()
            packages.distinct().forEach { pkg ->
                try {
                    val icon = service.packageManager.getApplicationIcon(pkg)
                    val imageView = ImageView(service).apply {
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

    private fun updateAlbumArt(retryCount: Int = 0) {
        val title = trackTitle
        val artist = artistName
        if (title == null) return

        try {
            val hashToUse = if (artHash != -1L) artHash else kotlin.math.abs("${title}_${artist}".hashCode().toLong())
            
            // 1. Try Memory Cache first (Instant)
            val cachedBitmap = com.sameerasw.essentials.services.NotificationListener.getCachedBitmap(hashToUse)
            if (cachedBitmap != null) {
                applyBitmaps(cachedBitmap)
                return
            }

            // 2. Try Disk Cache
            val artFile = File(service.cacheDir, "art_$hashToUse.png")
            if (artFile.exists()) {
                Thread {
                    try {
                        val bitmap = BitmapFactory.decodeFile(artFile.absolutePath)
                        if (bitmap != null) {
                            handler.post { applyBitmaps(bitmap) }
                        }
                    } catch (_: Exception) {}
                }.start()
                return
            }

            // 3. Fallback or Retry
            if (retryCount == 0) {
                val tempFile = File(service.cacheDir, "temp_album_art.png")
                if (tempFile.exists()) {
                    Thread {
                        try {
                            val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                            if (bitmap != null) {
                                handler.post { applyBitmaps(bitmap) }
                            }
                        } catch (_: Exception) {}
                    }.start()
                }
            }

            if (retryCount < 12) { // Try for 6 seconds
                handler.postDelayed({ updateAlbumArt(retryCount + 1) }, 500)
            } else {
                val placeholder = android.graphics.drawable.ColorDrawable(getPrimaryColor(service))
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

    private fun applyBitmaps(bitmap: android.graphics.Bitmap) {
        nextImageView?.setImageBitmap(bitmap)
        backgroundNextImageView?.setImageBitmap(bitmap)
        if (morphAnimator?.isRunning != true) {
            imageView?.setImageBitmap(bitmap)
            backgroundImageView?.setImageBitmap(bitmap)
        }
    }

    private fun showOverlay() {
        // Skip if Android Auto is running
        if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(service)) {
            return
        }

        // Remove existing if any
        removeOverlay()

        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Load bitmap from cache dictionary (hashed by title+artist)
        val bitmap = try {
            val artHash = kotlin.math.abs("${trackTitle}_${artistName}".hashCode())
            val artFile = File(service.cacheDir, "art_$artHash.png")
            if (artFile.exists()) {
                BitmapFactory.decodeFile(artFile.absolutePath)
            } else {
                val tempFile = File(service.cacheDir, "temp_album_art.png")
                if (tempFile.exists()) BitmapFactory.decodeFile(tempFile.absolutePath) else null
            }
        } catch (e: Exception) {
            null
        }

        // Create View
        val context = service
        // 0. Typeface
        val googleSansFlex = ResourcesCompat.getFont(service, R.font.google_sans_flex)

        val rootLayout = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
        }

        val mode = getAlbumArtMode()

        // 0. Background for Fill mode
        backgroundImageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = if (mode == "fill") 0.7f else 0f
            if (bitmap != null) setImageBitmap(bitmap)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(30f, 30f, android.graphics.Shader.TileMode.CLAMP))
            }
        }
        backgroundNextImageView = ImageView(context).apply {
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
        backgroundScrim = View(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Radial gradient for vignette effect (dark edges, clear center)
            background = android.graphics.drawable.GradientDrawable().apply {
                gradientType = android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT
                colors = intArrayOf(0x00000000, 0xFF000000.toInt())
                gradientRadius = context.resources.displayMetrics.widthPixels.toFloat() * 1.1f
                setGradientCenter(0.5f, 0.5f)
            }
            alpha = if (mode == "fill") 1f else 0f
        }
        rootLayout.addView(backgroundImageView)
        rootLayout.addView(backgroundNextImageView)
        rootLayout.addView(backgroundScrim)

        // 1. Clock at top
        clockView = object : TextClock(context) {
            override fun onDetachedFromWindow() {
                try {
                    super.onDetachedFromWindow()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
        }.apply {
            val isFill = mode == "fill"
            format12Hour = if (isFill) "hh\nmm" else "hh:mm"
            format24Hour = if (isFill) "HH\nmm" else "HH:mm"
            setLineSpacing(0f, 0.8f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                if (isFill) Gravity.CENTER else Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                topMargin = if (isFill) -dpToPx(40f) else dpToPx(100f)
            }
            typeface = googleSansFlex
            applyClockFontVariations(this, isFill)
            alpha = 0.8f
        }
        rootLayout.addView(clockView)

        // 2. Center Content (Art + Volume Stroke)
        centerContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            alpha = if (mode == "fill") 0f else 1f
        }

        val size = dpToPx(320f)

        val randomEnabled = isRandomShapesEnabled()
        currentPolygon =
            com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getRandomPolygon(randomEnabled)
        currentShapePath =
            com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getRandomShapePath(
                size.toFloat(),
                randomEnabled
            )

        // Container for clipping
        clipContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        currentShapePath?.let { outline.setPath(it) }
                    } else {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            }
            clipToOutline = true
        }


        imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (bitmap != null) {
                setImageBitmap(bitmap)
            } else {
                // Material primary color placeholder
                setBackgroundColor(getPrimaryColor(context))
            }
        }

        nextImageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
        }

        // Dark overlay
        val scrim = View(context).apply {
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
        volumeIconView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            setColorFilter(Color.WHITE)
            alpha = if (eventType == EVENT_VOLUME) 1f else 0f

            if (eventType == EVENT_VOLUME) {
                // Initial icon set
                if (volumeKey == 25) { // DOWN
                    setImageResource(R.drawable.rounded_volume_down_24)
                } else {
                    setImageResource(R.drawable.rounded_volume_up_24)
                }
            }
        }
        centerContainer?.addView(volumeIconView)

        // Volume Stroke 
        val initialPerc = if (eventType == EVENT_VOLUME) volumePercentage else 0
        volumeStrokeView = VolumeStrokeView(context, currentShapePath!!, initialPerc)
        volumeStrokeView?.layoutParams =
            FrameLayout.LayoutParams(size + dpToPx(20f), size + dpToPx(20f), Gravity.CENTER)
        volumeStrokeView?.setColor(if (eventType == EVENT_VOLUME) Color.WHITE else Color.GRAY)
        volumeStrokeView?.alpha = if (eventType == EVENT_VOLUME) 1f else 0f
        centerContainer?.addView(volumeStrokeView)

        if (eventType == EVENT_VOLUME) {
            handler.removeCallbacks(volumeHideRunnable)
            handler.postDelayed(volumeHideRunnable, 3000)
        }

        // Start progress polling if not a volume notification
        if (eventType != EVENT_VOLUME) {
            handler.post(progressUpdateRunnable)
        }

        centerContainer?.let { rootLayout.addView(it) }

        // 3. Bottom Text Content
        textContainer = LinearLayout(context).apply {
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


        titleView = TextView(context).apply {
            text = trackTitle ?: "Unknown Track"
            textSize = 22f
            typeface = googleSansFlex
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(280f),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(24f), 0, dpToPx(24f), dpToPx(4f))
            }
            maxLines = 1
        }


        artistView = TextView(context).apply {
            text = artistName ?: "Unknown Artist"
            textSize = 15f
            typeface = googleSansFlex
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(240f),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(24f), 0, dpToPx(24f), 0)
            }
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        textContainer?.addView(titleView)
        textContainer?.addView(artistView)

        // Like Status Icon
        likeStatusView = ImageView(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply {
                    topMargin = dpToPx(16f)
                }
            setColorFilter(Color.WHITE)
            setImageResource(if (isAlreadyLiked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24)
            alpha = 0.8f
        }
        textContainer?.addView(likeStatusView)

        textContainer?.let { rootLayout.addView(it) }

        if (volumeReceiver == null) {
            volumeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                        val audioManager =
                            context?.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                        audioManager?.let {
                            val current =
                                it.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            val max = it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val perc = (current.toFloat() / max.toFloat() * 100).toInt()

                            val mode = getAlbumArtMode()
                            if (mode == "fill") {
                                bottomVolumeProgressView?.updatePercentage(perc)
                                bottomVolumeProgressView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                            } else {
                                volumeStrokeView?.updatePercentage(perc)
                                volumeStrokeView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                            }

                            handler.removeCallbacks(volumeHideRunnable)
                            handler.postDelayed(volumeHideRunnable, 3000)
                        }
                    }
                }
            }
            context.registerReceiver(
                volumeReceiver,
                IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            )
        }

        // 4. Notification Icons at bottom
        notificationIconsLayout = android.widget.LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dpToPx(60f)
            }
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            dividerDrawable = android.graphics.drawable.GradientDrawable().apply {
                setSize(dpToPx(12f), 0)
            }
            showDividers = android.widget.LinearLayout.SHOW_DIVIDER_MIDDLE
        }
        rootLayout.addView(notificationIconsLayout)

        val isFill = getAlbumArtMode() == "fill"
        bottomVolumeProgressView = BottomVolumeProgressView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(240f),
                dpToPx(20f),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                bottomMargin = dpToPx(30f)
            }
            updatePercentage(initialPerc)
            alpha = if (eventType == EVENT_VOLUME && isFill) 1f else 0f
        }
        rootLayout.addView(bottomVolumeProgressView)

        overlayView = rootLayout
        overlayView?.alpha = 0f

        // Layout Params - Full Screen
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY")
                    .getInt(null)
            } catch (_: Exception) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            }
        } else {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }

        @Suppress("DEPRECATION")
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            overlayView?.alpha = 0f
            windowManager?.addView(overlayView, params)

            // Animation: Fade In
            overlayView?.animate()
                ?.alpha(1f)
                ?.setDuration(500)
                ?.withEndAction {
                    if (!isDockedMode) {
                        handler.postDelayed(hideRunnable, DISPLAY_DURATION)
                    } else {
                        handler.post(burnInProtectionRunnable)
                    }
                }
                ?.start()

        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun checkAndShowOnScreenOff() {
        val prefs = service.getSharedPreferences(
            com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME,
            Context.MODE_PRIVATE
        )
        val isEnabled = prefs.getBoolean(
            com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED,
            false
        )
        val isDocked = prefs.getBoolean(
            com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE,
            false
        )

        if (isEnabled && isDocked) {
            // Skip if Android Auto is running
            if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(service)) {
                return
            }

            val intent = Intent("com.sameerasw.essentials.ACTION_REQUEST_AMBIENT_GLANCE").apply {
                setPackage(service.packageName)
            }
            service.sendBroadcast(intent)

            try {
                val mediaSessionManager =
                    service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
                val componentName = android.content.ComponentName(
                    service,
                    com.sameerasw.essentials.services.NotificationListener::class.java
                )
                val sessions = mediaSessionManager.getActiveSessions(componentName)
                val playingSession =
                    sessions.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }

                if (playingSession != null) {
                    val metadata = playingSession.metadata
                    val showIntent = Intent("SHOW_AMBIENT_GLANCE").apply {
                        putExtra("event_type", EVENT_PLAY_PAUSE)
                        putExtra(
                            "track_title",
                            metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                        )
                        putExtra(
                            "artist_name",
                            metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)
                        )
                        putExtra("is_docked_mode", true)
                    }
                    handleIntent(showIntent)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun dismissImmediately() {
        val view = overlayView ?: return
        handler.removeCallbacks(hideRunnable)
        view.animate()
            .alpha(0f)
            .setDuration(250) // 0.25s
            .withEndAction {
                removeOverlay()
            }
            .start()
    }

    private fun fadeOutAndRemove() {
        val view = overlayView ?: return

        view.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction {
                removeOverlay()
            }
            .start()
    }

    fun removeOverlay() {
        handler.removeCallbacks(hideRunnable)
        handler.removeCallbacks(progressUpdateRunnable)
        handler.removeCallbacks(revertToMusicRunnable)
        handler.removeCallbacks(burnInProtectionRunnable)

        // Clear active unread notifications from the overlay on dismissal
        com.sameerasw.essentials.services.NotificationListener.clearUnreadNotifications()

        if (overlayView != null && windowManager != null) {
            isDetached = true
            try {
                service.unregisterReceiver(volumeReceiver)
                volumeReceiver = null
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                // ignore
            }
            // Cancel all animators
            clockView?.animate()?.cancel()
            centerContainer?.animate()?.cancel()
            textContainer?.animate()?.cancel()
            volumeStrokeView?.cleanup()
            bottomVolumeProgressView?.cleanup()

            overlayView = null
            volumeStrokeView = null
            bottomVolumeProgressView = null
            volumeIconView = null
            likeStatusView = null
            notificationIconsLayout = null
            clockView = null
            centerContainer = null
            textContainer = null
            imageView = null
            backgroundImageView = null
            backgroundNextImageView = null
            backgroundScrim = null
            titleView = null
            artistView = null
        }
    }

    private fun dpToPx(dp: Float): Int {
        val metrics = service.resources.displayMetrics
        return (dp * metrics.density).toInt()
    }

    private fun getAlbumArtMode(): String {
        val prefs = service.getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val selectedMode = prefs.getString(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ALBUM_ART_MODE, "default") ?: "default"

        val forceFillWhileCharging = prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_FORCE_FILL_WHILE_CHARGING, false)
        if (forceFillWhileCharging) {
            val batteryStatus: Intent? = service.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val status: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
            if (isCharging) return "fill"
        }

        return selectedMode
    }

    private fun isRandomShapesEnabled(): Boolean {
        val prefs = service.getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_RANDOM_SHAPES, false)
    }

    private fun getPrimaryColor(context: Context): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            0xFF6650a4.toInt()
        }
    }

    private inner class VolumeStrokeView(
        context: Context,
        private var petalPath: Path,
        private val percentage: Int
    ) : View(context) {
        private var currentPercentage: Float = percentage.toFloat()
        private var animator: android.animation.ValueAnimator? = null

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
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
