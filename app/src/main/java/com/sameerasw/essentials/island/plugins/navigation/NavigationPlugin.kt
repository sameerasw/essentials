package com.sameerasw.essentials.island.plugins.navigation

import com.sameerasw.essentials.island.ui.components.islandAccent

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.NavigationNotificationData
import com.sameerasw.essentials.domain.model.NavigationParser
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPluginContext
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.IslandStage
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.services.NotificationListener

class NavigationPlugin : BaseIslandPlugin() {
    override val id = "navigation"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_NAVIGATION,
        SettingsRepository.KEY_ISLAND_NAVIGATION_ON_NEW,
        SettingsRepository.KEY_ISLAND_NAVIGATION_PEEK_AFTER_EXPAND,
        SettingsRepository.KEY_ISLAND_NAVIGATION_EXPAND_DISTANCE,
    )

    private var data: NavigationNotificationData? = null

    private val trigger = NavigationTrigger()

    private val endRoute = Runnable { trigger.reset() }

    private val endExpand = Runnable {
        val c = ctx ?: return@Runnable
        if (c.focusedKey() != ITEM_KEY || c.currentStage() != IslandStage.Expanded) return@Runnable
        c.request(PluginRequest.Collapse(ITEM_KEY))
        if (settings.isIslandNavigationPeekAfterExpandEnabled() && settings.isIslandLineStageEnabled()) {
            c.mainHandler.postDelayed(peekAfterCollapse, COLLAPSE_ANIM_MS)
        }
    }

    private val peekAfterCollapse = Runnable {
        ctx?.request?.invoke(PluginRequest.Peek(ITEM_KEY, settings.getIslandPeekDurationMs()))
    }

    private val listener = object : NotificationListener.NavigationNotificationListener {
        override fun onNavigationNotificationUpdated(data: NavigationNotificationData?) {
            ctx?.mainHandler?.post { update(data) }
        }
    }

    override fun onStart() {
        NotificationListener.addNavigationNotificationListener(listener)
    }

    override fun onStop() {
        NotificationListener.removeNavigationNotificationListener(listener)
        ctx?.mainHandler?.removeCallbacks(endExpand)
        ctx?.mainHandler?.removeCallbacks(peekAfterCollapse)
        ctx?.mainHandler?.removeCallbacks(endRoute)
        data = null
        endRoute.run()
    }

    override fun refresh() {
        ctx ?: return
        data = stabilized(NotificationListener.getLatestNavigationNotification())
        render()
    }

    private fun update(next: NavigationNotificationData?) {
        val c = ctx ?: return
        val stable = stabilized(next)
        val turned = stable?.instruction != null && stable.instruction != data?.instruction
        data = stable
        render()
        if (stable == null) {
            c.mainHandler.removeCallbacks(endExpand)
            c.mainHandler.removeCallbacks(peekAfterCollapse)
            c.mainHandler.removeCallbacks(endRoute)
            c.mainHandler.postDelayed(endRoute, ROUTE_GRACE_MS)
            return
        }
        c.mainHandler.removeCallbacks(endRoute)
        if (!enabled()) return
        val focused = c.focusedKey()
        val action = trigger.decide(
            instruction = stable.instruction.orEmpty(),
            turned = turned,
            meters = NavigationParser.meters(stable.distance),
            threshold = settings.getIslandNavigationExpandDistance(),
            onNew = settings.getIslandNavigationOnNew(),
            canPeek = settings.isIslandLineStageEnabled(),
            canExpand = focused == null || focused == ITEM_KEY,
            now = System.currentTimeMillis(),
        )
        when (action) {
            NavigationTrigger.Action.Expand -> expand(c)
            NavigationTrigger.Action.Peek -> c.request(PluginRequest.Peek(ITEM_KEY, settings.getIslandPeekDurationMs()))
            NavigationTrigger.Action.None -> {}
        }
    }

    private fun stabilized(next: NavigationNotificationData?): NavigationNotificationData? {
        val previous = data?.maneuver ?: return next
        val incoming = next?.maneuver ?: return next
        return if (previous !== incoming && previous.sameAs(incoming)) next.copy(maneuver = previous) else next
    }

    private fun expand(c: IslandPluginContext) {
        if (c.focusedKey() == ITEM_KEY && c.currentStage() == IslandStage.Expanded) return
        c.mainHandler.removeCallbacks(peekAfterCollapse)
        c.request(PluginRequest.Expand(ITEM_KEY))
        c.mainHandler.removeCallbacks(endExpand)
        c.mainHandler.postDelayed(endExpand, EXPANDED_MS)
    }

    private fun enabled() = settings.isIslandShowNavigationEnabled()

    private fun render() {
        val d = data
        if (ctx == null || d == null || !enabled()) {
            publish(null)
            return
        }
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.NAVIGATION,
                placement = CompactPlacement.Dynamic,
                compact = listOfNotNull(
                    d.distance?.let { CompactCell("navigation.distance") { RollingText(it) } },
                    CompactCell("navigation.maneuver") { Maneuver(d, 18) },
                ),
                line = LineContent(
                    icon = { Maneuver(d, 20) },
                    start = d.instruction.orEmpty(),
                    end = d.distance.orEmpty(),
                ),
                expanded = ExpandedContent { scope ->
                    NavigationExpanded(
                        data = d,
                        accent = islandAccent(),
                        onOpen = { open(d) },
                        onExit = d.exitIntent?.let { intent ->
                            {
                                sendPendingIntent(
                                    context,
                                    intent
                                )
                            }
                        },
                        scope = scope,
                    )
                },
                onOpen = { open(d) },
                sourcePackage = d.packageName,
            ),
        )
    }

    private fun open(d: NavigationNotificationData) {
        if (!sendPendingIntent(context, d.contentIntent)) launchPackage(context, d.packageName)
    }

    companion object {
        const val ITEM_KEY = "navigation"

        private const val EXPANDED_MS = 6_000L
        private const val COLLAPSE_ANIM_MS = 600L
        private const val ROUTE_GRACE_MS = 60_000L
    }
}

