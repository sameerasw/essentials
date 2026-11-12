package com.sameerasw.essentials.island.plugins.progress

import com.sameerasw.essentials.island.ui.components.rememberLoopValue
import com.sameerasw.essentials.island.ui.components.islandStill
import com.sameerasw.essentials.island.ui.components.islandAccent

import com.sameerasw.essentials.island.ui.motionTween

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ColorFilter
import com.sameerasw.essentials.utils.AppIconUtil
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.domain.model.ProgressNotificationData
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.model.SideBubble
import com.sameerasw.essentials.island.model.SideBubblePriority
import com.sameerasw.essentials.island.model.StackIcon
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandMotion
import com.sameerasw.essentials.services.NotificationListener

class ProgressPlugin : BaseIslandPlugin() {
    override val id = "progress"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_NOTIF_KEEP_PROGRESS,
        SettingsRepository.KEY_ISLAND_NOTIF_SKIP_SILENT,
        SettingsRepository.KEY_ISLAND_SECONDARY_BUBBLE,
        SettingsRepository.KEY_ISLAND_LIVE_UPDATES_COUNT,
        SettingsRepository.KEY_ISLAND_LIVE_UPDATES_EXPANDED,
    )

    private var data: ProgressNotificationData? = null
    private var icon: ImageBitmap? = null
    private var all: List<ProgressNotificationData> = emptyList()
    private val icons = HashMap<String, Pair<Bitmap?, ImageBitmap?>>()
    private val order = mutableListOf<String>()
    private val themed = HashMap<String, ImageBitmap?>()
    private var selectedKey: String? = null

    private val listener = object : NotificationListener.ProgressNotificationListener {
        override fun onProgressNotificationUpdated(data: ProgressNotificationData?) {}

        override fun onProgressNotificationsUpdated(all: List<ProgressNotificationData>) {
            ctx?.mainHandler?.post { update(all) }
        }
    }

    override fun onStart() {
        NotificationListener.addProgressNotificationListener(listener)
    }

    override fun onStop() {
        NotificationListener.removeProgressNotificationListener(listener)
        data = null
        icon = null
        all = emptyList()
        icons.clear()
        order.clear()
        themed.clear()
        selectedKey = null
    }

    override fun refresh() {
        ctx ?: return
        update(NotificationListener.getProgressNotifications())
    }

    private fun update(incoming: List<ProgressNotificationData>) {
        if (ctx == null) return
        val skipSilent = settings.isIslandNotifSkipSilentEnabled()
        val next = if (skipSilent) incoming.filterNot { it.isSilent && it.isIndeterminate } else incoming
        val keys = next.mapTo(HashSet()) { it.key }
        order.retainAll(keys)
        next.asReversed().forEach { if (it.key !in order) order.add(it.key) }
        val byKey = next.associateBy { it.key }
        all = order.mapNotNull { byKey[it] }
        icons.keys.retainAll(keys)
        themed.keys.retainAll(next.mapTo(HashSet()) { it.packageName })
        next.forEach { d ->
            if (d.packageName !in themed) {
                themed[d.packageName] = runCatching { AppIconUtil.monochromeAppIcon(context, d.packageName)?.asImageBitmap() }.getOrNull()
            }
            val cached = icons[d.key]
            val fresh = d.icon
            val same = cached != null && (cached.first === fresh || (fresh != null && cached.first?.sameAs(fresh) == true))
            if (!same) icons[d.key] = fresh to fresh?.asImageBitmap()
        }
        if (selectedKey !in keys) selectedKey = null
        data = all.firstOrNull { it.key == selectedKey } ?: all.firstOrNull()
        icon = data?.let { icons[it.key]?.second }
        render()
    }

    private fun bubblesMode(): Boolean = settings.isIslandSecondaryBubbleEnabled()

    private fun select(key: String) {
        selectedKey = key
        data = all.firstOrNull { it.key == key } ?: return
        icon = icons[key]?.second
        render()
        ctx?.request?.invoke(PluginRequest.Expand(ITEM_KEY))
    }

    private fun render() {
        val d = data
        if (ctx == null || d == null || !settings.isIslandNotifKeepProgressEnabled()) {
            publish(null)
            return
        }
        val image = icon
        val progress = d.progress / 100f
        val indeterminate = d.isIndeterminate
        val bubbles = bubblesMode()
        val expandedLead = settings.isIslandLiveUpdatesExpandedEnabled()
        val shown = all.take(settings.getIslandLiveUpdatesCount())
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.PROGRESS,
                expandedParticles = true,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("progress.ring") {
                        ProgressRing(progress, indeterminate, image, islandAccent(), tinted = d.iconIsGlyph)
                    },
                ),
                expanded = ExpandedContent { scope ->
                    ProgressExpanded(
                        icon = icons[d.key]?.first ?: d.icon,
                        iconIsGlyph = d.iconIsGlyph,
                        title = d.title ?: d.appName ?: d.packageName,
                        text = d.text,
                        progress = progress,
                        indeterminate = indeterminate,
                        accent = islandAccent(),
                        actions = d.actions,
                        onAction = { action ->
                            IslandHaptics.button(context)
                            NotificationListener.instance?.performNotificationAction(action)
                        },
                        onOpen = { open(d) },
                        scope = scope,
                    )
                },
                onOpen = { open(d) },
                sourcePackage = d.packageName,
                compactVisible = !bubbles,
                sideBubble = if (bubbles) {
                    SideBubble(
                        priority = SideBubblePriority.PROGRESS,
                        onOpen = { select(d.key) },
                        icons = shown.mapIndexed { index, entry ->
                            val glyph = if (entry.iconIsGlyph) icons[entry.key]?.second else themed[entry.packageName]
                            val ringIcon = glyph ?: icons[entry.key]?.second
                            val pill = expandedLead && index == 0
                            StackIcon(entry.key, onSelect = { select(entry.key) }, fill = true, widthFactor = if (pill) PILL_WIDTH else 1f) { size ->
                                if (pill) {
                                    ProgressPill(entry.progress / 100f, entry.isIndeterminate, ringIcon, pillLabel(entry), islandAccent(), size, tinted = glyph != null)
                                } else {
                                    ProgressRing(entry.progress / 100f, entry.isIndeterminate, ringIcon, islandAccent(), size, tinted = glyph != null)
                                }
                            }
                        },
                    )
                } else {
                    SideBubble(
                        priority = SideBubblePriority.PROGRESS,
                        whenOccupiedOnly = true,
                        onOpen = { ctx?.request?.invoke(PluginRequest.Expand(ITEM_KEY)) },
                        content = { bubbleSize ->
                            ProgressRing(progress, indeterminate, image, islandAccent(), bubbleSize, tinted = d.iconIsGlyph)
                        },
                    )
                },
            ),
        )
    }

    private fun open(d: ProgressNotificationData) {
        if (!sendPendingIntent(context, d.contentIntent)) launchPackage(context, d.packageName)
    }

    private fun pillLabel(entry: ProgressNotificationData): String =
        (entry.shortText ?: if (entry.isIndeterminate) "" else "${entry.progress.toInt()}%").take(PILL_MAX_CHARS)

    companion object {
        const val ITEM_KEY = "progress"
        private const val PILL_WIDTH = 2.2f
        private const val PILL_MAX_CHARS = 7
    }
}

