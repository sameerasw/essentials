package com.sameerasw.essentials.island.service

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

object SharedOverlayWindow {
    enum class Layer { Island, Duo }

    private class Entry(val view: View, var rect: Rect?)

    private var wm: WindowManager? = null
    private var context: Context? = null
    private var root: FrameLayout? = null
    private var owner: OverlayLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null
    private var focusable = false
    private var windowRect: Rect? = null
    private val entries = LinkedHashMap<Layer, Entry>()

    fun attach(context: Context, wm: WindowManager, layer: Layer, view: View, rect: Rect? = null): Boolean {
        this.context = context
        if (root == null) this.wm = wm
        detach(layer)
        if (!ensureWindow(context, wm)) return false
        val entry = Entry(view, rect)
        entries[layer] = entry
        val host = root ?: return false
        val index = if (layer == Layer.Island) 0 else host.childCount
        host.addView(view, index, childParams(entry))
        return true
    }

    fun detach(layer: Layer) {
        val entry = entries.remove(layer) ?: return
        val host = root
        if (host != null) {
            try {
                host.removeView(entry.view)
            } catch (_: Exception) {
            }
        }
        if (entries.isEmpty()) dropWindow()
    }

    fun isAttached(layer: Layer): Boolean = entries.containsKey(layer)

    fun setRect(layer: Layer, rect: Rect?) {
        val entry = entries[layer] ?: return
        entry.rect = rect
        applyChild(entry)
    }

    fun setWindowRect(rect: Rect?, focusable: Boolean, softInputMode: Int = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED) {
        windowRect = rect
        this.focusable = focusable
        val lp = params ?: return
        val view = root ?: return
        applyWindowParams(lp)
        lp.softInputMode = softInputMode
        try {
            wm?.updateViewLayout(view, lp)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update shared overlay window", e)
        }
        entries.values.forEach(::applyChild)
    }

    private fun screenSize(): Pair<Int, Int> {
        val manager = wm
        if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = manager.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        manager?.defaultDisplay?.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun childParams(entry: Entry): FrameLayout.LayoutParams {
        val shifted = windowRect
        val rect = entry.rect
        val lp = FrameLayout.LayoutParams(0, 0)
        lp.gravity = Gravity.TOP or Gravity.START
        if (rect == null && shifted == null) {
            lp.width = FrameLayout.LayoutParams.MATCH_PARENT
            lp.height = FrameLayout.LayoutParams.MATCH_PARENT
            return lp
        }
        val screen = if (rect == null) screenSize() else null
        val target = rect ?: Rect(0, 0, screen!!.first, screen.second)
        lp.width = target.width()
        lp.height = target.height()
        lp.leftMargin = target.left - (shifted?.left ?: 0)
        lp.topMargin = target.top - (shifted?.top ?: 0)
        return lp
    }

    private fun applyChild(entry: Entry) {
        entry.view.layoutParams = childParams(entry)
    }

    private fun ensureWindow(context: Context, wm: WindowManager): Boolean {
        if (root != null) return true
        val lifecycle = OverlayLifecycleOwner().also { it.onCreate() }
        val frame = FrameLayout(context).apply {
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            clipChildren = true
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            0,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        applyWindowParams(lp)
        return try {
            wm.addView(frame, lp)
            root = frame
            owner = lifecycle
            params = lp
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add shared overlay window", e)
            lifecycle.onDestroy()
            false
        }
    }

    private fun applyWindowParams(lp: WindowManager.LayoutParams) {
        val inert = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        var flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        if (!focusable) flags = flags or inert
        lp.flags = flags
        val shifted = windowRect
        if (shifted != null) {
            lp.width = shifted.width()
            lp.height = shifted.height()
            lp.x = shifted.left
            lp.y = shifted.top
        } else {
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
            lp.x = 0
            lp.y = 0
        }
    }

    private fun dropWindow() {
        val host = root
        if (host != null) {
            try {
                wm?.removeViewImmediate(host)
            } catch (_: Exception) {
            }
        }
        owner?.onDestroy()
        root = null
        owner = null
        params = null
        focusable = false
        windowRect = null
        entries.clear()
    }

    private const val TAG = "SharedOverlayWindow"
}