@Composable
internal fun Maneuver(d: NavigationNotificationData, size: Int) {
    if (d.maneuver != null) {
        IslandBitmap(d.maneuver, size.dp, fallbackRes = R.drawable.round_navigation_24)
    } else {
        IslandIcon(
            R.drawable.round_navigation_24,
            size = size.dp,
            tint = islandAccent()
        )
    }
}

internal class NavigationTrigger {
    enum class Action { None, Expand, Peek }

    private val expandedFor = HashSet<String>()
    private val surfaced = LinkedHashSet<String>()
    private var lastSurfacedAt = 0L

    fun reset() {
        expandedFor.clear()
        surfaced.clear()
        lastSurfacedAt = 0L
    }

    fun decide(
        instruction: String,
        turned: Boolean,
        meters: Double?,
        threshold: Int,
        onNew: String,
        canPeek: Boolean,
        canExpand: Boolean,
        now: Long,
    ): Action {
        if (meters != null && meters > threshold * REARM_FACTOR) {
            expandedFor.remove(instruction)
            surfaced.remove(instruction)
        }
        val approaching = threshold > 0 && meters != null && meters <= threshold && instruction !in expandedFor
        val expandSoon = threshold > 0 && meters != null && meters <= threshold * REARM_FACTOR
        val announce = turned && instruction !in surfaced && !expandSoon && now - lastSurfacedAt >= COOLDOWN_MS
        val action = when {
            approaching && canExpand -> {
                expandedFor += instruction
                Action.Expand
            }
            !announce -> Action.None
            onNew == SettingsRepository.ISLAND_NAVIGATION_ON_NEW_EXPAND && canExpand -> Action.Expand
            onNew == SettingsRepository.ISLAND_NAVIGATION_ON_NEW_PEEK && canPeek -> Action.Peek
            else -> Action.None
        }
        if (action != Action.None) {
            surfaced += instruction
            if (surfaced.size > MAX_SURFACED) surfaced.remove(surfaced.first())
            lastSurfacedAt = now
        }
        return action
    }

    companion object {
        const val REARM_FACTOR = 1.5
        const val COOLDOWN_MS = 30_000L
        private const val MAX_SURFACED = 50
    }
}