@Composable
private fun ProgressPill(progress: Float, indeterminate: Boolean, icon: ImageBitmap?, label: String, color: Color, height: Dp, tinted: Boolean = false) {
    val fraction by animateFloatAsState(progress.coerceIn(0f, 1f), IslandMotion.float(), label = "pillProgress")
    val phase = rememberLoopValue(indeterminate && !islandStill(), 1f, 1600)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.height * 0.1f
            val inset = stroke / 2f
            val w = this.size.width - stroke
            val h = this.size.height - stroke
            val r = h / 2f
            val path = Path().apply {
                moveTo(inset + w / 2f, inset)
                lineTo(inset + w - r, inset)
                arcTo(Rect(inset + w - 2 * r, inset, inset + w, inset + h), -90f, 180f, false)
                lineTo(inset + r, inset + h)
                arcTo(Rect(inset, inset, inset + 2 * r, inset + h), 90f, 180f, false)
                close()
            }
            drawPath(path, color.copy(alpha = 0.25f), style = Stroke(stroke))
            val measure = PathMeasure().apply { setPath(path, false) }
            val length = measure.length
            val segment = Path()
            if (indeterminate) {
                val start = phase * length
                val end = start + length * 0.25f
                measure.getSegment(start, minOf(end, length), segment, true)
                if (end > length) measure.getSegment(0f, end - length, segment, true)
            } else {
                measure.getSegment(0f, length * fraction, segment, true)
            }
            drawPath(segment, color, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = height * 0.19f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(height * 0.12f),
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    contentScale = if (tinted) ContentScale.Fit else ContentScale.Crop,
                    colorFilter = if (tinted) ColorFilter.tint(color) else null,
                    modifier = Modifier.size(height * if (tinted) 0.8f else 0.62f).clip(CircleShape),
                )
            }
            AnimatedContent(
                targetState = if (label.endsWith("%")) "%" else label,
                transitionSpec = { fadeIn(IslandMotion.expressiveFastEffects()) togetherWith fadeOut(IslandMotion.expressiveFastEffects()) },
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f),
                label = "pillLabel",
            ) { kind ->
                val shownLabel = if (kind == "%") label else kind
                Text(
                    text = shownLabel,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    fontWeight = FontWeight.Bold,
                    fontSize = with(LocalDensity.current) { (height * 0.36f).toSp() },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun ProgressRing(progress: Float, indeterminate: Boolean, icon: ImageBitmap?, color: Color, size: Dp = 24.dp, tinted: Boolean = false) {
    val sweep by animateFloatAsState(360f * progress.coerceIn(0f, 1f), IslandMotion.float(), label = "progressSweep")
    val rotation = rememberLoopValue(indeterminate && !islandStill(), 360f, 1200)
    Box(
        Modifier
            .sizeIn(maxWidth = size, maxHeight = size)
            .aspectRatio(1f, matchHeightConstraintsFirst = true),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val diameter = this.size.minDimension
            val stroke = diameter * 0.1f
            val topLeft = Offset((this.size.width - diameter + stroke) / 2f, (this.size.height - diameter + stroke) / 2f)
            val arcSize = Size(diameter - stroke, diameter - stroke)
            drawArc(color.copy(alpha = 0.25f), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (indeterminate) {
                drawArc(color, rotation - 90f, 90f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            } else {
                drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = if (tinted) ContentScale.Fit else ContentScale.Crop,
                colorFilter = if (tinted) ColorFilter.tint(color) else null,
                modifier = Modifier.fillMaxSize(if (tinted) 0.8f else 0.62f).aspectRatio(1f).clip(CircleShape),
            )
        }
    }
}
