package com.sameerasw.essentials.island.plugins.notifications

import com.sameerasw.essentials.island.model.QueueOverflow

import com.sameerasw.essentials.island.model.QueueInfo
import com.sameerasw.essentials.island.model.StackIcon
import android.widget.Toast
import kotlin.math.ceil
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ActiveNotificationAlert
import com.sameerasw.essentials.domain.model.NotificationActionItem
import com.sameerasw.essentials.island.model.SideBubble
import com.sameerasw.essentials.island.model.SideBubblePriority
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
import com.sameerasw.essentials.island.ui.IslandStackBubble
import com.sameerasw.essentials.island.ui.IslandStackOverlap
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.HapticUtil

class NotificationsPlugin : BaseIslandPlugin() {
    override val id = "notifications"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_CATCH_UP_ENABLED,
        SettingsRepository.KEY_ISLAND_NOTIF_PEEK_STYLE,
        SettingsRepository.KEY_ISLAND_NOTIF_QUEUE_STYLE,
        SettingsRepository.KEY_ISLAND_NOTIF_QUEUE_INLINE,
        SettingsRepository.KEY_ISLAND_LINE_STAGE_ENABLED,
        SettingsRepository.KEY_ISLAND_SHOW_GLOW,
        SettingsRepository.KEY_ISLAND_NOTIF_QUEUE,
        SettingsRepository.KEY_ISLAND_NOTIF_SKIP_SILENT,
        SettingsRepository.KEY_ISLAND_NOTIF_FOLLOW_DND,
        SettingsRepository.KEY_ISLAND_NOTIF_FILTER_APPS,
        SettingsRepository.KEY_ISLAND_NOTIF_FILTER_APPS_LIST,
        SettingsRepository.KEY_ISLAND_NOTIF_TAP_TO_OPEN,
        SettingsRepository.KEY_ISLAND_SHOW_NOTIFICATIONS,
        SettingsRepository.KEY_ISLAND_NOTIF_HUB,
        SettingsRepository.KEY_ISLAND_NOTIF_HUB_ICONS,
        SettingsRepository.KEY_ISLAND_NOTIF_HUB_ORDER,
        SettingsRepository.KEY_ISLAND_NOTIF_HUB_CHAT_PICTURES,
        SettingsRepository.KEY_ISLAND_NOTIF_HUB_OVERFLOW,
        SettingsRepository.KEY_ISLAND_NOTIF_HUB_SILENT,
        SettingsRepository.KEY_ISLAND_NOTIF_HUB_GROUP,
        SettingsRepository.KEY_ISLAND_NOTIF_DISABLE_LOCKED,
        SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED,
        SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_CHAT_PICTURES,
        SettingsRepository.KEY_ISLAND_OTP_ENABLED,
        SettingsRepository.KEY_ISLAND_OTP_TAP_TO_COPY,
        SettingsRepository.KEY_ISLAND_OTP_DISMISS_AFTER_COPY,
        SettingsRepository.KEY_ISLAND_OTP_BYPASS_FILTER,
        SettingsRepository.KEY_ISLAND_OTP_TIMEOUT_MS,
    )

    private val alerts = ArrayDeque<ActiveNotificationAlert>()
    private val seen = HashSet<String>()
    private var currentIndex = 0
    
    private var manualPick = false
    
    private var pendingPopUp = false
    private var catchingUp = false
    private var pinnedKey: String? = null

    private fun busyElsewhere(): Boolean {
        val c = ctx ?: return false
        val stage = c.currentStage()
        return c.focusedKey() != ITEM_KEY && (stage == IslandStage.Line || stage == IslandStage.Expanded)
    }

    private var wasHere = false

    override fun onFocusChanged(stage: IslandStage, focusedKey: String?) {
        val here = focusedKey == ITEM_KEY && (stage == IslandStage.Line || stage == IslandStage.Expanded)
        val left = wasHere && !here
        wasHere = here
        if (left && !pendingPopUp && alerts.isNotEmpty() && currentAlert()?.hubOnly == false && settings.isIslandCatchUpEnabled()) {
            val c = ctx
            if (c != null) {
                c.mainHandler.removeCallbacks(timeoutRunnable)
                c.mainHandler.removeCallbacks(catchUpRunnable)
                if (!settings.isIslandCatchUpInfinite()) c.mainHandler.postDelayed(catchUpRunnable, settings.getIslandCatchUpTimeoutMs())
            }
            pinLatest()
            catchingUp = true
        }
        if (catchingUp) render()
        if (!pendingPopUp || alerts.isEmpty()) return
        if (stage == IslandStage.Line || stage == IslandStage.Expanded) return
        pendingPopUp = false
        scheduleTimeout()
        popUp()
    }

    private fun currentAlert(): ActiveNotificationAlert? = alerts.getOrNull(currentIndex.coerceIn(0, (alerts.size - 1).coerceAtLeast(0)))

    private fun waitingInOrder(current: ActiveNotificationAlert): List<ActiveNotificationAlert> {
        val index = alerts.indexOf(current)
        if (index < 0) return alerts.filter { it !== current }
        return alerts.drop(index + 1) + alerts.take(index).asReversed()
    }

    private fun markSeen() {
        currentAlert()?.let { seen.add(it.key) }
    }

    private fun cycles(from: ActiveNotificationAlert, to: ActiveNotificationAlert) = from.hubOnly || !to.hubOnly

    private fun advanceCurrent() {
        if (alerts.size < 2) return
        val start = currentIndex.coerceIn(0, alerts.size - 1)
        val from = alerts[start]
        for (offset in 1 until alerts.size) {
            val index = (start + offset) % alerts.size
            if (cycles(from, alerts[index])) {
                currentIndex = index
                markSeen()
                return
            }
        }
    }

    private fun advanceToUnseen(): Boolean {
        if (alerts.size < 2) return false
        val start = currentIndex.coerceIn(0, alerts.size - 1)
        for (offset in 1 until alerts.size) {
            val index = (start + offset) % alerts.size
            if (alerts[index].key !in seen) {
                currentIndex = index
                markSeen()
                return true
            }
        }
        return false
    }

    private fun select(key: String) {
        val c = ctx ?: return
        val index = alerts.indexOfFirst { it.key == key }
        if (index < 0) return
        catchingUp = false
        val here = showingHere()
        if (here && index == currentIndex) return
        currentIndex = index
        markSeen()
        manualPick = true
        pendingPopUp = false
        scheduleTimeout()
        render()
        if (!here) {
            autoExpanded = false
            c.request(PluginRequest.Expand(ITEM_KEY))
        }
    }
    private var registered = false
    private var autoExpanded = false

    private val timeoutRunnable = Runnable { onTimeout() }
    private val catchUpRunnable = Runnable {
        if (ctx?.isContentSuppressed?.invoke() == true) catchUpPaused = true else endSession()
    }

    private val replied = HashMap<String, Long>()
    private var catchUpPaused = false
    private var copiedKey: String? = null
    private var postedWhileCopied = false
    private val copiedRunnable = Runnable { finishCopy() }

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
        if (hub()) syncHub()
    }

    override fun onStop() {
        if (registered) {
            NotificationListener.removeNotificationAlertListener(listener)
            registered = false
        }
        cancelTimers()
        ctx?.mainHandler?.removeCallbacks(unpinRunnable)
        pinnedKey = null
        alerts.clear()
        seen.clear()
    }

    override fun refresh() {
        if (ctx == null) return
        if (!settings.isIslandShowNotificationsEnabled()) {
            clearAll()
            return
        }
        if (hub()) {
            syncHub()
        } else {
            if (alerts.removeAll { it.hubOnly || it.ongoing }) currentIndex = currentIndex.coerceIn(0, (alerts.size - 1).coerceAtLeast(0))
            if (alerts.isEmpty()) clearAll() else render()
        }
    }

    private fun hub() = settings.isIslandNotifHubEnabled() && settings.isIslandShowNotificationsEnabled()

    private fun maxQueue() = if (hub()) HUB_MAX_QUEUE else MAX_QUEUE

    private var hubSyncGeneration = 0

    private fun syncHub() {
        val generation = ++hubSyncGeneration
        val listener = NotificationListener.instance
        if (listener == null) {
            applyHub(emptyList())
            return
        }
        listener.hubAlertsAsync { live ->
            if (generation == hubSyncGeneration && ctx != null) applyHub(live)
        }
    }

    private fun applyHub(allLive: List<ActiveNotificationAlert>) {
        val live = allLive.filter { passesFilters(it) }
        val liveKeys = live.mapTo(HashSet()) { it.key }
        val stale = alerts.filter { it.key !in liveKeys && !it.key.startsWith("simulated_") }
        stale.forEach { removeQuietly(it.key) }
        live.forEach { alert ->
            val index = alerts.indexOfFirst { it.key == alert.key }
            if (index >= 0) {
                alerts[index] = alert.copy(hubOnly = alerts[index].hubOnly)
            } else {
                insertSilently(alert)
            }
        }
        if (alerts.isEmpty()) clearAll() else render()
    }

    private fun removeQuietly(key: String) {
        val index = alerts.indexOfFirst { it.key == key }
        if (index < 0) return
        alerts.removeAt(index)
        seen.remove(key)
        if (index < currentIndex) currentIndex--
        currentIndex = currentIndex.coerceIn(0, (alerts.size - 1).coerceAtLeast(0))
    }

    private fun insertSilently(alert: ActiveNotificationAlert) {
        val at = alerts.indexOfFirst { it.timestamp < alert.timestamp }.let { if (it < 0) alerts.size else it }
        alerts.add(at, alert)
        if (at <= currentIndex && alerts.size > 1) currentIndex++
        seen.add(alert.key)
        while (alerts.size > maxQueue()) alerts.removeLast()
    }

    private fun addSilently(alert: ActiveNotificationAlert) {
        if (!passesFilters(alert)) {
            removeQuietly(alert.key)
            render()
            return
        }
        val index = alerts.indexOfFirst { it.key == alert.key }
        if (index >= 0) alerts[index] = alert.copy(hubOnly = alerts[index].hubOnly) else insertSilently(alert)
        render()
    }

    override fun onSuppressionChanged(suppressed: Boolean) {
        val c = ctx ?: return
        if (suppressed) {
            if (catchingUp) {
                c.mainHandler.removeCallbacks(catchUpRunnable)
                catchUpPaused = true
            }
        } else if (catchUpPaused) {
            catchUpPaused = false
            if (alerts.isNotEmpty() && catchingUp && !settings.isIslandCatchUpInfinite()) {
                c.mainHandler.postDelayed(catchUpRunnable, settings.getIslandCatchUpTimeoutMs())
            }
        }
    }

    override fun onUserInteraction(focusedKey: String?) {
        if (alerts.isEmpty() || pendingPopUp) return
        if (focusedKey == ITEM_KEY) autoExpanded = false
        ctx?.mainHandler?.removeCallbacks(catchUpRunnable)
        scheduleTimeout()
    }

    override fun onScreenStateChanged() {
        if (hub()) {
            syncHub()
            return
        }
        if (ctx?.isContentSuppressed?.invoke() == true) clearAll() else render()
    }

    private fun passesFilters(alert: ActiveNotificationAlert): Boolean {
        if (alert.isSilent && settings.isIslandNotifSkipSilentEnabled()) return false
        if (alert.blockedByDnd && settings.isIslandNotifFollowDndEnabled()) return false
        if (isOtp(alert) && settings.isIslandOtpBypassFilterEnabled()) return true
        if (!settings.isIslandNotifFilterAppsEnabled()) return true
        return settings.loadIslandNotifFilterApps().any { it.isEnabled && it.packageName == alert.packageName }
    }

    private fun otpCodeOf(alert: ActiveNotificationAlert) = alert.otpCode?.takeIf { settings.isIslandOtpEnabled() }

    private fun isOtp(alert: ActiveNotificationAlert) = otpCodeOf(alert) != null

    private fun timeoutMs(): Long {
        val base = settings.getIslandTimeoutMs()
        return if (currentAlert()?.let(::otpCodeOf) != null) settings.getIslandOtpTimeoutMs() else base
    }

    private fun maskOtp(code: String) = formatOtp(code).map { if (it == ' ') ' ' else '•' }.joinToString("")

    private fun lockedOut(): Boolean =
        settings.getBoolean(SettingsRepository.KEY_ISLAND_NOTIF_DISABLE_LOCKED, true) &&
            ((context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true ||
                (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isInteractive == false)

    private fun concealed(): Boolean =
        settings.getBoolean(SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_LOCKED, false) &&
            (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    private fun onPosted(alert: ActiveNotificationAlert) {
        val c = ctx ?: return
        if (alert.hubOnly) {
            if (hub()) addSilently(alert)
            return
        }
        if (c.isContentSuppressed() || lockedOut() || !settings.isIslandShowNotificationsEnabled() || !passesFilters(alert)) {
            if (hub()) addSilently(alert)
            return
        }
        val existing = alerts.indexOfFirst { it.key == alert.key }
        if (existing >= 0) {
            alerts[existing] = alert
            render()
            return
        }
        val now = System.currentTimeMillis()
        replied.values.removeAll { now - it > REPLY_REPOST_WINDOW_MS }
        if (replied.containsKey(alert.key)) return
        if (copiedKey != null) {
            alerts.addLast(alert)
            while (alerts.size > MAX_QUEUE) alerts.removeFirst()
            postedWhileCopied = true
            return
        }
        c.mainHandler.removeCallbacks(catchUpRunnable)
        catchingUp = false
        if (queueEnabled() && showingHere()) {
            alerts.addLast(alert)
            while (alerts.size > maxQueue()) {
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
        val cap = if (queueEnabled()) maxQueue() else 1
        while (alerts.size > cap) alerts.removeLast()
        if (minimal()) {
            pendingPopUp = false
            holdMinimal()
            render()
            return
        }
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
        if (minimal()) return
        val otpNeedsExpand = currentAlert()?.let(::isOtp) == true && !settings.isIslandOtpTapToCopyEnabled()
        val lineAvailable = settings.isIslandLineStageEnabled() && !otpNeedsExpand &&
            peekStyle() != SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_EXPANDED
        markSeen()
        c.request(
            if (lineAvailable) PluginRequest.Peek(ITEM_KEY, timeoutMs(), sticky = true)
            else PluginRequest.Expand(ITEM_KEY).also { autoExpanded = true },
        )
    }

    private fun onRemoved(key: String) {
        val index = alerts.indexOfFirst { it.key == key }
        if (index < 0) return
        val removed = alerts.removeAt(index).also { seen.remove(it.key) }
        val wasCurrent = index == currentIndex && !removed.hubOnly
        if (index < currentIndex) currentIndex--
        currentIndex = currentIndex.coerceIn(0, (alerts.size - 1).coerceAtLeast(0))
        if (wasCurrent && alerts.getOrNull(currentIndex)?.hubOnly == true) {
            val fresh = (currentIndex until alerts.size).firstOrNull { !alerts[it].hubOnly }
                ?: (currentIndex - 1 downTo 0).firstOrNull { !alerts[it].hubOnly }
            if (fresh != null) {
                currentIndex = fresh
                markSeen()
            }
        }
        if (alerts.isEmpty()) clearAll() else render()
    }

    private fun onTimeout() {
        val c = ctx ?: return
        if (pendingPopUp) return
        if (showingHere() && alerts.size > 1 && !manualPick && advanceToUnseen()) {
            scheduleTimeout()
            render()
            if (c.currentStage() == IslandStage.Line) popUp()
            return
        }
        if (manualPick) {
            if (c.focusedKey() == ITEM_KEY) c.request(PluginRequest.Collapse(ITEM_KEY))
            endSession()
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
        if (settings.isIslandCatchUpEnabled() && alerts.isNotEmpty() && currentAlert()?.hubOnly == false) {
            if (!settings.isIslandCatchUpInfinite()) c.mainHandler.postDelayed(catchUpRunnable, settings.getIslandCatchUpTimeoutMs())
            pinLatest()
            catchingUp = true
            render()
            return
        }
        endSession()
    }

    private fun holdMinimal() {
        val c = ctx ?: return
        if (!settings.isIslandCatchUpEnabled()) {
            scheduleTimeout()
            return
        }
        cancelTimers()
        pinLatest()
        catchingUp = true
        if (!settings.isIslandCatchUpInfinite()) c.mainHandler.postDelayed(catchUpRunnable, settings.getIslandCatchUpTimeoutMs())
    }

    private val unpinRunnable = Runnable {
        pinnedKey = null
        render()
    }

    // The newest notification that just dropped into the catch-up bubble leads and gets a ring for the catch-up timeout
    private fun pinLatest() {
        val c = ctx ?: return
        if (!hub() || settings.getIslandNotifHubOrder() != SettingsRepository.ISLAND_NOTIF_HUB_ORDER_DYNAMIC) return
        c.mainHandler.removeCallbacks(unpinRunnable)
        pinnedKey = currentAlert()?.takeIf { !it.hubOnly }?.key
        if (pinnedKey == null) return
        val stay = settings.getIslandCatchUpStoredMs()
        if (stay > 0L) c.mainHandler.postDelayed(unpinRunnable, stay)
    }

    private fun bubbleCells(): Int {
        if (!hub() || settings.getIslandNotifQueueStyle() != SettingsRepository.ISLAND_NOTIF_QUEUE_STYLE_OVERLAP) return 1
        val cap = settings.getIslandNotifHubIcons().let { if (it == 0) AUTO_CELL_ICONS else it }
        val count = hubEntries().size
        val shown = count.coerceIn(1, cap)
        val overflow = if (count > cap && settings.getIslandNotifHubOverflow() != QueueOverflow.None) 0.3f else 0f
        return ceil(1f + 0.55f * (shown - 1) + overflow).toInt()
    }

    private fun priorityRank(alert: ActiveNotificationAlert) = when {
        alert.ongoing -> 0
        alert.isConversation -> 1
        alert.isSilent -> 3
        else -> 2
    }

    private class HubEntry(val key: String, val alerts: List<ActiveNotificationAlert>, val grouped: Boolean) {
        val lead = alerts.first()
        val timestamp = alerts.maxOf { it.timestamp }
    }

    private fun entryRank(entry: HubEntry) = if (entry.grouped) {
        if (entry.alerts.all { it.isSilent }) 3 else 2
    } else {
        priorityRank(entry.lead)
    }

    // Live updates and conversations always stand alone, everything else can fold into one entry per app
    private fun hubEntries(): List<HubEntry> {
        val shown = alerts
            .filter { settings.isIslandNotifHubIncludeSilent() || !it.isSilent || it.ongoing }
            .sortedByDescending { it.timestamp }
        val entries = if (settings.isIslandNotifHubGroupByApps()) {
            val chats = settings.isIslandNotifHubPreferChatPictures()
            val standalone = shown.filter { it.ongoing || (chats && it.isConversation) }.map { HubEntry(it.key, listOf(it), grouped = false) }
            val groups = shown.filterNot { it.ongoing || (chats && it.isConversation) }
                .groupBy { it.packageName }
                .map { (pkg, members) -> if (members.size == 1) HubEntry(members.first().key, members, grouped = false) else HubEntry("group:$pkg", members, grouped = true) }
            standalone + groups
        } else {
            shown.map { HubEntry(it.key, listOf(it), grouped = false) }
        }
        val mode = settings.getIslandNotifHubOrder()
        val recent = entries.sortedByDescending { it.timestamp }
        if (mode == SettingsRepository.ISLAND_NOTIF_HUB_ORDER_RECENT) return recent
        val byPriority = recent.sortedBy(::entryRank)
        val pinned = pinnedKey
            ?.takeIf { mode == SettingsRepository.ISLAND_NOTIF_HUB_ORDER_DYNAMIC }
            ?.let { key -> byPriority.firstOrNull { entry -> entry.alerts.any { it.key == key } } }
        return if (pinned != null) listOf(pinned) + byPriority.filter { it !== pinned } else byPriority
    }

    private fun hubIconFor(entry: HubEntry): StackIcon {
        val highlight = pinnedKey?.let { key -> entry.alerts.any { it.key == key } } == true
        if (!entry.grouped) return stackIconFor(entry.lead, current = false, highlight = highlight)
        val icon = entry.lead.appIcon ?: entry.lead.icon
        return StackIcon(entry.key, onSelect = { select(entry.lead.key) }, highlight = highlight) { size ->
            IslandBitmap(icon, size, circle = true, fallbackRes = R.drawable.rounded_notifications_unread_24)
        }
    }

    private fun scheduleTimeout() {
        val handler = ctx?.mainHandler ?: return
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(timeoutRunnable, timeoutMs())
    }

    private fun cancelTimers() {
        ctx?.mainHandler?.removeCallbacks(timeoutRunnable)
        ctx?.mainHandler?.removeCallbacks(catchUpRunnable)
        ctx?.mainHandler?.removeCallbacks(copiedRunnable)
    }

    private fun endSession() {
        if (!hub()) {
            clearAll()
            return
        }
        cancelTimers()
        ctx?.mainHandler?.removeCallbacks(unpinRunnable)
        pinnedKey = null
        catchingUp = false
        catchUpPaused = false
        manualPick = false
        pendingPopUp = false
        for (i in alerts.indices) if (!alerts[i].hubOnly) alerts[i] = alerts[i].copy(hubOnly = true)
        render()
    }

    private fun clearAll() {
        ctx?.mainHandler?.removeCallbacks(unpinRunnable)
        pinnedKey = null
        catchingUp = false
        catchUpPaused = false
        copiedKey = null
        postedWhileCopied = false
        cancelTimers()
        alerts.clear()
        seen.clear()
        currentIndex = 0
        manualPick = false
        pendingPopUp = false
        render()
    }

    private fun advanceQueue() {
        catchingUp = false
        advanceCurrent()
        scheduleTimeout()
        render()
    }

    private fun popCurrent(reExpand: Boolean) {
        val simulated = currentAlert()?.takeIf { it.key.startsWith("simulated_") }
        if (simulated != null) {
            catchingUp = false
            onRemoved(simulated.key)
            if (reExpand && alerts.isNotEmpty()) ctx?.request?.invoke(PluginRequest.Expand(ITEM_KEY))
            return
        }
        advanceQueue()
        if (reExpand) ctx?.request?.invoke(PluginRequest.Expand(ITEM_KEY))
    }

    private fun render() {
        if (copiedKey != null) {
            publish(copiedItem())
            return
        }
        val alert = currentAlert()
        if (alert == null || lockedOut()) {
            publish(null)
            return
        }
        val index = alerts.indexOf(alert)
        val next = if (queueEnabled()) {
            alerts.drop(index + 1).firstOrNull { cycles(alert, it) } ?: alerts.take(index).lastOrNull { cycles(alert, it) }
        } else {
            null
        }
        val stack = alerts.map { stackIconFor(it, current = it === alert) }
        val waitingAlerts = waitingInOrder(alert).filter { !it.hubOnly && it.key !in seen }
        val queued = if (hub()) {
            hubEntries().filter { entry -> entry.alerts.none { it === alert } }.map { hubIconFor(it) }
        } else {
            waitingAlerts.map { stackIconFor(it, current = false) }
        }
        val minimal = minimal()
        val waiting = if (minimal) waitingAlerts.map { stackIconFor(it, current = false) } else emptyList()
        val inlineQueue = minimal && waiting.isNotEmpty() && settings.isIslandNotifQueueInlineEnabled()
        val bubble = when {
            minimal && waiting.isNotEmpty() && !inlineQueue -> SideBubble(
                priority = SideBubblePriority.NOTIFICATION,
                icons = waiting,
                onOpen = { select(waiting.first().key) },
                onDismiss = { clearWaiting() },
            )
            minimal -> null
            hub() && hubEntries().isEmpty() -> null
            settings.isIslandCatchUpEnabled() -> SideBubble(
                priority = SideBubblePriority.NOTIFICATION,
                icons = if (hub()) {
                    hubEntries().map { hubIconFor(it) }
                } else {
                    alerts.map { stackIconFor(it, current = false) }
                },
                onOpen = { select(alert.key) },
                onDismiss = if (hub()) null else ({ clearAll() }),
                maxIcons = if (hub()) settings.getIslandNotifHubIcons() else null,
                overflow = if (hub()) settings.getIslandNotifHubOverflow() else QueueOverflow.Number,
                cells = bubbleCells(),
            )
            else -> null
        }
        publish(
            itemFor(alert).let { current ->
                current.withStack(
                    sideBubble = bubble,
                    compactVisible = minimal || bubble == null,
                    leadingCell = if (inlineQueue) queueCell(waiting) else null,
                    queue = next?.let {
                        QueueInfo(
                            next = itemFor(it).withStack(null, alerts.map { a -> stackIconFor(a, current = a === it) }),
                            onAdvance = { advanceQueue() },
                        )
                    },
                    stack = stack,
                    queued = queued,
                )
            },
        )
    }

    private fun queueEnabled() = settings.isIslandNotifQueueEnabled()

    private fun queueCell(icons: List<StackIcon>): CompactCell = CompactCell("notif.queue") {
        when (settings.getIslandNotifQueueStyle()) {
            SettingsRepository.ISLAND_NOTIF_QUEUE_STYLE_OVERLAP -> IslandStackOverlap(icons, 22.dp)
            else -> IslandStackBubble(icons, 26.dp)
        }
    }

    private fun clearWaiting() {
        val current = currentAlert() ?: return
        alerts.removeAll { it !== current }
        seen.retainAll { it == current.key }
        currentIndex = 0
        render()
    }

    private fun peekStyle(): String =
        if (settings.isIslandLineStageEnabled()) {
            settings.getIslandNotifPeekStyle()
        } else {
            SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_FULL
        }

    private fun condensedLine() = peekStyle() == SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_COMPACT

    private fun minimal() = peekStyle() == SettingsRepository.ISLAND_NOTIF_PEEK_STYLE_MINIMAL

    private fun itemFor(alert: ActiveNotificationAlert): IslandItem {
        if (concealed()) return concealedItemFor(alert)
        val code = otpCodeOf(alert)
        if (code != null) return otpItemFor(alert, code)
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
                if (!minimal() && alert.chatIcon != null && appIcon != null) {
                    add(CompactCell("notif.app", soloOnly = true) { IslandBitmap(appIcon, 22.dp, circle = true) })
                }
            },
            line = LineContent(
                icon = { IslandBitmap(icon, 24.dp, fallbackRes = R.drawable.rounded_notifications_unread_24) },
                start = sender,
                end = message,
                condensed = condensedLine(),
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
            contentId = alert.key,
        )
    }

    private fun hidesChatPictures() = settings.getBoolean(SettingsRepository.KEY_ISLAND_NOTIF_CONCEAL_CHAT_PICTURES, false)

    private fun concealedItemFor(alert: ActiveNotificationAlert): IslandItem {
        val hideChat = hidesChatPictures()
        val icon = if (hideChat) alert.appIcon ?: alert.icon else alert.chatIcon ?: alert.appIcon ?: alert.icon
        val open = {
            if (!sendPendingIntent(context, alert.contentIntent)) launchPackage(context, alert.packageName)
            popCurrent(reExpand = false)
        }
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
                if (!minimal() && !hideChat && alert.chatIcon != null && appIcon != null) {
                    add(CompactCell("notif.app", soloOnly = true) { IslandBitmap(appIcon, 22.dp, circle = true) })
                }
            },
            line = if (isOtp(alert)) {
                LineContent(
                    icon = { IslandBitmap(alert.appIcon ?: alert.icon, 24.dp, fallbackRes = R.drawable.rounded_lock_24) },
                    start = "${appNameFor(context, alert)} · ${context.getString(R.string.island_otp_label)}",
                    end = alert.otpCode?.let(::maskOtp).orEmpty(),
                    endSlot = { IslandIcon(R.drawable.rounded_lock_24, size = 18.dp) },
                )
            } else {
                LineContent(
                    icon = { IslandBitmap(icon, 24.dp, fallbackRes = R.drawable.rounded_notifications_unread_24) },
                    start = appNameFor(context, alert),
                    end = "",
                    condensed = condensedLine(),
                )
            },
            accent = alert.appColor?.let { Color(soften(it)) },
            dismissible = true,
            onDismiss = {
                NotificationListener.dismissNotification(alert.key)
                popCurrent(reExpand = false)
            },
            onOpen = open,
            interactions = InteractionOverrides(onTap = { open(); true }),
            sourcePackage = alert.packageName,
            contentId = alert.key,
        )
    }

    private fun otpItemFor(alert: ActiveNotificationAlert, code: String): IslandItem {
        val (sender, message) = senderAndMessage(context, alert)
        val showGlow = settings.isIslandShowGlowEnabled()
        val icon = alert.appIcon ?: alert.icon
        val copy = { copyCode(alert, code) }
        val dismiss = {
            NotificationListener.dismissNotification(alert.key)
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
                start = sender,
                end = formatOtp(code),
                endSlot = { IslandIcon(R.drawable.rounded_content_copy_24, size = 18.dp) },
            ),
            expanded = ExpandedContent { scope ->
                OtpExpanded(
                    alert = alert,
                    code = code,
                    sender = sender,
                    message = message,
                    showGlow = showGlow,
                    onCopy = copy,
                    onDismiss = dismiss,
                    scope = scope,
                )
            },
            accent = alert.appColor?.let { Color(soften(it)) },
            dismissible = true,
            onDismiss = dismiss,
            onOpen = {
                if (!sendPendingIntent(context, alert.contentIntent)) launchPackage(context, alert.packageName)
                popCurrent(reExpand = false)
            },
            interactions = if (settings.isIslandOtpTapToCopyEnabled()) {
                InteractionOverrides(onTap = { copy(); true }, onLineTap = { copy(); true })
            } else {
                InteractionOverrides.Default
            },
            sourcePackage = alert.packageName,
            contentId = alert.key,
        )
    }

    private fun copiedItem() = IslandItem(
        key = ITEM_KEY,
        priority = IslandPriority.NOTIFICATION,
        placement = CompactPlacement.Dynamic,
        compact = listOf(CompactCell("notif.copied") { IslandIcon(R.drawable.rounded_check_24, size = 20.dp) }),
        line = LineContent(
            icon = { IslandIcon(R.drawable.rounded_check_24, size = 22.dp) },
            start = context.getString(R.string.island_otp_copied),
            end = "",
        ),
        contentId = "otp.copied",
    )

    private fun copyCode(alert: ActiveNotificationAlert, code: String) {
        val c = ctx ?: return
        if (!OtpClipboard.copy(context, code)) return
        HapticUtil.performStrongTickHaptic(context)
        cancelTimers()
        copiedKey = alert.key
        render()
        c.request(PluginRequest.Collapse(ITEM_KEY))
        c.request(PluginRequest.Peek(ITEM_KEY, COPIED_MS))
        c.mainHandler.postDelayed(copiedRunnable, COPIED_MS)
    }

    private fun finishCopy() {
        val key = copiedKey ?: return
        copiedKey = null
        if (settings.isIslandOtpDismissAfterCopyEnabled()) NotificationListener.dismissNotification(key)
        val index = alerts.indexOfFirst { it.key == key }
        if (index >= 0) currentIndex = alerts.removeKeepingCurrent(index, currentIndex)
        if (alerts.isEmpty()) {
            clearAll()
            return
        }
        if (postedWhileCopied) {
            postedWhileCopied = false
            if (!queueEnabled()) while (alerts.size > 1) alerts.removeFirst()
            currentIndex = alerts.lastIndex
            manualPick = false
            scheduleTimeout()
            render()
            popUp()
            return
        }
        scheduleTimeout()
        render()
    }

    private fun stackIconFor(alert: ActiveNotificationAlert, current: Boolean, highlight: Boolean = false): StackIcon {
        val icon = if ((concealed() && hidesChatPictures()) || (hub() && !settings.isIslandNotifHubPreferChatPictures())) alert.appIcon ?: alert.icon else alert.chatIcon ?: alert.appIcon ?: alert.icon
        return StackIcon(alert.key, current = current, onSelect = { select(alert.key) }, highlight = highlight) { size ->
            IslandBitmap(icon, size, circle = true, fallbackRes = R.drawable.rounded_notifications_unread_24)
        }
    }

    private fun IslandItem.withStack(
        queue: QueueInfo?,
        stack: List<StackIcon>,
        sideBubble: SideBubble? = null,
        compactVisible: Boolean = sideBubble == null,
        leadingCell: CompactCell? = null,
        queued: List<StackIcon>? = null,
    ) = IslandItem(
        key = key,
        priority = priority,
        placement = placement,
        compact = if (leadingCell != null) listOf(leadingCell) + compact.take(1) else compact,
        line = line,
        expanded = expanded,
        accent = accent,
        dismissible = dismissible,
        onDismiss = onDismiss,
        onOpen = onOpen,
        interactions = interactions,
        queue = queue,
        sourcePackage = sourcePackage,
        contentId = contentId,
        stack = stack,
        queued = queued,
        compactVisible = compactVisible,
        sideBubble = sideBubble,
    )

    private fun sendReply(alert: ActiveNotificationAlert, action: NotificationActionItem, text: String) {
        val canCarryText = !action.remoteInputs.isNullOrEmpty()
        val sent = canCarryText && NotificationListener.instance?.performNotificationAction(action, text) == true
        if (sent) {
            Toast.makeText(context, R.string.island_reply_sent, Toast.LENGTH_SHORT).show()
            dismissAfterReply(alert)
        } else {
            dismissAfterReply(alert)
            context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
        }
    }

    private fun dismissAfterReply(alert: ActiveNotificationAlert) {
        val c = ctx ?: return
        replied[alert.key] = System.currentTimeMillis()
        val wasFocused = c.focusedKey() == ITEM_KEY
        onRemoved(alert.key)
        if (wasFocused) c.request(PluginRequest.Collapse(ITEM_KEY))
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
        private const val HUB_MAX_QUEUE = 200
        private const val AUTO_CELL_ICONS = 4
        private const val COPIED_MS = 1_500L
        private const val REPLY_REPOST_WINDOW_MS = 10_000L

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
