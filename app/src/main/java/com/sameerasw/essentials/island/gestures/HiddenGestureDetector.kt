package com.sameerasw.essentials.island.gestures

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.sameerasw.essentials.island.ui.IslandHaptics
import kotlin.math.abs
import kotlin.math.hypot

class HiddenGestureDetector(
    private val context: Context,
    private val gestures: () -> CompactGestures,
    private val onTap: () -> Unit,
    private val onFeedback: (Boolean) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val density = context.resources.displayMetrics.density
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val commitThreshold = 56 * density
    private val slideStep = 18 * density
    private val brightnessRange = 240 * density

    private var config: CompactGestures = CompactGestures.None
    private var mode = SlideMode.None
    private var downX = 0f
    private var downY = 0f
    private var dx = 0f
    private var horizontal: Boolean? = null
    private var longFired = false
    private var lastStepX = 0f

    private val clearFeedback = Runnable {
        IslandSlideFeedback.publish(null)
        onFeedback(false)
    }

    private fun publish(armed: Boolean) {
        IslandSlideFeedback.publish(
            when (mode) {
                SlideMode.Volume -> SlideFeedback.Level(brightness = false, percent = config.levelPercent())
                SlideMode.Brightness -> SlideFeedback.Level(brightness = true, percent = config.levelPercent())
                SlideMode.SoundMode -> SlideFeedback.Sound(if (armed) config.soundModeAfter(dx) else config.soundMode())
                SlideMode.Track -> SlideFeedback.Track(next = config.trackForward(dx), armed = armed)
                SlideMode.None -> null
            },
        )
    }

    private val longPress = Runnable {
        longFired = true
        if (config.hasLongPress) {
            IslandHaptics.commit(context)
            config.longPress()
        }
    }

    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                dx = 0f
                lastStepX = 0f
                horizontal = null
                longFired = false
                config = gestures()
                mode = config.slideMode
                if (config.hasLongPress) handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                dx = event.rawX - downX
                val dy = event.rawY - downY
                if (horizontal == null && hypot(dx, dy) > slop) {
                    horizontal = abs(dx) > abs(dy)
                    handler.removeCallbacks(longPress)
                    if (horizontal == true) {
                        config.slideBegin()
                        if (mode != SlideMode.None) {
                            handler.removeCallbacks(clearFeedback)
                            onFeedback(true)
                            publish(armed = false)
                        }
                    }
                }
                if (horizontal == true && !longFired) {
                    when (mode) {
                        SlideMode.Brightness -> {
                            config.slideTo(dx, brightnessRange)
                            publish(armed = false)
                        }
                        SlideMode.Volume -> if (abs(dx - lastStepX) >= slideStep) {
                            config.slideStep(forward = dx > lastStepX)
                            lastStepX = dx
                            IslandHaptics.sliderStep(context)
                            publish(armed = false)
                        }
                        SlideMode.SoundMode, SlideMode.Track -> publish(armed = abs(dx) >= commitThreshold)
                        SlideMode.None -> {}
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                when {
                    longFired -> {}
                    horizontal == null -> {
                        IslandHaptics.tap(context)
                        onTap()
                    }
                    horizontal == true && abs(dx) >= commitThreshold &&
                        (mode == SlideMode.Track || mode == SlideMode.SoundMode) -> {
                        IslandHaptics.commit(context)
                        config.slideCommit(dx)
                        publish(armed = mode == SlideMode.Track)
                    }
                }
                if (horizontal == true && mode != SlideMode.None) handler.postDelayed(clearFeedback, FEEDBACK_LINGER_MS)
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPress)
                if (horizontal == true && mode != SlideMode.None) clearFeedback.run()
            }
        }
        return true
    }
}

private const val FEEDBACK_LINGER_MS = 700L
