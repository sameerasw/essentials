package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.utils.StatusBarLayoutUtil
import com.sameerasw.essentials.utils.StatusBarLayoutUtil.Section

class StatusBarSectionsDebugHandler(
    private val service: AccessibilityService,
) {
    companion object {
        private const val TAG = "StatusBarSections"
        private const val POLL_MS = 1000L
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: SectionsView? = null
    private var lastLogged: List<Section> = emptyList()
    private var lastNames: Set<String> = emptySet()

    private val pollRunnable =
        object : Runnable {
            override fun run() {
                refresh()
                handler.postDelayed(this, POLL_MS)
            }
        }

    private val enabled: Boolean
        get() = service.getSharedPreferences("essentials_prefs", Context.MODE_PRIVATE)
            .getBoolean(SettingsRepository.KEY_DEBUG_UI_MARGIN_OVERLAY, false)

    fun start() {
        if (!enabled) {
            destroy()
            return
        }
        StatusBarLayoutUtil.ensureFlags(service)
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
    }

    fun onWindowsChanged() {
        if (!enabled) return
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
    }

    fun destroy() {
        handler.removeCallbacks(pollRunnable)
        overlay?.let { runCatching { windowManager.removeView(it) } }
        overlay = null
    }

    private fun refresh() {
        val bar = StatusBarLayoutUtil.sections(service)
        val sections = bar +
            StatusBarLayoutUtil.volumePanelSections(service) +
            StatusBarLayoutUtil.systemUiComponentSections(service) +
            StatusBarLayoutUtil.navigationBarSections(service)
        if (bar != lastLogged) {
            lastLogged = bar
            bar.forEach { Log.d(TAG, "${it.name}: ${it.bounds} w=${it.bounds.width()} h=${it.bounds.height()}") }
            StatusBarLayoutUtil.dumpTree(service).forEach { Log.d("$TAG-tree", it) }
        }
        val shadeNames = sections.map { it.name }.toSet()
        if (shadeNames != lastNames) {
            lastNames = shadeNames
            StatusBarLayoutUtil.shadeTree(service).forEach { Log.d("$TAG-shade", it) }
        }
        ensureOverlay().setSections(sections)
    }

    private fun ensureOverlay(): SectionsView {
        overlay?.let { return it }
        val view = SectionsView(service)
        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                fitInsetsTypes = 0
            }
        windowManager.addView(view, params)
        overlay = view
        return view
    }

    private class SectionsView(context: Context) : View(context) {
        private val density = context.resources.displayMetrics.density
        private var sections: List<Section> = emptyList()
        private val loc = IntArray(2)
        private val rect = RectF()
        private val stroke =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.RED
                strokeWidth = 1.5f * density
            }
        private val label =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                textSize = 8f * density
            }

        fun setSections(newSections: List<Section>) {
            if (newSections == sections) return
            sections = newSections
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            getLocationOnScreen(loc)
            val radius = 8f * density
            sections.forEach { s ->
                rect.set(s.bounds)
                rect.offset(-loc[0].toFloat(), -loc[1].toFloat())
                canvas.drawRoundRect(rect, radius, radius, stroke)
                val text = "${s.name} ${s.bounds.width()}x${s.bounds.height()}"
                val y = if (rect.bottom + label.textSize > height) rect.top - 2 * density else rect.bottom + label.textSize
                canvas.drawText(text, rect.left + 2 * density, y, label)
            }
        }
    }
}
