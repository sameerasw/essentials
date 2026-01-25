package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.sameerasw.essentials.R
import java.io.File

class AmbientGlanceHandler(
    private val service: AccessibilityService
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private val DISPLAY_DURATION = 5000L
    
    private var eventType: String? = null
    private var trackTitle: String? = null
    private var artistName: String? = null
    private var isAlreadyLiked: Boolean = false

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
            
            if (eventType == EVENT_VOLUME && overlayView != null) {
                handler.removeCallbacks(hideRunnable)
                handler.postDelayed(hideRunnable, DISPLAY_DURATION)
                return
            }
            
            showOverlay()
        }
    }

    private fun showOverlay() {
        // Remove existing if any
        removeOverlay()
        
        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Load bitmap from cache
        val bitmap = try {
            val file = File(service.cacheDir, "temp_album_art.png")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }

        // Create View
        val context = service
        val rootLayout = FrameLayout(context).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        
        // 1. Center Content (Art + Icon)
        val centerContainer = FrameLayout(context).apply {
             layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        }
        
        val size = dpToPx(400f)
        
        // Container for clipping
        val clipContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
            // Apply 12-petal clip
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val path = createScallopPath(view.width.toFloat(), view.height.toFloat(), 12, 0.10f)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        outline.setPath(path)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                             outline.setPath(path)
                        } else {
                             outline.setOval(0, 0, view.width, view.height)
                        }
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
                 setBackgroundColor(0xFF222222.toInt())
            }
        }
        
        // Dark overlay
        val scrim = View(context).apply {
             layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
             setBackgroundColor(0x60000000.toInt()) 
        }
        
        clipContainer.addView(imageView)
        clipContainer.addView(scrim)
        
        // Icon
        val iconSize = dpToPx(56f)
        val iconView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            setColorFilter(android.graphics.Color.WHITE)
            
            // Icon logic based on event type
            when (eventType) {
                EVENT_LIKE -> {
                    setImageResource(R.drawable.rounded_favorite_24)
                    if (isAlreadyLiked) alpha = 0.7f
                }
                EVENT_PLAY_PAUSE -> {
                    setImageResource(R.drawable.rounded_play_arrow_24) 
                }
                EVENT_VOLUME -> {
                    setImageResource(R.drawable.rounded_volume_up_24)
                }
                EVENT_TRACK_CHANGE -> {
                    setImageResource(R.drawable.rounded_music_note_24)
                }
                else -> {
                    setImageResource(R.drawable.rounded_music_note_24)
                }
            }
        }
        
        centerContainer.addView(clipContainer)
        centerContainer.addView(iconView)
        rootLayout.addView(centerContainer)
        
        // 2. Bottom Text Content
        val textContainer = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
                bottomMargin = dpToPx(100f)
            }
        }
        
        val titleView = TextView(context).apply {
            text = trackTitle ?: "Unknown Track"
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dpToPx(24f), 0, dpToPx(24f), dpToPx(4f))
            }
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        
        val artistView = TextView(context).apply {
            text = artistName ?: "Unknown Artist"
            textSize = 16f
            setTextColor(0xCCFFFFFF.toInt())
            gravity = Gravity.CENTER
             layoutParams = android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dpToPx(24f), 0, dpToPx(24f), 0)
            }
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        
        textContainer.addView(titleView)
        textContainer.addView(artistView)
        rootLayout.addView(textContainer)

        
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
                    handler.postDelayed(hideRunnable, DISPLAY_DURATION)
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
             val theta = Math.toRadians(i.toDouble())
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
        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }
    
    private fun dpToPx(dp: Float): Int {
        val metrics = service.resources.displayMetrics
        return (dp * metrics.density).toInt()
    }
}
