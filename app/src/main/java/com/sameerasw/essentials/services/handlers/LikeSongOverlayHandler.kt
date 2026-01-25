package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.cardview.widget.CardView
import com.sameerasw.essentials.R
import java.io.File

class LikeSongOverlayHandler(
    private val service: AccessibilityService
) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private val DISPLAY_DURATION = 3000L
    private var isAlreadyLiked = false

    fun handleIntent(intent: Intent) {
        if (intent.action == "SHOW_LIKE_OVERLAY") {
            isAlreadyLiked = intent.getBooleanExtra("is_already_liked", false)
            showOverlay()
        }
    }

    private fun showOverlay() {
        // Remove existing if any (debounce/restart)
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

        val context = service
        val rootLayout = FrameLayout(context)
        
        val size = dpToPx(220f)
        
        val clipContainer = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(size, size, Gravity.CENTER)
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
        
        val iconSize = dpToPx(56f)
        val iconView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            setImageResource(R.drawable.rounded_favorite_24)
            setColorFilter(android.graphics.Color.WHITE)
            
            if (isAlreadyLiked) {
                alpha = 0.7f
            }
        }
        
        clipContainer.addView(imageView)
        clipContainer.addView(scrim)
        
        rootLayout.addView(clipContainer)
        rootLayout.addView(iconView)
        
        overlayView = rootLayout
        overlayView?.alpha = 0f

        // Layout Params
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
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }

        try {
            windowManager?.addView(overlayView, params)
            
            overlayView?.animate()
                ?.alpha(1f)
                ?.setDuration(500)
                ?.withEndAction {
                    handler.postDelayed({
                        fadeOutAndRemove()
                    }, DISPLAY_DURATION)
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
        val innerR = radius * (1f - depth)
        
        
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
