/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core Utilities
 * File: CameraFlipButton.kt
 * Description: Helpers for finding and clicking camera flip buttons via accessibility.
 */

package com.sameerasw.essentials.utils

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

data class CameraSession(
    val lens: Int,
    val openedAt: Long,
    val closedAt: Long? = null,
)

fun lensInUseAt(
    sessions: List<CameraSession>,
    time: Long,
): Int? = sessions.lastOrNull { it.openedAt < time && (it.closedAt == null || it.closedAt >= time) }?.lens

fun openLens(sessions: List<CameraSession>): Int? = sessions.lastOrNull { it.closedAt == null }?.lens

object CameraFlipButton {
    private val ID_KEYWORDS =
        listOf(
            "camera_switch",
            "switch_camera",
            "camera_toggle",
            "toggle_camera",
            "flip_camera",
            "camera_flip",
            "front_back",
            "camera_facing",
        )

    private val DESCRIPTION_KEYWORDS =
        listOf(
            "switch camera",
            "flip camera",
            "switch to front",
            "switch to back",
            "switch to rear",
            "front camera",
            "back camera",
            "rear camera",
            "selfie camera",
        )

    fun isFlipButton(
        viewId: String?,
        description: CharSequence?,
    ): Boolean {
        if (!viewId.isNullOrBlank()) {
            val entryName = viewId.substringAfter(":id/").lowercase()
            if (ID_KEYWORDS.any { entryName.contains(it) }) {
                return true
            }
        }
        if (!description.isNullOrBlank()) {
            val desc = description.toString().lowercase()
            if (DESCRIPTION_KEYWORDS.any { desc.contains(it) }) {
                return true
            }
        }
        return false
    }

    fun click(service: AccessibilityService): Boolean {
        val appRoots =
            service.windows
                ?.filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
                ?.mapNotNull { it.root }
                .orEmpty()
        val roots =
            if (appRoots.isNotEmpty()) {
                appRoots
            } else {
                listOfNotNull(service.rootInActiveWindow)
            }
        if (roots.isEmpty()) return false

        for (root in roots) {
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(root)
            try {
                while (queue.isNotEmpty()) {
                    val node = queue.removeFirst()
                    if (node.isVisibleToUser && isFlipButton(node.viewIdResourceName, node.contentDescription)) {
                        var target: AccessibilityNodeInfo? = node
                        while (target != null && !target.isClickable) {
                            target = target.parent
                        }
                        return target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
                    }
                    for (i in 0 until node.childCount) {
                        node.getChild(i)?.let(queue::add)
                    }
                }
            } catch (_: IllegalStateException) {
            }
        }
        return false
    }
}
