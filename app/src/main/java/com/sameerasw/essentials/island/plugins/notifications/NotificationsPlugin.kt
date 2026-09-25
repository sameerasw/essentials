package com.sameerasw.essentials.island.plugins.notifications

import com.sameerasw.essentials.island.model.QueueInfo
import com.sameerasw.essentials.island.model.StackIcon
import android.widget.Toast
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
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
import com.sameerasw.essentials.island.model.InteractionOverrides
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
        SettingsRepository.KEY_ISLAND_SHOW_GLOW,
        SettingsRepository.KEY_ISLAND_NOTIF_QUEUE,
        SettingsRepository.KEY_ISLAND_NOTIF_TAP_TO_OPEN,
        SettingsRepository.KEY_ISLAND_SHOW_NOTIFICATIONS,
        SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED,
    )

    private val alerts = ArrayDeque<ActiveNotificationAlert>()
    private var currentIndex = 0
    
    private var manualPick = false
    
    private var pendingPopUp = false

    private fun busyElsewhere(): Boolean {
        val c = ctx ?: return false
        val stage = c.currentStage()
        return c.focusedKey() != ITEM_KEY && (stage == IslandStage.Line || stage == IslandStage.Expanded)
    }

    override fun onFocusChanged(stage: IslandStage, focusedKey: String?) {
        if (!pendingPopUp || alerts.isEmpty()) return
        if (stage == IslandStage.Line || stage == IslandStage.Expanded) return
        pendingPopUp = false
        scheduleTimeout()
        popUp()
    }

    private fun currentAlert(): ActiveNotificationAlert? = alerts.getOrNull(currentIndex.coerceIn(0, (alerts.size - 1).coerceAtLeast(0)))

    private fun removeCurrent() {
        if (alerts.isEmpty()) return
        alerts.removeAt(currentIndex.coerceIn(0, alerts.size - 1))
        currentIndex = currentIndex.coerceIn(0, (alerts.size - 1).coerceAtLeast(0))
    }

    private fun select(key: String) {
        val c = ctx ?: return
        val index = alerts.indexOfFirst { it.key == key }
        if (index < 0) return
        val here = showingHere()
        if (here && index == currentIndex) return
        currentIndex = index
        manualPick = true
        pendingPopUp = false
        scheduleTimeout()
        render()
        if (!here) {
            if (c.currentStage() == IslandStage.Expanded) c.request(PluginRequest.Expand(ITEM_KEY)) else popUp()
        }
    }
    private var registered = false
    private var autoExpanded = false

    private val timeoutRunnable = Runnable { onTimeout() }
    private val catchUpRunnable = Runnable { clearAll() }

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
        if (alerts.isEmpty() || pendingPopUp) return
        if (focusedKey == ITEM_KEY) autoExpanded = false
        ctx?.mainHandler?.removeCallbacks(catchUpRunnable)
        scheduleTimeout()
    }

    override fun onScreenStateChanged() {
        if (ctx?.isContentSuppressed?.invoke() == true) clearAll() else render()
    }

    private fun concealed(): Boolean =
        settings.getBoolean(SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED, false) &&
            (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    private fun onPosted(alert: ActiveNotificationAlert) {
        val c = ctx ?: return
        if (c.isContentSuppressed() || !settings.isIslandShowNotificationsEnabled()) return
        val existing = alerts.indexOfFirst { it.key == alert.key }
        if (existing >= 0) {
            alerts[existing] = alert
            render()
            return
        }
        c.mainHandler.removeCallbacks(catchUpRunnable)
        if (queueEnabled() && showingHere()) {
            alerts.addLast(alert)
            while (alerts.size > MAX_QUEUE) {
                alerts.removeAt(if (currentIndex == 0) 1 else 0)
                if (currentIndex > 0) currentIndex--
            }
            render()
            return
        }
        val waiting = pendingPopUp && queueEnabled()
        if (waiting) alerts.addLast(alert) else alerts.addFirst(alert)
        if (!waiting) currentIndex = 0
        manualPick = false
        val cap = if (queueEnabled()) MAX_QUEUE else 1
        while (alerts.size > cap) alerts.removeLast()
        if (busyElsewhere()) {
            pendingPopUp = true
            cancelTimers()
            render()
            return
        }
        scheduleTimeout()
        render()
        popUp()
    }

    private fun showingHere(): Boolean {
        val c = ctx ?: return false
        val stage = c.currentStage()
        return c.focusedKey() == ITEM_KEY && (stage == IslandStage.Line || stage == IslandStage.Expanded)
    }

    private fun popUp() {
        val c = ctx ?: return
        val lineAvailable = settings.isIslandLineStageEnabled() && settings.isIslandNotifCompactHeadsUpEnabled()
        c.request(
            if (lineAvailable) PluginRequest.Peek(ITEM_KEY, settings.getIslandTimeoutMs(), sticky = true)
            else PluginRequest.Expand(ITEM_KEY).also { autoExpanded = true },
        )
    }

    private fun onRemoved(key: String) {
        val index = alerts.indexOfFirst { it.key == key }
        if (index < 0) return
        alerts.removeAt(index)
        if (index < currentIndex) currentIndex--
        currentIndex = currentIndex.coerceIn(0, (alerts.size - 1).coerceAtLeast(0))
        if (alerts.isEmpty()) clearAll() else render()
    }

    private fun onTimeout() {
        val c = ctx ?: return
        if (pendingPopUp) return
        if (showingHere() && alerts.size > 1 && !manualPick) {
            removeCurrent()
            scheduleTimeout()
            render()
            if (c.currentStage() == IslandStage.Line) popUp()
            return
        }
        if (manualPick) {
            if (c.focusedKey() == ITEM_KEY) c.request(PluginRequest.Collapse(ITEM_KEY))
            clearAll()
            return
        }
        if (c.focusedKey() == ITEM_KEY && c.currentStage() == IslandStage.Line) {
            c.request(PluginRequest.Collapse(ITEM_KEY))
        } else if (c.focusedKey() == ITEM_KEY && c.currentStage() == IslandStage.Expanded) {
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
        clearAll()
    }

    private fun scheduleTimeout() {
        val handler = ctx?.mainHandler ?: return
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, settings.getIslandTimeoutMs())
    }

    private fun cancelTimers() {
        ctx?.mainHandler?.removeCallbacks(timeoutRunnable)
        ctx?.mainHandler?.removeCallbacks(catchUpRunnable)
    }

    private fun clearAll() {
        cancelTimers()
        alerts.clear()
        currentIndex = 0
        manualPick = false
        pendingPopUp = false
        render()
    }

    private fun popCurrent(reExpand: Boolean) {
        removeCurrent()
        if (alerts.isEmpty()) {
            clearAll()
            return
        }
        scheduleTimeout()
        render()
        if (reExpand) ctx?.request?.invoke(PluginRequest.Expand(ITEM_KEY))
    }

    private fun render() {
        val alert = currentAlert()
        if (alert == null) {
            publish(null)
            return
        }
        val index = alerts.indexOf(alert)
        val next = if (queueEnabled()) alerts.getOrNull(index + 1) ?: alerts.getOrNull(index - 1) else null
        val stack = alerts.map { stackIconFor(it, current = it === alert) }
        publish(
            itemFor(alert).let { current ->
                current.withStack(
                    queue = next?.let {
                        QueueInfo(
                            next = itemFor(it).withStack(null, alerts.map { a -> stackIconFor(a, current = a === it) }),
                            onAdvance = { popCurrent(reExpand = false) },
                        )
                    },
                    stack = stack,
                )
            },
        )
    }

    private fun queueEnabled() = settings.isIslandNotifQueueEnabled()

    private fun itemFor(alert: ActiveNotificationAlert): IslandItem {
        if (concealed()) return concealedItemFor(alert)
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

    private fun concealedItemFor(alert: ActiveNotificationAlert): IslandItem {
        val icon = alert.appIcon ?: alert.icon
        val open = {
            if (!sendPendingIntent(context, alert.contentIntent)) launchPackage(context, alert.packageName)
            popCurrent(reExpand = false)
        }
        return IslandItem(
            key = ITEM_KEY,
            priority = IslandPriority.NOTIFICATION,
            placement = CompactPlacement.Dynamic,
            compact = listOf(
                CompactCell("notif.icon") {
                    IslandBitmap(icon, 22.dp, fallbackRes = R.drawable.rounded_notifications_unread_24)
                },
            ),
            line = LineContent(
                icon = { IslandBitmap(icon, 24.dp, fallbackRes = R.drawable.rounded_notifications_unread_24) },
                start = appNameFor(context, alert),
                end = "",
            ),
            accent = alert.appColor?.let { Color(soften(it)) },
            dismissible = true,
            onDismiss = {
                NotificationListener.dismissNotification(alert.key)
                popCurrent(reExpand = false)
            },
            onOpen = open,
            interactions = InteractionOverrides(onTap = { open(); true }),
            sourcePackage = alert.packageName,
        )
    }

    private fun stackIconFor(alert: ActiveNotificationAlert, current: Boolean): StackIcon {
        val icon = if (concealed()) alert.appIcon ?: alert.icon else alert.chatIcon ?: alert.appIcon ?: alert.icon
        return StackIcon(alert.key, current = current, onSelect = { select(alert.key) }) { size ->
            IslandBitmap(icon, size, circle = true, fallbackRes = R.drawable.rounded_notifications_unread_24)
        }
    }

    private fun IslandItem.withStack(queue: QueueInfo?, stack: List<StackIcon>) = IslandItem(
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
        stack = stack,
    )

    private fun sendReply(alert: ActiveNotificationAlert, action: NotificationActionItem, text: String) {
        val canCarryText = !action.remoteInputs.isNullOrEmpty()
        val sent = canCarryText && NotificationListener.instance?.performNotificationAction(action, text) == true
        if (sent) {
            Toast.makeText(context, R.string.island_reply_sent, Toast.LENGTH_SHORT).show()
            if (currentAlert()?.key == alert.key) popCurrent(reExpand = false)
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
        if (currentAlert()?.key == alert.key) popCurrent(reExpand = true)
    }

    companion object {
        const val ITEM_KEY = "notifications"
        private const val MAX_QUEUE = 20

        private val GENERIC_TITLES = setOf(
            "you", "whatsapp", "messages", "telegram", "gmail", "instagram", "slack", "discord", "essentials",
        )

        fun appNameFor(context: Context, alert: ActiveNotificationAlert): String =
            alert.appName?.trim() ?: try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(alert.packageName, 0)).toString().trim()
            } catch (_: Exception) {
                ""
            }

        fun senderAndMessage(context: Context, alert: ActiveNotificationAlert): Pair<String, String> {
            val appName = appNameFor(context, alert)
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
