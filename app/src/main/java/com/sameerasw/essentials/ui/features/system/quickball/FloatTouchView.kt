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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
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
        val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val scheme =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) darkColorScheme() else lightColorScheme()
            }

        val ballBg = scheme.surfaceContainerHighest.toArgb()
        val iconColor = scheme.primary.toArgb()
        val borderColor = scheme.outlineVariant.toArgb()

        background =
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ballBg)
                setStroke((1.5f * density).roundToInt(), borderColor)
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = RippleDrawable(ColorStateList.valueOf(Color.argb(50, 255, 255, 255)), null, background)
        }

        imageView.setColorFilter(iconColor)

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
                    HapticUtil.performUIHaptic(this)
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
                        HapticUtil.performGestureThresholdHaptic(this)
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
                    if (settings.isQuickBallHaptic()) {
                        HapticUtil.performUIHaptic(this)
                    }
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
