/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities
 * File: LockscreenShortcutDetector.kt
 * Description: Reads the System UI lock screen through accessibility to find the system's own shortcut buttons.
 */

package com.sameerasw.essentials.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo

// Shortcut selections are behind a signature permission, so look for the buttons in System UI's tree
object LockscreenShortcutDetector {
    private const val SYSTEM_UI = "com.android.systemui"
    private const val MAX_NODES = 600

    private val LOCKSCREEN_MARKERS =
        listOf("element:lockscreen", "keyguard_root_view", "keyguard_bottom_area", "keyguard_indication_area")
    private val OCCLUDING_MARKERS =
        listOf("bouncer", "element:shade", "element:quickSettings", "qs_frame", "shade_header_root", "quick_qs_panel")
    private val AFFORDANCE_IDS = listOf("start_button", "end_button")
    private val EXCLUDED_IDS = listOf("device_entry_icon", "lock_icon", "keyguard_indication", "date", "clock")

    data class Result(
        val isLockscreenVisible: Boolean,
        val hasSystemShortcuts: Boolean,
    )

    fun scan(
        service: AccessibilityService,
        screenWidth: Int,
        screenHeight: Int,
    ): Result {
        var lockscreenVisible = false
        var occluded = false
        var hasShortcuts = false

        val roots =
            try {
                service.windows.mapNotNull { it.root }.filter { it.packageName?.toString() == SYSTEM_UI }
            } catch (_: Exception) {
                emptyList()
            }
        // No content-change events reach the service, so System UI's cached nodes go stale between scans
        val canClearSubtree = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        if (canClearSubtree) {
            roots.forEach {
                service.clearCachedSubtree(it)
                it.refresh()
            }
        }

        val bounds = Rect()
        for (root in roots) {
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            var visited = 0
            while (queue.isNotEmpty() && visited < MAX_NODES) {
                val node = queue.removeFirst()
                visited++
                if (!canClearSubtree) node.refresh()
                if (!node.isVisibleToUser) continue

                val id = node.viewIdResourceName?.substringAfter(":id/").orEmpty()
                if (LOCKSCREEN_MARKERS.any { id == it }) lockscreenVisible = true
                if (OCCLUDING_MARKERS.any { id.contains(it) }) occluded = true

                node.getBoundsInScreen(bounds)
                val isCandidate =
                    isShortcutCandidate(
                        id, node.isClickable, bounds.left, bounds.top, bounds.right, bounds.bottom,
                        screenWidth, screenHeight,
                    )
                if (isCandidate) {
                    hasShortcuts = true
                }

                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { queue.add(it) }
                }
            }
        }

        return Result(
            isLockscreenVisible = lockscreenVisible && !occluded,
            hasSystemShortcuts = hasShortcuts,
        )
    }

    fun isShortcutCandidate(
        id: String,
        isClickable: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        screenWidth: Int,
        screenHeight: Int,
    ): Boolean {
        if (AFFORDANCE_IDS.any { id == it }) return true
        if (!isClickable || screenWidth <= 0 || screenHeight <= 0) return false
        if (EXCLUDED_IDS.any { id.contains(it) }) return false
        val width = right - left
        if (width <= 0 || bottom - top <= 0) return false
        if (width > screenWidth / 3) return false

        val centerX = (left + right) / 2f
        val centerY = (top + bottom) / 2f
        val inBottomBand = centerY > screenHeight * 0.85f
        val inCorner = centerX < screenWidth * 0.25f || centerX > screenWidth * 0.75f
        return inBottomBand && inCorner
    }
}
