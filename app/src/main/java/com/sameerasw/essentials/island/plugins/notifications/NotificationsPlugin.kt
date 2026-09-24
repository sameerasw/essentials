package com.sameerasw.essentials.island.plugins.notifications

import com.sameerasw.essentials.island.model.QueueInfo
import android.widget.Toast
import android.accessibilityservice.AccessibilityService
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.domain.model.NotificationActionItem
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.plugins.soften
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.HapticUtil

class NotificationsPlugin : BaseIslandPlugin() {
    override val id = "notifications"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_CATCH_UP_ENABLED,
        SettingsRepository.KEY_ISLAND_NOTIF_COMPACT_HEADS_UP,
        SettingsRepository.KEY_ISLAND_NOTIF_ARRIVAL_STYLE,
        SettingsRepository.KEY_ISLAND_SHOW_GLOW,
        SettingsRepository.KEY_ISLAND_NOTIF_QUEUE,
        SettingsRepository.KEY_ISLAND_NOTIF_TAP_TO_OPEN,
        SettingsRepository.KEY_ISLAND_SHOW_NOTIFICATIONS,
        SettingsRepository.KEY_ISLAND_NOTIF_PERSISTENT,
        SettingsRepository.KEY_ISLAND_NOTIF_COMPACT_CYCLE,
    )

    private val alerts = ArrayDeque<ActiveNotificationAlert>()
    private var registered = false
    private var autoExpanded = false

    private val timeoutRunnable = Runnable { onTimeout() }
    private val catchUpRunnable = Runnable { clearAll() }
    // Rotates the compact pill icon every CYCLE_INTERVAL_MS when multiple notifications are queued
    private val cycleRunnable = Runnable { onCycleTick() }

    private val listener = object : NotificationListener.NotificationAlertListener {
        override fun onNotificationAlertPosted(alert: ActiveNotificationAlert) {
            ctx?.mainHandler?.post { onPosted(alert) }
        }

        override fun onNotificationAlertRemoved(key: String) {
            ctx?.mainHandler?.post { onRemoved(key) }
        }
    }

    override fun onStart() {
        if (!registered) {
            NotificationListener.addNotificationAlertListener(listener)
            registered = true
        }
    }

    override fun onStop() {
        if (registered) {
            NotificationListener.removeNotificationAlertListener(listener)
            registered = false
        }
        cancelTimers()
        alerts.clear()
    }

    override fun refresh() {
        if (ctx == null) return
        if (!settings.isIslandShowNotificationsEnabled()) clearAll() else render()
    }

    override fun onUserInteraction(focusedKey: String?) {
        if (alerts.isEmpty()) return
        if (focusedKey == ITEM_KEY) autoExpanded = false
        ctx?.mainHandler?.removeCallbacks(catchUpRunnable)
        scheduleTimeout()
    }

    override fun onScreenStateChanged() {
        val suppressed = ctx?.isContentSuppressed?.invoke() == true
        if (suppressed) {
            if (!settings.isIslandNotifPersistentEnabled()) clearAll()
            // Always pause icon cycling when screen is off / suppressed
            ctx?.mainHandler?.removeCallbacks(cycleRunnable)
        } else {
            scheduleCycle()
        }
    }

    private fun onPosted(alert: ActiveNotificationAlert) {
        val c = ctx ?: return
        if (c.isContentSuppressed() || !settings.isIslandShowNotificationsEnabled()) return
        alerts.removeAll { it.key == alert.key }
        alerts.addFirst(alert)
        val cap = if (queueEnabled()) MAX_QUEUE else 1
        while (alerts.size > cap) alerts.removeLast()
        c.mainHandler.removeCallbacks(catchUpRunnable)
        scheduleTimeout()
        scheduleCycle()
        render()
        val expandedElsewhere = c.currentStage() == IslandStage.Expanded
        if (!expandedElsewhere) {
            when (settings.getIslandNotifArrivalStyle()) {
                SettingsRepository.NOTIF_ARRIVAL_SILENT_PILL -> {
                    autoExpanded = false
                }
                SettingsRepository.NOTIF_ARRIVAL_FULL_EXPAND -> {
                    c.request(PluginRequest.Expand(ITEM_KEY).also { autoExpanded = true })
                }
                else -> { // NOTIF_ARRIVAL_LINE_PEEK (default)
                    if (settings.isIslandLineStageEnabled()) {
                        c.request(PluginRequest.Peek(ITEM_KEY, settings.getIslandTimeoutMs()))
                    } else {
                        autoExpanded = false
                    }
                }
            }
        }
    }

    private fun onRemoved(key: String) {
        if (alerts.removeAll { it.key == key }) {
            if (alerts.isEmpty()) clearAll() else { scheduleCycle(); render() }
        }
    }

    private fun onTimeout() {
        val c = ctx ?: return
        if (c.focusedKey() == ITEM_KEY && c.currentStage() == IslandStage.Expanded) {
            if (autoExpanded) {
                autoExpanded = false
                c.request(PluginRequest.Collapse(ITEM_KEY))
            }
            scheduleTimeout()
            return
        }
        if (settings.isIslandCatchUpEnabled() && alerts.isNotEmpty()) {
            if (!settings.isIslandCatchUpInfinite()) c.mainHandler.postDelayed(catchUpRunnable, settings.getIslandCatchUpTimeoutMs())
            return
        }
        // Persistent: collapse to compact pill instead of clearing all
        if (settings.isIslandNotifPersistentEnabled() && alerts.isNotEmpty()) {
            c.request(PluginRequest.Collapse(ITEM_KEY))
            scheduleCycle()
            return
        }
        clearAll()
    }

    /** Rotates the front notification to the back so compact pill icons cycle non-destructively. */
    private fun advanceQueue() {
        val front = alerts.removeFirstOrNull() ?: return
        alerts.addLast(front)
        render()
    }

    private fun onCycleTick() {
        if (alerts.size > 1 && settings.isIslandNotifCompactCycleEnabled()) {
            advanceQueue()
        }
        scheduleCycle()
    }

    private fun scheduleCycle() {
        val handler = ctx?.mainHandler ?: return
        handler.removeCallbacks(cycleRunnable)
        if (alerts.size > 1 && settings.isIslandNotifCompactCycleEnabled()) {
            handler.postDelayed(cycleRunnable, CYCLE_INTERVAL_MS)
        }
    }

    private fun scheduleTimeout() {
        val handler = ctx?.mainHandler ?: return
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, settings.getIslandTimeoutMs())
    }

    private fun cancelTimers() {
        ctx?.mainHandler?.removeCallbacks(timeoutRunnable)
        ctx?.mainHandler?.removeCallbacks(catchUpRunnable)
        ctx?.mainHandler?.removeCallbacks(cycleRunnable)
    }

    private fun clearAll() {
        cancelTimers()
        alerts.clear()
        render()
    }

    private fun popCurrent(reExpand: Boolean) {
        alerts.removeFirstOrNull()
        if (alerts.isEmpty()) {
            clearAll()
            return
        }
        scheduleTimeout()
        scheduleCycle()
        render()
        if (reExpand) ctx?.request?.invoke(PluginRequest.Expand(ITEM_KEY))
    }

    private fun render() {
        val alert = alerts.firstOrNull()
        if (alert == null) {
            publish(null)
            return
        }
        val next = if (queueEnabled()) alerts.getOrNull(1) else null
        publish(
            itemFor(alert).let { current ->
                if (next == null) {
                    current
                } else {
                    current.withQueue(QueueInfo(next = itemFor(next), onAdvance = { popCurrent(reExpand = false) }))
                }
            },
        )
    }

    private fun queueEnabled() = settings.isIslandNotifQueueEnabled()

    private fun itemFor(alert: ActiveNotificationAlert): IslandItem {
        val (sender, message) = senderAndMessage(context, alert)
        val showGlow = settings.isIslandShowGlowEnabled()
        val tapToOpen = settings.isIslandNotifTapToOpenEnabled()
        val icon = alert.chatIcon ?: alert.appIcon ?: alert.icon
        val accent = alert.appColor?.let { Color(soften(it)) }
        return IslandItem(
            key = ITEM_KEY,
            priority = IslandPriority.NOTIFICATION,
            placement = CompactPlacement.Dynamic,
            compact = buildList {
                add(
                    CompactCell("notif.icon") {
                        IslandBitmap(icon, 22.dp, fallbackRes = R.drawable.rounded_notifications_unread_24)
                    },
                )
                val appIcon = alert.appIcon
                if (alert.chatIcon != null && appIcon != null) {
                    add(CompactCell("notif.app", soloOnly = true) { IslandBitmap(appIcon, 22.dp, circle = true) })
                }
            },
            line = LineContent(
                icon = { IslandBitmap(icon, 24.dp, fallbackRes = R.drawable.rounded_notifications_unread_24) },
                start = sender,
                end = message,
            ),
            expanded = ExpandedContent { scope ->
                NotificationExpanded(
                    alert = alert,
                    sender = sender,
                    message = message,
                    showGlow = showGlow,
                    onAction = { runAction(alert, it) },
                    onReply = { action, text -> sendReply(alert, action, text) },
                    scope = scope,
                    tapToOpen = tapToOpen,
                )
            },
            accent = accent,
            dismissible = true,
            onDismiss = {
                NotificationListener.dismissNotification(alert.key)
                popCurrent(reExpand = false)
            },
            onOpen = {
                if (!sendPendingIntent(context, alert.contentIntent)) launchPackage(context, alert.packageName)
                popCurrent(reExpand = false)
            },
            sourcePackage = alert.packageName,
        )
    }

    private fun IslandItem.withQueue(queue: QueueInfo) = IslandItem(
        key = key,
        priority = priority,
        placement = placement,
        compact = compact,
        line = line,
        expanded = expanded,
        accent = accent,
        dismissible = dismissible,
        onDismiss = onDismiss,
        onOpen = onOpen,
        interactions = interactions,
        queue = queue,
        sourcePackage = sourcePackage,
    )

    private fun sendReply(alert: ActiveNotificationAlert, action: NotificationActionItem, text: String) {
        val canCarryText = !action.remoteInputs.isNullOrEmpty()
        val sent = canCarryText && NotificationListener.instance?.performNotificationAction(action, text) == true
        if (sent) {
            Toast.makeText(context, R.string.island_reply_sent, Toast.LENGTH_SHORT).show()
            if (alerts.firstOrNull()?.key == alert.key) popCurrent(reExpand = false)
        } else {
            popCurrent(reExpand = false)
            context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    private fun runAction(alert: ActiveNotificationAlert, action: NotificationActionItem) {
        HapticUtil.performStrongTickHaptic(context)
        if (action.isQuickReply) {
            popCurrent(reExpand = false)
            context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            return
        }
        if (action.pendingIntent != null) NotificationListener.instance?.performNotificationAction(action)
        if (alerts.firstOrNull()?.key == alert.key) popCurrent(reExpand = true)
    }

    companion object {
        const val ITEM_KEY = "notifications"
        private const val MAX_QUEUE = 5
        private const val CYCLE_INTERVAL_MS = 5_000L

        private val GENERIC_TITLES = setOf(
            "you", "whatsapp", "messages", "telegram", "gmail", "instagram", "slack", "discord", "essentials",
        )

        fun senderAndMessage(context: Context, alert: ActiveNotificationAlert): Pair<String, String> {
            val appName = alert.appName?.trim() ?: try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(alert.packageName, 0)).toString().trim()
            } catch (_: Exception) {
                ""
            }
            var sender = alert.senderName?.trim().orEmpty()
            if (sender.isBlank() || sender.equals("You", true)) {
                val title = alert.title.trim()
                val clean = listOf(": ", " - ", " • ").firstNotNullOfOrNull { sep ->
                    if (appName.isNotBlank() && title.startsWith("$appName$sep", true)) title.substring(appName.length + sep.length).trim() else null
                } ?: title
                val isAppTitle = clean.isBlank() || clean.lowercase() in GENERIC_TITLES ||
                    (appName.isNotBlank() && clean.equals(appName, true)) || clean.equals(alert.packageName, true)
                sender = if (isAppTitle) appName.ifBlank { clean } else clean
            }
            if (sender.isBlank() || sender.equals("You", true)) sender = appName.ifBlank { "Notification" }

            var message = alert.text.trim()
            if (message.isBlank()) {
                val title = alert.title.trim()
                if (!title.equals(sender, true) && !title.equals(appName, true) && !title.equals("You", true)) message = title
            } else if (message.startsWith("$sender: ", true)) {
                message = message.substring(sender.length + 2).trim()
            }
            return sender to message
        }
    }
}
