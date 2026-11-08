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
    private val START_FALLBACKS = listOf("status_bar_start_side_except_heads_up", "status_bar_start_side_content", "status_bar_left_side")
    private val END_FALLBACKS = listOf("system_icons", "status_bar_end_side_content")

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

    private val VOLUME_IDS = listOf(
        "volume_dialog_background",
        "volume_ringer_drawer",
        "ringer_buttons_background",
        "volume_dialog_main_slider_container",
        "volume_dialog_bottom_section_container",
        "volume_dialog_rows",
        "volume_row_slider",
    )

    fun volumePanelSections(service: AccessibilityService): List<Section> {
        val windows = runCatching { service.windows }.getOrNull() ?: return emptyList()
        for (window in windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() != SYSTEMUI) continue
            if (nodes(root, "volume_dialog").none { it.isVisibleToUser }) continue
            return VOLUME_IDS.flatMap { id -> visibleBounds(root, id).map { Section(id, it) } }
        }
        return emptyList()
    }

    fun navigationBarSections(service: AccessibilityService): List<Section> {
        val wm = service.getSystemService(android.view.WindowManager::class.java) ?: return emptyList()
        val metrics = wm.currentWindowMetrics
        val bounds = metrics.bounds
        val insets = metrics.windowInsets
        val gestural = runCatching {
            android.provider.Settings.Secure.getInt(service.contentResolver, "navigation_mode")
        }.getOrDefault(0) == 2
        val result = mutableListOf<Section>()
        val nav = insets.getInsets(android.view.WindowInsets.Type.navigationBars())
        if (nav.bottom > 0) {
            result += Section(if (gestural) "gesture_bar" else "navigation_bar", Rect(bounds.left, bounds.bottom - nav.bottom, bounds.right, bounds.bottom))
        }
        if (nav.left > 0) result += Section("navigation_bar", Rect(bounds.left, bounds.top, bounds.left + nav.left, bounds.bottom))
        if (nav.right > 0) result += Section("navigation_bar", Rect(bounds.right - nav.right, bounds.top, bounds.right, bounds.bottom))
        val gestures = insets.getInsets(android.view.WindowInsets.Type.mandatorySystemGestures())
        if (gestural && gestures.bottom > nav.bottom) {
            result += Section("gesture_area", Rect(bounds.left, bounds.bottom - gestures.bottom, bounds.right, bounds.bottom))
        }
        return result
    }

    fun edges(service: AccessibilityService, ignoreNotifications: Boolean = false): Edges? {
        val root = statusBarRoot(service) ?: return null
        val rootBounds = Rect().also { root.getBoundsInScreen(it) }
        val maxContainer = rootBounds.width() / 2
        val start = startBounds(root, ignoreNotifications, maxContainer)
        var end = END_IDS.flatMap { visibleBounds(root, it) } +
            END_ICON_CONTAINERS.flatMap { childBounds(root, it) }
        if (end.isEmpty()) end = END_FALLBACKS.flatMap { visibleBounds(root, it) }.filter { it.width() < maxContainer }
        return Edges(start.maxOfOrNull { it.right }, end.minOfOrNull { it.left })
    }

    fun dumpTree(service: AccessibilityService): List<String> {
        val root = statusBarRoot(service) ?: return emptyList()
        val out = mutableListOf<String>()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            val r = Rect().also { node.getBoundsInScreen(it) }
            out += "  ".repeat(depth) + "${node.viewIdResourceName?.substringAfter(":id/") ?: "-"} " +
                "${node.className?.toString()?.substringAfterLast('.')} $r vis=${node.isVisibleToUser} " +
                "t=${node.text ?: ""} d=${node.contentDescription ?: ""}"
            for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(root, 0)
        return out
    }

    private val NOTIFICATION_KEY = Regex("""^-?\d+\|.+\|.*""")
    private val TIME_TEXT = Regex("""^\d{1,2}[:.]\d{2}.*""")

    private fun startBounds(root: AccessibilityNodeInfo, ignoreNotifications: Boolean, maxContainer: Int): List<Rect> {
        val clocks = visibleBounds(root, "clock")
        val container = START_FALLBACKS.firstNotNullOfOrNull { id -> nodes(root, id).firstOrNull { it.isVisibleToUser } }
        val clockText = mutableListOf<Rect>()
        val others = mutableListOf<Rect>()
        if (container != null) collectStart(container, clockText, others, ignoreNotifications)
        if (ignoreNotifications) return clocks + clockText
        val legacy = START_IDS.flatMap { visibleBounds(root, it) } + START_ICON_CONTAINERS.flatMap { childBounds(root, it) }
        val found = clocks + clockText + others + legacy
        if (found.isNotEmpty()) return found
        return START_FALLBACKS.flatMap { visibleBounds(root, it) }.filter { it.width() < maxContainer }
    }

    private fun collectStart(node: AccessibilityNodeInfo, clock: MutableList<Rect>, others: MutableList<Rect>, ignoreNotifications: Boolean) {
        val id = node.viewIdResourceName?.substringAfter(":id/")
        val isNotification = id == "notification_icon_area" || id == "notificationIcons" ||
            id?.startsWith("ongoing_") == true || (id != null && NOTIFICATION_KEY.matches(id))
        if (isNotification) {
            if (!ignoreNotifications) node.visibleRect()?.let { others += it }
            return
        }
        val text = node.text?.toString()
        if (text != null && TIME_TEXT.matches(text.trim())) {
            node.visibleRect()?.let { clock += it }
            return
        }
        if (node.childCount == 0) {
            node.visibleRect()?.let { others += it }
            return
        }
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectStart(it, clock, others, ignoreNotifications) }
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
