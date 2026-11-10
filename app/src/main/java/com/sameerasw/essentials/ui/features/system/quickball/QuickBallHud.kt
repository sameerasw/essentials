/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Action Feedback HUD
 * File: QuickBallHud.kt
 * Description: Floating semi-transparent capsule HUD displayed briefly on action execution.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.diy.Action
import kotlin.math.roundToInt

object QuickBallHud {

    private var hudView: LinearLayout? = null
    private var iconView: ImageView? = null
    private var textView: TextView? = null
    private var windowManager: WindowManager? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var dismissRunnable: Runnable? = null
    private var isDismissing = false

    fun show(
        context: Context,
        action: Action,
        customMessage: String? = null,
        @DrawableRes customIcon: Int? = null,
    ) {
        val settings = SettingsRepository(context)
        if (!settings.isQuickBallActionHudEnabled()) return

        val iconRes = customIcon ?: run {
            val iconPack = QuickBallIconPack.fromName(settings.getQuickBallIconPack())
            QuickBallIconResolver.resolveIcon(action, iconPack)
        }
        val label = customMessage ?: runCatching { context.getString(action.title) }.getOrDefault("")

        showInternal(context, iconRes, label)
    }

    fun showCustom(
        context: Context,
        @DrawableRes iconRes: Int,
        message: String,
    ) {
        val settings = SettingsRepository(context)
        if (!settings.isQuickBallActionHudEnabled()) return
        showInternal(context, iconRes, message)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showInternal(
        context: Context,
        @DrawableRes iconRes: Int,
        message: String,
    ) {
        android.util.Log.d("QuickBallHud", "showInternal called: msg=$message, icon=$iconRes")
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { showInternal(context, iconRes, message) }
            return
        }

        dismissRunnable?.let { handler.removeCallbacks(it) }
        val wm = windowManager ?: (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).also {
            windowManager = it
        }

        val settings = SettingsRepository(context)
        val density = context.resources.displayMetrics.density
        val colors = QuickBallTheme.resolveColors(context, settings)

        // Pill appearance
        val pillBg = (colors.surfaceColor and 0x00FFFFFF) or (0xEB shl 24) // ~92% opacity
        val contentColor = colors.contentColor

        val view = hudView ?: LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            elevation = 10f * density
            outlineProvider = ViewOutlineProvider.BACKGROUND

            val padH = (18f * density).roundToInt()
            val padV = (10f * density).roundToInt()
            setPadding(padH, padV, padH, padV)

            val iv = ImageView(context).apply {
                val size = (22f * density).roundToInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (10f * density).roundToInt()
                }
            }
            iconView = iv
            addView(iv)

            val tv = TextView(context).apply {
                textSize = 14f
                setTextColor(contentColor)
                gravity = Gravity.CENTER_VERTICAL
                includeFontPadding = false
            }
            textView = tv
            addView(tv)

            hudView = this
        }

        // Apply updated theme background
        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f * density
            setColor(pillBg)
            if (settings.isQuickBallBorderEnabled()) {
                val borderWidthPx = (settings.getQuickBallBorderWidth() * density).roundToInt()
                if (borderWidthPx > 0) {
                    setStroke(borderWidthPx, colors.borderColor)
                }
            }
        }
        view.background = bgDrawable

        // Update icon and text
        iconView?.apply {
            setImageResource(iconRes)
            setColorFilter(contentColor)
        }
        textView?.apply {
            text = message
            setTextColor(contentColor)
        }

        val params = layoutParams ?: WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (64f * density).roundToInt()
            layoutParams = this
        }

        if (view.parent == null) {
            try {
                wm.addView(view, params)
                android.util.Log.d("QuickBallHud", "Successfully added HUD view to WindowManager")
            } catch (e: WindowManager.BadTokenException) {
                android.util.Log.w("QuickBallHud", "BadTokenException on TYPE_ACCESSIBILITY_OVERLAY, trying TYPE_APPLICATION_OVERLAY", e)
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                try {
                    wm.addView(view, params)
                    android.util.Log.d("QuickBallHud", "Fallback added HUD view to WindowManager")
                } catch (e2: Exception) {
                    android.util.Log.e("QuickBallHud", "Failed to add HUD view fallback", e2)
                    return
                }
            } catch (e: Exception) {
                android.util.Log.e("QuickBallHud", "Failed to add HUD view", e)
                return
            }
            // Animate entry
            view.alpha = 0f
            view.translationY = -30f * density
            view.scaleX = 0.92f
            view.scaleY = 0.92f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(220L)
                .setInterpolator(OvershootInterpolator(0.85f))
                .start()
        } else {
            // Already showing, just pop slightly
            view.animate().cancel()
            view.alpha = 1f
            view.translationY = 0f
            view.scaleX = 1.05f
            view.scaleY = 1.05f
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160L)
                .start()
        }

        isDismissing = false
        val dismiss = Runnable {
            if (isDismissing) return@Runnable
            isDismissing = true
            view.animate()
                .alpha(0f)
                .translationY(-24f * density)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(180L)
                .setInterpolator(DecelerateInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            if (view.parent != null) {
                                wm.removeView(view)
                            }
                        } catch (_: Exception) {}
                        isDismissing = false
                    }
                })
                .start()
        }
        dismissRunnable = dismiss
        handler.postDelayed(dismiss, 1400L)
    }

    fun dismiss() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        hudView?.let { view ->
            try {
                if (view.parent != null) {
                    windowManager?.removeView(view)
                }
            } catch (_: Exception) {}
        }
        hudView = null
        iconView = null
        textView = null
    }
}
