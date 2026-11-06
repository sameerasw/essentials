package com.sameerasw.essentials.utils

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

object VolumePanelDetector {
    private const val SYSTEM_UI = "com.android.systemui"
    private const val MAX_NODES = 24
    private const val CACHE_MS = 400L
    private val VOLUME_IDS = listOf("$SYSTEM_UI:id/volume_dialog", "$SYSTEM_UI:id/volume_dialog_container")

    private val cache = HashMap<Int, Pair<Long, Boolean>>()

    fun isVolumePanel(window: AccessibilityWindowInfo): Boolean {
        if (window.type != AccessibilityWindowInfo.TYPE_SYSTEM) return false
        if (window.title?.contains("Volume", ignoreCase = true) == true) return true
        val now = SystemClock.uptimeMillis()
        synchronized(cache) {
            cache[window.id]?.takeIf { now - it.first < CACHE_MS }?.let { return it.second }
        }
        val result = inspect(window)
        synchronized(cache) {
            if (cache.size > 16) cache.clear()
            cache[window.id] = now to result
        }
        return result
    }

    private fun inspect(window: AccessibilityWindowInfo): Boolean {
        val root = window.root ?: return false
        if (root.packageName?.toString() != SYSTEM_UI) return false
        if (VOLUME_IDS.any { root.findAccessibilityNodeInfosByViewId(it).isNotEmpty() }) return true
        return hasVolumeId(root)
    }

    private fun hasVolumeId(root: AccessibilityNodeInfo): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited++
            if (node.viewIdResourceName?.contains("volume", ignoreCase = true) == true) return true
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return false
    }
}
