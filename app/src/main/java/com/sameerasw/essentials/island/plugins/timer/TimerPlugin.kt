package com.sameerasw.essentials.island.plugins.timer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.utils.chronometer.ChronometerEntry
import com.sameerasw.essentials.utils.chronometer.ChronometerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class TimerPlugin : BaseIslandPlugin() {
    override val id = "timer"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_TIMERS)

    private var entries: List<ChronometerEntry> = emptyList()
    private val seen = HashSet<String>()
    private var observer: Job? = null
    private var ticker: Job? = null

    override fun onStart() {
        val c = ctx!!
        observer = c.scope.launch {
            ChronometerRepository.entries.collect { next ->
                entries = next
                val fresh = next.firstOrNull { it.key !in seen && it.running }
                seen.retainAll(next.map { it.key }.toSet())
                next.forEach { seen += it.key }
                render()
                if (fresh != null && settings.isIslandShowTimersEnabled()) {
                    c.request(PluginRequest.Peek(ITEM_KEY, settings.getIslandPeekDurationMs()))
                }
                restartTicker()
            }
        }
    }

    override fun onStop() {
        observer?.cancel()
        ticker?.cancel()
        entries = emptyList()
    }

    override fun refresh() = render()

    private fun restartTicker() {
        ticker?.cancel()
        if (entries.none { it.running }) return
        ticker = ctx?.scope?.launch {
            while (isActive) {
                delay(msUntilNextTick())
                render()
            }
        }
    }

    private fun msUntilNextTick(): Long {
        val entry = primary()?.takeIf { it.running } ?: return 1000L
        val ms = entry.displayMillis()
        val untilChange = if (entry.countDown) ms % 1000L else 1000L - ms % 1000L
        return (if (untilChange == 0L) 1000L else untilChange) + 2L
    }

    private fun primary(): ChronometerEntry? {
        val now = System.currentTimeMillis()
        return entries.filter { it.running && it.countDown }.minByOrNull { it.displayMillis(now) }
            ?: entries.filter { it.running }.maxByOrNull { it.postedAt }
            ?: entries.maxByOrNull { it.postedAt }
    }

    private fun render() {
        val entry = primary()
        if (ctx == null || entry == null || !settings.isIslandShowTimersEnabled()) {
            publish(null)
            return
        }
        val time = format(entry.displayMillis())
        val icon = if (entry.countDown) R.drawable.rounded_timer_24 else R.drawable.rounded_av_timer_24
        val label = entry.title ?: context.getString(if (entry.countDown) R.string.island_timer_default else R.string.island_stopwatch_default)
        val more = entries.size - 1
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.TIMER,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("timer.icon") {
                        IslandIcon(icon, size = 18.dp, tint = if (entry.running) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.6f))
                    },
                    CompactCell("timer.time") { RollingText(time) },
                ),
                line = LineContent(
                    icon = { IslandIcon(icon, tint = MaterialTheme.colorScheme.primary) },
                    start = label,
                    end = time,
                ),
                expanded = ExpandedContent { scope ->
                    val spec = scope.spec
                    val side = spec.expandedPadding + spec.expandedCorner * 0.35f
                    Box(propagateMinConstraints = true) {
                        Column(Modifier.padding(spec.expandedOutset)) {
                            Spacer(Modifier.height(spec.expandedTopPadding))
                            scope.CameraRow(
                                horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                                start = {
                                    Box(Modifier.padding(horizontal = 4.dp)) { IslandIcon(icon, size = 20.dp, tint = MaterialTheme.colorScheme.primary) }
                                    MarqueeText(
                                        text = if (more > 0) "$label  +$more" else label,
                                        style = IslandTextStyles.title,
                                        modifier = Modifier.weight(1f),
                                    )
                                },
                                end = { entry.appName?.let { MarqueeText(it, IslandTextStyles.body, Modifier.weight(1f, fill = false)) } },
                            )
                            RollingText(
                                text = time,
                                style = IslandTextStyles.compact.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = side),
                            )
                            if (entry.actions.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                ConnectedButtonRow(
                                    height = 36.dp,
                                    modifier = Modifier.padding(horizontal = side),
                                    container = Color.White.copy(alpha = 0.2f),
                                    items = entry.actions.take(3).map { action ->
                                        ConnectedItem({ sendPendingIntent(context, action.intent) }) { ConnectedTextLabel(action.title) }
                                    },
                                )
                            }
                            Spacer(Modifier.height(spec.expandedPadding * 0.7f))
                        }
                    }
                },
                onOpen = { if (!sendPendingIntent(context, entry.contentIntent)) launchPackage(context, entry.packageName) },
            ),
        )
    }

    private fun format(millis: Long): String {
        val total = millis / 1000L
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    companion object {
        const val ITEM_KEY = "timer"
    }
}
