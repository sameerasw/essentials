package com.sameerasw.essentials.island.plugins.caffeinate

import com.sameerasw.essentials.island.ui.components.islandAccent

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.MainActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.controller.CaffeinateController
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.ConnectedTextLabel
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.RollingText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CaffeinatePlugin : BaseIslandPlugin() {
    override val id = "caffeinate"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_CAFFEINATE,
        SettingsRepository.KEY_ISLAND_CAFFEINATE_SHOW_TIME,
    )

    private var observer: Job? = null
    private var ticker: Job? = null

    override fun onStart() {
        observer = ctx?.scope?.launch {
            snapshotFlow {
                listOf(
                    CaffeinateController.isActive.value,
                    CaffeinateController.isStarting.value,
                    CaffeinateController.startingTimeLeft.value,
                    CaffeinateController.selectedTimeout.value,
                    CaffeinateController.endTime.value,
                )
            }.collect {
                render()
                restartTicker()
            }
        }
    }

    override fun onStop() {
        observer?.cancel()
        ticker?.cancel()
    }

    override fun refresh() = render()

    private fun restartTicker() {
        ticker?.cancel()
        if (!CaffeinateController.isActive.value || CaffeinateController.endTime.value <= 0L) return
        ticker = ctx?.scope?.launch {
            while (isActive) {
                val remaining = CaffeinateController.endTime.value - System.currentTimeMillis()
                delay((remaining % 1000L).let { if (it <= 0L) 1000L else it } + 2L)
                render()
            }
        }
    }

    private fun format(ms: Long): String {
        val total = (ms.coerceAtLeast(0L) + 999L) / 1000L
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun presetLabel(minutes: Int): String =
        if (minutes == -1) "" else if (minutes >= 60) "${minutes / 60}h" else "${minutes}m"

    private fun render() {
        if (ctx == null || !settings.isIslandShowCaffeinateEnabled()) {
            publish(null)
            return
        }
        val starting = CaffeinateController.isStarting.value && !CaffeinateController.isActive.value
        val active = CaffeinateController.isActive.value
        if (!starting && !active) {
            publish(null)
            return
        }
        val end = CaffeinateController.endTime.value
        val duration = CaffeinateController.durationMillis.value
        val remaining = if (end > 0L) end - System.currentTimeMillis() else 0L
        val value = when {
            starting -> presetLabel(CaffeinateController.selectedTimeout.value)
            end <= 0L -> ""
            else -> format(remaining)
        }
        val showTime = value.isNotEmpty() && settings.isIslandCaffeinateShowTimeEnabled()
        val subtitle = when {
            starting -> context.getString(R.string.caffeinate_starting_in, CaffeinateController.startingTimeLeft.value)
            end <= 0L -> context.getString(R.string.caffeinate_notification_desc)
            else -> context.getString(R.string.caffeinate_remaining, value)
        }
        val progress = if (!starting && end > 0L && duration > 0L) (remaining.toFloat() / duration).coerceIn(0f, 1f) else null
        val title = context.getString(R.string.feat_caffeinate_title)
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.CAFFEINATE,
                placement = CompactPlacement.Dynamic,
                compact = listOfNotNull(
                    CompactCell("caffeinate.icon") { IslandIcon(R.drawable.rounded_coffee_24, size = 18.dp, tint = islandAccent()) },
                    if (showTime) CompactCell("caffeinate.time") { RollingText(value) } else null,
                ),
                line = LineContent(
                    icon = { IslandIcon(R.drawable.rounded_coffee_24, tint = islandAccent()) },
                    start = title,
                    end = value,
                ),
                expanded = ExpandedContent { scope ->
                    CaffeinateExpanded(title, value, subtitle, progress, starting, scope)
                },
                onOpen = ::open,
            ),
        )
    }

    private fun open() {
        try {
            context.startActivity(
                com.sameerasw.essentials.utils.AppIconUtil.mainIntent(context)
                    .putExtra("feature", "Caffeinate")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun CaffeinateExpanded(
        title: String,
        value: String,
        subtitle: String,
        progress: Float?,
        starting: Boolean,
        scope: IslandExpandedScope,
    ) {
        val spec = scope.spec
        val sidePadding = spec.expandedPadding + spec.expandedCorner * 0.35f
        val accent = islandAccent()
        Box(propagateMinConstraints = true) {
            Column(Modifier.fillMaxWidth().padding(spec.expandedOutset)) {
                Spacer(Modifier.height(spec.expandedTopPadding))
                scope.CameraRow(
                    horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                    start = {
                        IslandIcon(R.drawable.rounded_coffee_24, size = 20.dp, tint = accent)
                        MarqueeText(text = title, style = IslandTextStyles.title, modifier = Modifier.weight(1f))
                    },
                    end = { if (value.isNotEmpty()) RollingText(value) },
                )
                Spacer(Modifier.height(4.dp))
                MarqueeText(
                    text = subtitle,
                    style = IslandTextStyles.body,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                )
                if (progress != null) {
                    Spacer(Modifier.height(16.dp))
                    LinearWavyProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = sidePadding),
                        color = accent,
                        trackColor = Color.White.copy(alpha = 0.2f),
                    )
                }
                Spacer(Modifier.height(16.dp))
                val items = buildList {
                    if (starting) {
                        add(ConnectedItem({ CaffeinateController.cycleTimeout(context) }) { ConnectedTextLabel(stringResource(R.string.caffeinate_timeout_presets_title)) })
                    }
                    add(
                        ConnectedItem({
                            CaffeinateController.cancelAll(context)
                            scope.collapse()
                        }) { ConnectedTextLabel(stringResource(R.string.action_stop)) },
                    )
                    add(
                        ConnectedItem({
                            open()
                            scope.collapse()
                        }) { ConnectedTextLabel(stringResource(R.string.island_progress_open)) },
                    )
                }
                ConnectedButtonRow(
                    height = 44.dp,
                    modifier = Modifier.padding(horizontal = sidePadding).padding(bottom = spec.expandedBottomPadding),
                    container = Color.White.copy(alpha = 0.2f),
                    items = items,
                )
            }
        }
    }

    companion object {
        const val ITEM_KEY = "caffeinate"
    }
}
