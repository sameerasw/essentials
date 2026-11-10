/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Quick Ball Floating Window
 * File: FloatTouchView.kt
 * Description: Circular floating action ball handling dragging, edge physics, and touch dispatching.
 */

package com.sameerasw.essentials.ui.features.system.quickball

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.ImageView
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.utils.HapticUtil
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt

class FloatTouchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val settings = SettingsRepository(context)
    private val density = context.resources.displayMetrics.density
    private val handler = Handler(Looper.getMainLooper())

    private val imageView =
        ImageView(context).apply {
            setImageResource(R.drawable.ic_menu_open)
        }

    var isExpanded: Boolean = false
        private set

    var onRight: Boolean = true

    var listener: GestureListener? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    private var startX = 0f
    private var startY = 0f
    private var isDragging = false
    private var isFlicking = false
    private var isMoveMode = false

    private val holdToMoveRunnable =
        Runnable {
            if (!isExpanded && !isFlicking) {
                isMoveMode = true
                if (settings.isQuickBallHaptic()) {
                    IslandHaptics.thresholdReached(context)
                }
                animate().scaleX(1.15f).scaleY(1.15f).setDuration(180L).start()
            }
        }

    init {
        addView(imageView)
        updateAppearance()
    }

    fun updateAppearance() {
        val colors = QuickBallTheme.resolveColors(context, settings)
        val style = QuickBallBallStyle.fromName(settings.getQuickBallBallStyle())
        val isBorderEnabled = settings.isQuickBallBorderEnabled()
        val borderWidthPx = if (isBorderEnabled) (settings.getQuickBallBorderWidth() * density).roundToInt() else 0
        val ballBg = colors.ballColor
        val ballIconColor = colors.ballIconColor
        val ballBorderColor = colors.ballBorderColor

        val bgDrawable = GradientDrawable()
        when (style) {
            QuickBallBallStyle.CLASSIC -> {
                bgDrawable.shape = GradientDrawable.OVAL
                bgDrawable.setColor(ballBg)
                if (borderWidthPx > 0) {
                    bgDrawable.setStroke(borderWidthPx, ballBorderColor)
                }
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.OUTLINE -> {
                bgDrawable.shape = GradientDrawable.OVAL
                val glassBg = (ballBg and 0x00FFFFFF) or (0x28 shl 24)
                bgDrawable.setColor(glassBg)
                if (borderWidthPx > 0) {
                    bgDrawable.setStroke(borderWidthPx, ballBorderColor)
                }
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.SQUIRCLE -> {
                bgDrawable.shape = GradientDrawable.RECTANGLE
                bgDrawable.cornerRadius = 14f * density
                bgDrawable.setColor(ballBg)
                if (borderWidthPx > 0) {
                    bgDrawable.setStroke(borderWidthPx, ballBorderColor)
                }
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.NEON -> {
                bgDrawable.shape = GradientDrawable.OVAL
                bgDrawable.setColor(ballBg)
                if (borderWidthPx > 0) {
                    bgDrawable.setStroke(borderWidthPx, colors.accentColor)
                }
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.MINIMAL -> {
                bgDrawable.shape = GradientDrawable.OVAL
                bgDrawable.setColor(colors.accentColor)
                if (borderWidthPx > 0) {
                    bgDrawable.setStroke(borderWidthPx, ballBorderColor)
                }
                imageView.setColorFilter(ballIconColor)
            }
        }

        background = bgDrawable

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = RippleDrawable(ColorStateList.valueOf(Color.argb(50, 255, 255, 255)), null, background)
        }

        val ballSizeDp = settings.getQuickBallSize()
        val iconSizePx = ((ballSizeDp - 16f).coerceAtLeast(20f) * density).roundToInt()
        imageView.layoutParams = LayoutParams(iconSizePx, iconSizePx, Gravity.CENTER)
    }

    fun setExpanded(
        expanded: Boolean,
        animate: Boolean = true,
    ) {
        this.isExpanded = expanded
        val targetRotation = if (expanded) 180f else 0f
        val drawableRes = if (expanded) R.drawable.ic_menu_close else R.drawable.ic_menu_open

        if (animate) {
            if (expanded) {
                imageView.setImageResource(R.drawable.ic_menu_close)
            }
            imageView.animate()
                .rotation(targetRotation)
                .setDuration(260L)
                .withEndAction {
                    if (!expanded) {
                        imageView.setImageResource(R.drawable.ic_menu_open)
                    }
                }
                .start()
        } else {
            imageView.rotation = targetRotation
            imageView.setImageResource(drawableRes)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isExpanded && !isFlicking) {
            if (event.action == MotionEvent.ACTION_UP) {
                if (settings.isQuickBallHaptic()) {
                    IslandHaptics.openClick(context)
                }
                listener?.onSingleTap()
            }
            return true
        }

        return handleTouchGesture(event)
    }

    private fun handleTouchGesture(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (settings.isQuickBallHaptic()) {
                    IslandHaptics.touchDown(context)
                }
                listener?.onTouchDown()
                startX = event.rawX
                startY = event.rawY
                isDragging = false
                isFlicking = false
                isMoveMode = false

                handler.removeCallbacks(holdToMoveRunnable)
                if (!settings.isQuickBallLockPosition()) {
                    val delay = settings.getQuickBallHoldToMoveDuration().coerceIn(1000L, 5000L)
                    handler.postDelayed(holdToMoveRunnable, delay)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - startX
                val dy = event.rawY - startY
                val dist = hypot(dx, dy)

                if (dist > touchSlop) {
                    if (isMoveMode) {
                        // User held down for preferred duration (1-3s) to unlock Move Mode:
                        // Freely drag across the screen to reposition
                        if (!isDragging) {
                            isDragging = true
                        }
                        listener?.onDragMove(dx, dy)
                        return true
                    }

                    val isInward = if (onRight) {
                        dx < -touchSlop * 0.45f
                    } else {
                        dx > touchSlop * 0.45f
                    }

                    // Check if inward swipe towards radial arc (Flick-to-Select)
                    if (settings.isQuickBallFlickToSelect() && (isFlicking || isInward)) {
                        handler.removeCallbacks(holdToMoveRunnable)
                        if (!isFlicking) {
                            isFlicking = true
                            listener?.onFlickStart()
                        }
                        listener?.onFlickMove(event.rawX, event.rawY)
                        return true
                    }

                    // User moved finger before holding for preferred duration -> cancel hold timer
                    // Moving without holding is strictly blocked!
                    handler.removeCallbacks(holdToMoveRunnable)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(holdToMoveRunnable)

                if (isMoveMode) {
                    animate().scaleX(1f).scaleY(1f).setDuration(160L).start()
                    isMoveMode = false
                    if (isDragging) {
                        isDragging = false
                        listener?.onDragEnd()
                    }
                    return true
                }

                if (isFlicking) {
                    isFlicking = false
                    listener?.onFlickEnd()
                    return true
                }

                if (isDragging) {
                    isDragging = false
                    listener?.onDragEnd()
                    return true
                }

                // Plain single tap on the floating ball
                listener?.onSingleTap()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(holdToMoveRunnable)
                if (isMoveMode) {
                    animate().scaleX(1f).scaleY(1f).setDuration(160L).start()
                    isMoveMode = false
                }
                if (isFlicking) {
                    isFlicking = false
                    listener?.onFlickEnd()
                }
                if (isDragging) {
                    isDragging = false
                    listener?.onDragEnd()
                }
                listener?.onTouchCancel()
                return true
            }

            else -> return super.onTouchEvent(event)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updateGestureExclusion()
    }

    fun updateGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val w = width
            val h = height
            if (w > 0 && h > 0) {
                systemGestureExclusionRects = listOf(Rect(0, 0, w, h))
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }
}
