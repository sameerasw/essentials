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

    private var currentShapePath: Path? = null

    // State
    private var isMusicMode = false
    private var trackTitle: String? = null
    private var artistName: String? = null
    private var isAlreadyLiked: Boolean = false
    private var volumePercentage: Int = 0
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
            format12Hour = "hh\nmm"
            format24Hour = "HH\nmm"
            textSize = 80f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isSingleLine = false
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
                                volumeStrokeView?.setColor(Color.WHITE)
                                volumeStrokeView?.updatePercentage(perc)

                                // Show and schedule hide
                                volumeStrokeView?.animate()?.alpha(1f)?.setDuration(300)?.start()
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

        // Hide music elements initially
        centerContainer?.alpha = 0f
        textContainer?.alpha = 0f

        // Clock is visible in Idle Mode (Screensaver default)
        clockView?.alpha = 1f
        clockView?.textSize = 60f // Large for idle
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
    }

    private fun handleIntent(intent: Intent) {
        if (isDetached) return

        // Skip if Android Auto is running
        if (com.sameerasw.essentials.utils.AppUtil.isAndroidAutoRunning(this)) {
            switchToClockMode()
            return
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

        if (trackTitle != null) {
            switchToMusicMode()
            if (metadataChanged || eventType == "track_change") {
                updateMetadata()
            }

            if (eventType == "volume") {
                volumeStrokeView?.setColor(Color.WHITE)
                volumeStrokeView?.updatePercentage(volumePercentage)

                // Show and schedule hide
                volumeStrokeView?.animate()?.alpha(1f)?.setDuration(300)?.start()
                handler.removeCallbacks(volumeHideRunnable)
                handler.postDelayed(volumeHideRunnable, 3000)

                handler.removeCallbacks(revertToMusicRunnable)
                handler.postDelayed(revertToMusicRunnable, 5000)
            }
        } else {
            switchToClockMode()
        }
    }

    private val revertToMusicRunnable = Runnable {
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
            }
        } else {
            currentController?.unregisterCallback(mediaCallback)
            currentController = null
            switchToClockMode()
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

        // Move Clock to Top
        clockView?.animate()?.alpha(0f)?.setDuration(150)?.withEndAction {
            clockView?.textSize = 25f
            clockView?.format12Hour = "hh:mm"
            clockView?.format24Hour = "HH:mm"
            clockView?.setLineSpacing(0f, 1f)
            clockView?.layoutParams = (clockView?.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = dpToPx(100f)
            }
            clockView?.requestLayout()
            clockView?.animate()?.alpha(0.8f)?.setDuration(150)?.start()
        }?.start()

        centerContainer?.animate()?.alpha(1f)?.setDuration(300)?.start()
        textContainer?.animate()?.alpha(1f)?.setDuration(300)?.start()

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
            clockView?.layoutParams = (clockView?.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.CENTER
                topMargin = 0
            }
            clockView?.requestLayout()
            clockView?.animate()?.alpha(1f)?.setDuration(150)?.start()
        }?.start()

        centerContainer?.animate()?.alpha(0f)?.setDuration(300)?.start()
        textContainer?.animate()?.alpha(0f)?.setDuration(300)?.start()
    }

    private fun updateMetadata(directBitmap: android.graphics.Bitmap? = null) {
        titleView?.text = trackTitle ?: "Unknown Track"
        artistView?.text = artistName ?: "Unknown Artist"
        likeStatusView?.setImageResource(if (isAlreadyLiked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24)

        // Update Dynamic Shape with Morphing
        val size = dpToPx(320f).toFloat()
        val newPolygon =
            com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getPolygon("${trackTitle}_${artistName}")

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
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        imageView?.setImageDrawable(nextImageView?.drawable)
                        nextImageView?.alpha = 0f
                    }
                })
                start()
            }
        } else {
            currentShapePath = com.sameerasw.essentials.utils.AmbientMusicShapeHelper.getShapePath(
                "${trackTitle}_${artistName}",
                size
            )
            volumeStrokeView?.updatePath(currentShapePath!!)
            clipContainer?.invalidateOutline()
        }
        currentPolygon = newPolygon

        updateAlbumArt(directBitmap)
    }

    private fun updateAlbumArt(directBitmap: android.graphics.Bitmap? = null, retryCount: Int = 0) {
        if (directBitmap != null) {
            nextImageView?.setImageBitmap(directBitmap)
            if (morphAnimator?.isRunning != true) {
                imageView?.setImageBitmap(directBitmap)
            }
            return
        }

        // Load Art from Cache (Fallback)
        try {
            val title = trackTitle
            val artist = artistName
            if (title == null) return

            val artHash = abs("${title}_${artist}".hashCode())
            val artFile = File(cacheDir, "art_$artHash.png")
            val bitmap = if (artFile.exists()) {
                BitmapFactory.decodeFile(artFile.absolutePath)
            } else {
                val tempFile = File(cacheDir, "temp_album_art.png")
                if (tempFile.exists()) BitmapFactory.decodeFile(tempFile.absolutePath) else null
            }
            if (bitmap != null) {
                imageView?.setImageBitmap(bitmap)
            } else {
                if (retryCount < 10) {
                    handler.postDelayed({ updateAlbumArt(null, retryCount + 1) }, 500)
                } else {
                    imageView?.setImageDrawable(ColorDrawable(Color.DKGRAY))
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
}
