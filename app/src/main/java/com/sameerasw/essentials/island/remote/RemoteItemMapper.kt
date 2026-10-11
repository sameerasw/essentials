package com.sameerasw.essentials.island.remote

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.InteractionOverrides
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.ui.IslandLayoutSpec
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.island.sdk.IslandAction
import com.sameerasw.island.sdk.IslandCellPayload
import com.sameerasw.island.sdk.IslandItemPayload
import com.sameerasw.island.sdk.IslandPlacement

interface RemoteItemHandler {
    val priorityGroup: String
    fun namespaced(rawKey: String): String
    fun invoke(action: IslandAction, itemKey: String, siblings: Set<String>): Boolean
    fun surfaceSession(rawKey: String): RemoteSurfaceSession?
    fun bindScope(rawKey: String, scope: IslandExpandedScope?)
    fun reportedHeightPx(rawKey: String): Int
    fun snapshot(rawKey: String): ImageBitmap?
    fun onLayout(spec: IslandLayoutSpec, density: Float, accent: Color)
}

object RemoteItemMapper {
    fun map(
        payload: IslandItemPayload,
        assets: Map<String, Bitmap>,
        siblings: Set<String>,
        handler: RemoteItemHandler,
    ): IslandItem? {
        if (payload.compact.isEmpty() || payload.compact.size > 2) return null
        val rawKey = payload.key
        val priority = payload.priority.coerceIn(IslandPriority.NOTIFICATION + 1, IslandPriority.DEFAULT)
        val override = payload.priorityOverride?.coerceIn(IslandPriority.NOTIFICATION + 1, IslandPriority.DEFAULT)
        val interactions = payload.interactions
        return IslandItem(
            key = handler.namespaced(rawKey),
            priority = priority,
            priorityOverride = override,
            placement = if (payload.placement == IslandPlacement.PINNED) CompactPlacement.Pinned else CompactPlacement.Dynamic,
            compact = payload.compact.map { cell -> cell(cell, assets) },
            line = payload.line?.let { line ->
                LineContent(
                    icon = { RemoteIcon(line.iconId?.let(assets::get), line.tone) },
                    start = line.start,
                    end = line.end,
                )
            },
            expanded = payload.expanded?.let { expanded ->
                ExpandedContent { scope ->
                    RemoteExpanded(
                        session = handler.surfaceSession(rawKey),
                        scope = scope,
                        heightDp = expanded.heightDp,
                        reportedHeightPx = handler.reportedHeightPx(rawKey),
                        snapshot = handler.snapshot(rawKey),
                        onScope = { handler.bindScope(rawKey, it) },
                        onLayout = handler::onLayout,
                    )
                }
            },
            accent = payload.accent?.let { Color(it) },
            dismissible = payload.dismissible,
            onOpen = interactions.onOpen?.let { action -> { handler.invoke(action, rawKey, siblings) } },
            interactions = InteractionOverrides(
                onTap = interactions.onTap?.let { action -> { handler.invoke(action, rawKey, siblings) } },
                onLongPress = interactions.onLongPress?.let { action -> { handler.invoke(action, rawKey, siblings) } },
                onExpandedTap = interactions.onExpandedTap?.let { action -> { handler.invoke(action, rawKey, siblings) } },
            ),
            compactVisible = payload.compactVisible,
            priorityGroup = handler.priorityGroup,
        )
    }

    private fun cell(cell: IslandCellPayload, assets: Map<String, Bitmap>): CompactCell =
        CompactCell(cell.key, cell.soloOnly) {
            val icon = cell.iconId?.let(assets::get)
            val text = cell.text
            if (icon != null && text != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteIcon(icon, cell.tone)
                    RollingText(text)
                }
            } else if (icon != null) {
                RemoteIcon(icon, cell.tone)
            } else if (text != null) {
                RollingText(text)
            }
        }
}
