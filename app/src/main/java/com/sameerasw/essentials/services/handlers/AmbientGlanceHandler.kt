package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.*
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.essentials.R
import com.sameerasw.essentials.services.tiles.ScreenOffAccessibilityService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AmbientGlanceHandler(
    private val service: AccessibilityService
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var volumeStrokeView: VolumeStrokeView? = null
    private var volumeIconView: ImageView? = null
    private var likeStatusView: ImageView? = null
    private var volumeReceiver: BroadcastReceiver? = null
    private val handler = Handler(Looper.getMainLooper())

    private var clockView: View? = null
    private var centerContainer: FrameLayout? = null
    private var textContainer: LinearLayout? = null

    private val burnInProtectionRunnable = object : Runnable {
        override fun run() {
            if (overlayView == null || !isDockedMode) return
            
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
            if (overlayView == null || eventType == EVENT_VOLUME) return
            
            val mediaSessionManager = service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = mediaSessionManager.getActiveSessions(android.content.ComponentName(service, ScreenOffAccessibilityService::class.java))
            
            // Find playing session
            val activeSession = sessions.firstOrNull { 
                it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING 
            }
            
            if (activeSession != null) {
                val position = activeSession.playbackState?.position ?: 0L
                val duration = activeSession.metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                
                if (duration > 0) {
                    val progress = (position.toFloat() / duration.toFloat() * 100).toInt()
                    volumeStrokeView?.updatePercentage(progress)
                }
            } else {
                // No active playing session
                if (isDockedMode) {
                    fadeOutAndRemove() // Hide if music stops/pauses in docked mode
                }
            }
            
            handler.postDelayed(this, 1000L)
        }
    }
    
    private val revertToMusicRunnable = Runnable {
        if (overlayView != null && isDockedMode) {
            eventType = EVENT_PLAY_PAUSE // Switch back to music view
            volumeIconView?.animate()?.alpha(0f)?.setDuration(200)?.start()
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
        if (intent.action == "SHOW_AMBIENT_GLANCE") {
            eventType = intent.getStringExtra("event_type")
            trackTitle = intent.getStringExtra("track_title")
            artistName = intent.getStringExtra("artist_name")
            isAlreadyLiked = intent.getBooleanExtra("is_already_liked", false)
            isDockedMode = intent.getBooleanExtra("is_docked_mode", false)
            volumePercentage = intent.getIntExtra("volume_percentage", 0)
            volumeKey = intent.getIntExtra("volume_key_code", -1)
            
            if (overlayView != null) {
                // If song changed while visible, refresh entire overlay or just content
                if (eventType == EVENT_TRACK_CHANGE || trackTitle != intent.getStringExtra("track_title")) {
                     trackTitle = intent.getStringExtra("track_title")
                     artistName = intent.getStringExtra("artist_name")
                     showOverlay()
                     return
                }

                handler.removeCallbacks(hideRunnable)
                
                // Volume key icon update helper
                if (eventType == EVENT_VOLUME) {
                    if (volumeKey == 24) volumeIconView?.setImageResource(R.drawable.rounded_volume_up_24)
                    else if (volumeKey == 25) volumeIconView?.setImageResource(R.drawable.rounded_volume_down_24)
                    volumeIconView?.animate()?.alpha(1f)?.setDuration(200)?.start()
                }

                // Update like status
                likeStatusView?.setImageResource(if (isAlreadyLiked) R.drawable.round_favorite_24 else R.drawable.rounded_favorite_24)
                
                if (eventType == EVENT_LIKE) {
                    likeStatusView?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(150)?.withEndAction {
                        likeStatusView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(150)?.start()
                    }?.start()
                }

                // If volume changed, pause progress update
                if (eventType == EVENT_VOLUME) {
                    handler.removeCallbacks(progressUpdateRunnable)
                    handler.removeCallbacks(revertToMusicRunnable)
                    volumeStrokeView?.updatePercentage(volumePercentage)
                    
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
        }
    }

    private fun showOverlay() {
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
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        // 1. Clock at top
        clockView = TextClock(context).apply {
            format12Hour = "hh:mm"
            format24Hour = "HH:mm"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dpToPx(100f)
            }
            typeface = googleSansFlex
            alpha = 0.8f
        }
        rootLayout.addView(clockView)
        
        // 2. Center Content (Art + Volume Stroke)
        centerContainer = FrameLayout(context).apply {
             layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }
        
        val size = dpToPx(320f)
        
        // Path for both clipping and stroke
        val petalPath = createScallopPath(size.toFloat(), size.toFloat(), 12, 0.10f)

        // Container for clipping
        val clipContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        outline.setPath(petalPath)
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        outline.setPath(petalPath)
                    } else {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            }
            clipToOutline = true
        }
        
        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            if (bitmap != null) {
                setImageBitmap(bitmap)
            } else {
                 // Material primary color placeholder
                 setBackgroundColor(getPrimaryColor(context))
            }
        }
        
        // Dark overlay
        val scrim = View(context).apply {
             layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
             setBackgroundColor(0x40000000.toInt()) 
        }
        
        clipContainer.addView(imageView)
        clipContainer.addView(scrim)
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
        volumeStrokeView = VolumeStrokeView(context, petalPath, initialPerc)
        volumeStrokeView?.layoutParams = FrameLayout.LayoutParams(size + dpToPx(20f), size + dpToPx(20f), Gravity.CENTER)
        centerContainer?.addView(volumeStrokeView)
        
        // Start progress polling if not a volume notification
        if (eventType != EVENT_VOLUME) {
            handler.post(progressUpdateRunnable)
        }
        
        centerContainer?.let { rootLayout.addView(it) }
        
        // 3. Bottom Text Content
        textContainer = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dpToPx(120f)
            }
        }
        
        val titleView = TextView(context).apply {
            text = trackTitle ?: "Unknown Track"
            textSize = 22f
            typeface = googleSansFlex
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(280f), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dpToPx(24f), 0, dpToPx(24f), dpToPx(4f))
            }
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        
        val artistView = TextView(context).apply {
            text = artistName ?: "Unknown Artist"
            textSize = 15f
            typeface = googleSansFlex
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
             layoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(240f), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dpToPx(24f), 0, dpToPx(24f), 0)
            }
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        
        textContainer?.addView(titleView)
        textContainer?.addView(artistView)

        // Like Status Icon
        likeStatusView = ImageView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(24f), dpToPx(24f)).apply {
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
                        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                        audioManager?.let {
                            val current = it.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                            val max = it.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                            val perc = (current.toFloat() / max.toFloat() * 100).toInt()
                            volumeStrokeView?.updatePercentage(perc)
                        }
                    }
                }
            }
            context.registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        }

        
        overlayView = rootLayout
        overlayView?.alpha = 0f

        // Layout Params - Full Screen
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                WindowManager.LayoutParams::class.java.getField("TYPE_ACCESSIBILITY_OVERLAY").getInt(null)
            } catch (_: Exception) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            }
        } else {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        }
        
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
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
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
    
    private fun createScallopPath(width: Float, height: Float, count: Int, depth: Float): android.graphics.Path {
        val path = android.graphics.Path()
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
        val prefs = service.getSharedPreferences(com.sameerasw.essentials.data.repository.SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_ENABLED, false)
        val isDocked = prefs.getBoolean(com.sameerasw.essentials.data.repository.SettingsRepository.KEY_AMBIENT_MUSIC_GLANCE_DOCKED_MODE, false)
        
        if (isEnabled && isDocked) {
             val mediaSessionManager = service.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
             val sessions = mediaSessionManager.getActiveSessions(android.content.ComponentName(service, ScreenOffAccessibilityService::class.java))
             val playingSession = sessions.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
             
             if (playingSession != null) {
                 val metadata = playingSession.metadata
                 val intent = Intent("SHOW_AMBIENT_GLANCE").apply {
                     putExtra("event_type", EVENT_PLAY_PAUSE)
                     putExtra("track_title", metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE))
                     putExtra("artist_name", metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST))
                     putExtra("is_docked_mode", true)
                 }
                 handleIntent(intent)
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
        if (overlayView != null && windowManager != null) {
            try {
                service.unregisterReceiver(volumeReceiver)
                volumeReceiver = null
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                // ignore
            }
            overlayView = null
            volumeStrokeView = null
            volumeIconView = null
            likeStatusView = null
            clockView = null
            centerContainer = null
            textContainer = null
        }
    }
    
    private fun dpToPx(dp: Float): Int {
        val metrics = service.resources.displayMetrics
        return (dp * metrics.density).toInt()
    }

    private fun getPrimaryColor(context: Context): Int {
        val typedValue = TypedValue()
        return if (context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            0xFF6650a4.toInt()
        }
    }

    private inner class VolumeStrokeView(context: Context, private val petalPath: Path, private val percentage: Int) : View(context) {
        private var currentPercentage: Float = percentage.toFloat()
        private var animator: android.animation.ValueAnimator? = null
        
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = dpToPx(6f).toFloat()
            strokeCap = Paint.Cap.ROUND
        }
        private val pathMeasure = PathMeasure(petalPath, false)
        private val progressPath = Path()

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

        override fun onDraw(canvas: Canvas) {
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
