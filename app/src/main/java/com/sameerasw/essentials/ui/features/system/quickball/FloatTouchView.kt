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
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
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
import kotlin.math.hypot
import kotlin.math.roundToInt

class FloatTouchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val settings = SettingsRepository(context)
    private val density = context.resources.displayMetrics.density

    private val imageView =
        ImageView(context).apply {
            setImageResource(R.drawable.ic_menu_open)
        }

    var isExpanded: Boolean = false
        private set

    var listener: GestureListener?
        get() = gestureDetector.listener
        set(value) {
            gestureDetector.listener = value
        }

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private var startX = 0f
    private var startY = 0f
    private var isDragging = false

    private val gestureDetector by lazy {
        QuickBallGestureDetector(
            context = context,
            isGestureEnabled = { settings.isQuickBallGesturesEnabled() && settings.isQuickBallLockPosition() },
        )
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
                bgDrawable.setStroke(borderWidthPx, ballBorderColor)
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.OUTLINE -> {
                bgDrawable.shape = GradientDrawable.OVAL
                val glassBg = (ballBg and 0x00FFFFFF) or (0x28 shl 24)
                bgDrawable.setColor(glassBg)
                bgDrawable.setStroke(kotlin.math.max(borderWidthPx, (2f * density).roundToInt()), ballBorderColor)
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.SQUIRCLE -> {
                bgDrawable.shape = GradientDrawable.RECTANGLE
                bgDrawable.cornerRadius = 14f * density
                bgDrawable.setColor(ballBg)
                bgDrawable.setStroke(borderWidthPx, ballBorderColor)
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.NEON -> {
                bgDrawable.shape = GradientDrawable.OVAL
                bgDrawable.setColor(ballBg)
                bgDrawable.setStroke(kotlin.math.max(borderWidthPx, (2.5f * density).roundToInt()), colors.accentColor)
                imageView.setColorFilter(ballIconColor)
            }
            QuickBallBallStyle.MINIMAL -> {
                bgDrawable.shape = GradientDrawable.OVAL
                bgDrawable.setColor(colors.accentColor)
                bgDrawable.setStroke(0, 0)
                imageView.setColorFilter(ballIconColor)
            }
        }

        background = bgDrawable

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = RippleDrawable(ColorStateList.valueOf(Color.argb(50, 255, 255, 255)), null, background)
        }

        val ballSizeDp = settings.getQuickBallSize()
        val iconSizePx = ((ballSizeDp - 18f).coerceAtLeast(20f) * density).roundToInt()
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
        if (isExpanded) {
            if (event.action == MotionEvent.ACTION_UP) {
                if (settings.isQuickBallHaptic()) {
                    IslandHaptics.openClick(context)
                }
                listener?.onSingleTap()
            }
            return true
        }

        return if (settings.isQuickBallLockPosition()) {
            gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
        } else {
            handleDragTouchEvent(event)
        }
    }

    private fun handleDragTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (settings.isQuickBallHaptic()) {
                    IslandHaptics.touchDown(context)
                }
                listener?.onTouchDown()
                startX = event.rawX
                startY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - startX
                val dy = event.rawY - startY

                if (!isDragging && hypot(dx, dy) > touchSlop) {
                    isDragging = true
                    if (settings.isQuickBallHaptic()) {
                        IslandHaptics.thresholdReached(context)
                    }
                }

                if (isDragging) {
                    listener?.onDragMove(dx, dy)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    listener?.onDragEnd()
                } else {
                    listener?.onSingleTap()
                }
                isDragging = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    listener?.onDragEnd()
                }
                listener?.onTouchCancel()
                isDragging = false
                return true
            }

            else -> return super.onTouchEvent(event)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gestureDetector.cleanup()
    }
}
