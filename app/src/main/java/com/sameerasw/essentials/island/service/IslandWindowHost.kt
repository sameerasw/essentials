package com.sameerasw.essentials.island.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
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
    private var drawRoot: FrameLayout? = null
    private var composeView: ComposeView? = null
    private var owner: OverlayLifecycleOwner? = null
    private var drawParams: WindowManager.LayoutParams? = null
    private var touchView: View? = null
    private var touchParams: WindowManager.LayoutParams? = null
    private var touchAdded = false
    private var geometry: CameraGeometry? = null
    private var targetBounds: IntRect? = null
    private var stage = IslandStage.Hidden

    var maxWidthPx: Int = 0

    private val density get() = context.resources.displayMetrics.density
    private val touchPad get() = (8 * density).roundToInt()

    fun attach(geometry: CameraGeometry, content: @Composable () -> Unit): Boolean {
        this.geometry = geometry
        if (drawRoot != null) {
            layoutDrawWindow()
            return true
        }
        val lifecycle = OverlayLifecycleOwner().also { it.onCreate() }
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent(content)
        }
        val frame = FrameLayout(context).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            addView(compose)
        }
        val lp = baseParams(touchable = false)
        try {
            drawRoot = frame
            composeView = compose
            owner = lifecycle
            drawParams = lp
            applyDrawSize(lp)
            wm.addView(frame, lp)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add island window", e)
            drawRoot = null
            composeView = null
            drawParams = null
            owner = null
            lifecycle.onDestroy()
            return false
        }
    }

    fun updateGeometry(geometry: CameraGeometry) {
        this.geometry = geometry
        layoutDrawWindow()
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
        removeTouchWindow()
        drawRoot?.let {
            try {
                wm.removeViewImmediate(it)
            } catch (_: Exception) {
            }
        }
        owner?.onDestroy()
        drawRoot = null
        composeView = null
        owner = null
        drawParams = null
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
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun applyDrawSize(lp: WindowManager.LayoutParams) {
        val geo = geometry ?: return
        val width = (maxWidthPx + 32 * density).roundToInt().coerceAtMost(geo.screenWidth)
        lp.width = width
        lp.height = (geo.screenHeight * 0.6f).roundToInt()
        lp.x = (geo.centerX - width / 2f).roundToInt()
        lp.y = 0
    }

    private fun layoutDrawWindow() {
        val lp = drawParams ?: return
        val view = drawRoot ?: return
        val before = Triple(lp.width, lp.height, lp.x)
        applyDrawSize(lp)
        if (before == Triple(lp.width, lp.height, lp.x)) return
        try {
            wm.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update island window", e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun layoutTouchWindow() {
        val draw = drawParams
        val bounds = targetBounds
        if (draw == null || bounds == null || stage == IslandStage.Hidden) {
            removeTouchWindow()
            return
        }
        val lp = touchParams ?: baseParams(touchable = true).also { touchParams = it }
        lp.x = draw.x + bounds.left - touchPad
        lp.y = draw.y + bounds.top - touchPad
        lp.width = bounds.width + touchPad * 2
        lp.height = bounds.height + touchPad * 2
        val view = touchView ?: View(context).also { v ->
            v.setOnTouchListener { _, event -> forward(event) }
            touchView = v
        }
        try {
            if (touchAdded) {
                wm.updateViewLayout(view, lp)
            } else {
                wm.addView(view, lp)
                touchAdded = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to place island touch window", e)
        }
    }

    private fun forward(event: MotionEvent): Boolean {
        val target = composeView ?: return false
        val draw = drawParams ?: return false
        val touch = touchParams ?: return false
        val copy = MotionEvent.obtain(event)
        copy.offsetLocation((touch.x - draw.x).toFloat(), (touch.y - draw.y).toFloat())
        target.dispatchTouchEvent(copy)
        copy.recycle()
        return true
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
