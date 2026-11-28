/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: WallpaperDockHandler.kt
 * Description: Tracks the launcher dock icon bounds for the live wallpaper dock container.
 */

package com.sameerasw.essentials.services.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.sameerasw.essentials.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WallpaperDockBridge {
    private val _bounds = MutableStateFlow<Rect?>(null)
    val bounds: StateFlow<Rect?> = _bounds

    fun publish(rect: Rect?) {
        if (_bounds.value != rect) _bounds.value = rect
    }
}

class WallpaperDockHandler(
    private val service: AccessibilityService,
) {
    private val repository = SettingsRepository(service)
    private val thread = HandlerThread("essentials-dock").apply { start() }
    private val handler = Handler(thread.looper)
    private val scanRunnable = Runnable { scan() }

    @Volatile private var homePackage: String? = null

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == SettingsRepository.KEY_LIVE_WALLPAPER_DOCK || key == SettingsRepository.KEY_LIVE_WALLPAPER_SELECTED_VIDEO) {
                updateEventSubscription()
                if (isActive()) requestScan(0) else WallpaperDockBridge.publish(null)
            }
        }

    fun start() {
        repository.registerLiveWallpaperPrefsListener(prefsListener)
        updateEventSubscription()
        if (isActive()) requestScan(0)
    }

    fun destroy() {
        repository.unregisterLiveWallpaperPrefsListener(prefsListener)
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
        WallpaperDockBridge.publish(null)
    }

    private fun isActive(): Boolean = repository.isLiveWallpaperDockEnabled() && repository.isLiveWallpaperDailyImageSelected()

    // content change events are only subscribed while the dock is on, and the service hands them straight here
    fun updateEventSubscription() {
        val info = service.serviceInfo ?: return
        val target =
            if (isActive()) {
                info.eventTypes or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            } else {
                info.eventTypes and AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED.inv()
            }
        if (info.eventTypes != target) {
            info.eventTypes = target
            service.serviceInfo = info
        }
    }

    fun onWindowsChanged() {
        if (isActive()) requestScan(WINDOW_SCAN_DELAY)
    }

    fun onContentChanged(event: AccessibilityEvent) {
        if (!isActive()) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != homePackage) return
        requestScan(CONTENT_SCAN_DELAY)
    }

    private fun requestScan(delayMs: Long) {
        handler.removeCallbacks(scanRunnable)
        handler.postDelayed(scanRunnable, delayMs)
    }

    private fun resolveHomePackage(): String? =
        try {
            service.packageManager
                .resolveActivity(
                    Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                    PackageManager.MATCH_DEFAULT_ONLY,
                )?.activityInfo
                ?.packageName
        } catch (_: Exception) {
            null
        }

    private fun scan() {
        if (!isActive()) {
            WallpaperDockBridge.publish(null)
            return
        }
        val home = resolveHomePackage() ?: return
        homePackage = home

        // launcher or dock not on screen (app open, drawer, recents): keep the last bounds as is
        val root =
            try {
                service.windows
                    ?.firstOrNull { it.type == AccessibilityWindowInfo.TYPE_APPLICATION && it.root?.packageName == home }
                    ?.root
            } catch (_: Exception) {
                null
            } ?: return

        val hotseat = findHotseat(root, home) ?: return
        if (!hotseat.isVisibleToUser) return
        WallpaperDockBridge.publish(iconBounds(hotseat))
    }

    private fun findHotseat(
        root: AccessibilityNodeInfo,
        home: String,
    ): AccessibilityNodeInfo? {
        root.findAccessibilityNodeInfosByViewId("$home:id/hotseat")?.firstOrNull()?.let { return it }

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            if (node.viewIdResourceName?.endsWith(":id/hotseat") == true) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    private fun iconBounds(hotseat: AccessibilityNodeInfo): Rect? {
        val union = Rect()
        val bounds = Rect()

        fun addIcon(node: AccessibilityNodeInfo) {
            if (!node.isVisibleToUser) return
            node.getBoundsInScreen(bounds)
            if (bounds.isEmpty) return
            if (union.isEmpty) union.set(bounds) else union.union(bounds)
        }

        for (i in 0 until hotseat.childCount) {
            val child = hotseat.getChild(i) ?: continue
            val id = child.viewIdResourceName.orEmpty()
            if (id.contains("search", ignoreCase = true) || id.contains("qsb", ignoreCase = true)) continue
            if (child.childCount > 0 && !child.isClickable) {
                for (j in 0 until child.childCount) child.getChild(j)?.let { addIcon(it) }
            } else {
                addIcon(child)
            }
        }
        return if (union.isEmpty) null else union
    }

    companion object {
        private const val WINDOW_SCAN_DELAY = 250L
        private const val CONTENT_SCAN_DELAY = 300L
        private const val MAX_NODES = 400
    }
}
