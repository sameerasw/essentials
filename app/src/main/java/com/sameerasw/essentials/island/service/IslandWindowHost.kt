package com.sameerasw.essentials.island.service

import com.sameerasw.essentials.island.state.CameraAnchor
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sameerasw.essentials.utils.HapticUtil
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.state.CameraGeometry
import kotlin.math.roundToInt

class IslandWindowHost(
    private val context: Context,
    private val wm: WindowManager,
) {
    private var composeView: ComposeView? = null
    private var drawRect: Rect? = null
    private var touchView: View? = null
    private var touchParams: WindowManager.LayoutParams? = null
    private var touchAdded = false
    private var geometry: CameraGeometry? = null
    private var targetBounds: IntRect? = null
    private var stage = IslandStage.Hidden
    private var textInput = false
    private var interactionsDisabled = false
    var maxWidthPx: Int = 0

    var hiddenTouch: ((MotionEvent) -> Boolean)? = null
    private var alwaysGestures = false
    private var gestureView: View? = null
    private var gestureParams: WindowManager.LayoutParams? = null
    private var gestureAdded = false
    private var gestureTouching = false

    // Fired for touches anywhere outside the island, via FLAG_WATCH_OUTSIDE_TOUCH.
    var onOutsideTouch: (() -> Unit)? = null

    private val density get() = context.resources.displayMetrics.density
    private val touchPad get() = (8 * density).roundToInt()

    fun attach(geometry: CameraGeometry, content: @Composable () -> Unit): Boolean {
        this.geometry = geometry
        if (composeView != null) {
            layoutDrawWindow()
            return true
        }
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                requestedFrameRate = View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
            }
            setContent {
                val gated = HapticUtil.gate(LocalContext.current, LocalHapticFeedback.current)
                CompositionLocalProvider(LocalHapticFeedback provides gated) { content() }
            }
        }
        val rect = currentRect() ?: return false
        if (!SharedOverlayWindow.attach(context, wm, SharedOverlayWindow.Layer.Island, compose, rect)) {
            Log.e(TAG, "Failed to add island to the shared overlay")
            return false
        }
        composeView = compose
        drawRect = rect
        return true
    }

    fun updateGeometry(geometry: CameraGeometry) {
        this.geometry = geometry
        layoutDrawWindow()
        layoutTouchWindow()
    }

    fun setTextInput(active: Boolean) {
        if (textInput == active) return
        textInput = active
        if (composeView == null) return
        val rect = currentRect() ?: return
        drawRect = rect
        SharedOverlayWindow.setRect(SharedOverlayWindow.Layer.Island, rect)
        if (active) {
            SharedOverlayWindow.setWindowRect(
                rect,
                focusable = true,
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED,
            )
        } else {
            SharedOverlayWindow.setWindowRect(null, focusable = false)
        }
        layoutTouchWindow()
    }

    fun setInteractionsDisabled(disabled: Boolean) {
        if (interactionsDisabled == disabled) return
        interactionsDisabled = disabled
        layoutTouchWindow()
    }

    fun setAlwaysGestures(enabled: Boolean) {
        if (alwaysGestures == enabled) return
        alwaysGestures = enabled
        layoutTouchWindow()
    }

    fun onStageChanged(stage: IslandStage) {
        this.stage = stage
        layoutTouchWindow()
    }

    fun onTargetBoundsChanged(bounds: IntRect) {
        targetBounds = bounds
        layoutTouchWindow()
    }

    fun detach() {
        if (textInput) SharedOverlayWindow.setWindowRect(null, focusable = false)
        textInput = false
        removeTouchWindow()
        removeGestureWindow()
        alwaysGestures = false
        SharedOverlayWindow.detach(SharedOverlayWindow.Layer.Island)
        composeView = null
        drawRect = null
        targetBounds = null
    }

    private fun baseParams(touchable: Boolean) = WindowManager.LayoutParams(
        1,
        1,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            (if (touchable) WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun currentRect(): Rect? {
        val geo = geometry ?: return null
        if (textInput) {
            val reach = maxWidthPx + 32 * density
            val width = if (geo.anchor == CameraAnchor.Center) reach.roundToInt().coerceAtMost(geo.screenWidth) else (reach * 2).roundToInt()
            val height = if (geo.screenWidth > geo.screenHeight) geo.screenHeight else (geo.screenHeight * 0.6f).roundToInt()
            val left = (geo.centerX - width / 2f).roundToInt()
            return Rect(left, 0, left + width, height)
        }
        val viewport = islandDrawViewport(geo)
        return Rect(viewport.left, viewport.top, viewport.right, viewport.bottom)
    }

    private fun layoutDrawWindow() {
        if (composeView == null) return
        val rect = currentRect() ?: return
        if (rect == drawRect) return
        drawRect = rect
        SharedOverlayWindow.setRect(SharedOverlayWindow.Layer.Island, rect)
        if (textInput) SharedOverlayWindow.setWindowRect(rect, focusable = true, softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun layoutGestureWindow() {
        val geo = geometry
        if (geo == null || drawRect == null || !alwaysGestures || (stage != IslandStage.Hidden && !gestureTouching) || textInput || interactionsDisabled) {
            removeGestureWindow()
            return
        }
        val lp = gestureParams ?: baseParams(touchable = true).also { gestureParams = it }
        val width = maxOf(geo.cameraSlotWidth + 48 * density, 96 * density).roundToInt()
        lp.width = width
        lp.height = (geo.compactHeight + 8 * density).roundToInt()
        lp.x = (geo.centerX - width / 2f).roundToInt()
        lp.y = geo.surfaceTop.roundToInt()
        val view = gestureView ?: View(context).also { v ->
            v.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> gestureTouching = true
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        gestureTouching = false
                        v.post { layoutGestureWindow() }
                    }
                }
                hiddenTouch?.invoke(event) ?: false
            }
            gestureView = v
        }
        gestureAdded = placeWindow(view, lp, gestureAdded, "gesture")
    }

    private fun removeGestureWindow() {
        if (!gestureAdded) return
        try {
            gestureView?.let { wm.removeViewImmediate(it) }
        } catch (_: Exception) {
        }
        gestureAdded = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun layoutTouchWindow() {
        layoutGestureWindow()
        val draw = drawRect
        val bounds = targetBounds
        if (draw == null || bounds == null || stage == IslandStage.Hidden || textInput || interactionsDisabled) {
            removeTouchWindow()
            return
        }
        val lp = touchParams ?: baseParams(touchable = true).also { touchParams = it }
        lp.x = draw.left + bounds.left - touchPad
        lp.y = draw.top + bounds.top - touchPad
        lp.width = bounds.width + touchPad * 2
        lp.height = bounds.height + touchPad * 2
        val view = touchView ?: View(context).also { v ->
            v.setOnTouchListener { _, event -> forward(event) }
            touchView = v
        }
        touchAdded = placeWindow(view, lp, touchAdded, "touch")
    }

    private fun forward(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_OUTSIDE) {
            onOutsideTouch?.invoke()
            return false
        }
        val target = composeView ?: return false
        val draw = drawRect ?: return false
        val touch = touchParams ?: return false
        val copy = MotionEvent.obtain(event)
        copy.offsetLocation((touch.x - draw.left).toFloat(), (touch.y - draw.top).toFloat())
        target.dispatchTouchEvent(copy)
        copy.recycle()
        return true
    }

    private fun placeWindow(
        view: View,
        lp: WindowManager.LayoutParams,
        added: Boolean,
        name: String,
    ): Boolean {
        try {
            if (added) wm.updateViewLayout(view, lp) else wm.addView(view, lp)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to place island $name window, re-adding", e)
        }
        try {
            wm.removeViewImmediate(view)
        } catch (_: Exception) {
        }
        return try {
            wm.addView(view, lp)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-add island $name window", e)
            false
        }
    }

    private fun removeTouchWindow() {
        if (!touchAdded) return
        try {
            touchView?.let { wm.removeViewImmediate(it) }
        } catch (_: Exception) {
        }
        touchAdded = false
    }

    private companion object {
        const val TAG = "IslandWindowHost"
    }
}
