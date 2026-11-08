package com.sameerasw.essentials.utils

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

object StatusBarLayoutUtil {
    private const val SYSTEMUI = "com.android.systemui"

    private val START_IDS = listOf("clock", "ongoing_activity_chip", "ongoing_call_chip")
    private val START_ICON_CONTAINERS = listOf("notificationIcons", "notification_icon_area_inner")
    private val END_IDS = listOf("battery")
    private val END_ICON_CONTAINERS = listOf("statusIcons")
    private const val END_FALLBACK = "system_icons"

    val DEBUG_IDS = listOf(
        "status_bar_start_side_content",
        "status_bar_start_side_except_heads_up",
        "status_bar_left_side",
        "clock",
        "notification_icon_area",
        "notificationIcons",
        "ongoing_activity_chip",
        "ongoing_call_chip",
        "status_bar_end_side_content",
        "system_icons",
        "statusIcons",
        "battery",
    )

    data class Section(val name: String, val bounds: Rect)

    data class Edges(val startContentEnd: Int?, val endContentStart: Int?)

    fun ensureFlags(service: AccessibilityService) {
        val info = service.serviceInfo ?: return
        val wanted = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
            AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
            AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        if (info.flags and wanted == wanted) return
        info.flags = info.flags or wanted
        service.serviceInfo = info
    }

    fun statusBarRoot(service: AccessibilityService): AccessibilityNodeInfo? {
        val windows = runCatching { service.windows }.getOrNull() ?: return null
        for (window in windows) {
            if (window.type != AccessibilityWindowInfo.TYPE_SYSTEM) continue
            val wb = Rect().also { window.getBoundsInScreen(it) }
            if (wb.top != 0 || wb.height() > wb.width() / 4) continue
            val root = window.root ?: continue
            if (root.packageName?.toString() == SYSTEMUI) return root
        }
        return null
    }

    fun sections(service: AccessibilityService, ids: List<String> = DEBUG_IDS): List<Section> {
        val root = statusBarRoot(service) ?: return emptyList()
        return ids.flatMap { id -> visibleBounds(root, id).map { Section(id, it) } }
    }

    fun edges(service: AccessibilityService): Edges? {
        val root = statusBarRoot(service) ?: return null
        val start = START_IDS.flatMap { visibleBounds(root, it) } +
            START_ICON_CONTAINERS.flatMap { childBounds(root, it) }
        var end = END_IDS.flatMap { visibleBounds(root, it) } +
            END_ICON_CONTAINERS.flatMap { childBounds(root, it) }
        if (end.isEmpty()) end = visibleBounds(root, END_FALLBACK)
        return Edges(start.maxOfOrNull { it.right }, end.minOfOrNull { it.left })
    }

    private fun nodes(root: AccessibilityNodeInfo, id: String): List<AccessibilityNodeInfo> =
        runCatching { root.findAccessibilityNodeInfosByViewId("$SYSTEMUI:id/$id") }.getOrNull().orEmpty()

    private fun visibleBounds(root: AccessibilityNodeInfo, id: String): List<Rect> =
        nodes(root, id).mapNotNull { it.visibleRect() }

    private fun childBounds(root: AccessibilityNodeInfo, id: String): List<Rect> =
        nodes(root, id).filter { it.isVisibleToUser }.flatMap { parent ->
            (0 until parent.childCount).mapNotNull { parent.getChild(it)?.visibleRect() }
        }

    private fun AccessibilityNodeInfo.visibleRect(): Rect? {
        if (!isVisibleToUser) return null
        val r = Rect().also { getBoundsInScreen(it) }
        return r.takeUnless { it.isEmpty }
    }
}
