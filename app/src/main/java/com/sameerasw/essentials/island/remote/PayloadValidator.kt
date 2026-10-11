package com.sameerasw.essentials.island.remote

import android.graphics.Bitmap
import com.sameerasw.island.sdk.BriefCardPayload
import com.sameerasw.island.sdk.DecodedPayload
import com.sameerasw.island.sdk.IslandAction
import com.sameerasw.island.sdk.IslandCellPayload
import com.sameerasw.island.sdk.IslandExpandedPayload
import com.sameerasw.island.sdk.IslandInteractionsPayload
import com.sameerasw.island.sdk.IslandItemPayload
import com.sameerasw.island.sdk.IslandLinePayload
import com.sameerasw.island.sdk.IslandPayload

object PayloadValidator {
    private const val MAX_ITEMS = 6
    private const val MAX_CARDS = 4
    private const val MAX_ASSETS = 48
    private const val MAX_ICON_PX = 256
    private const val MAX_ASSET_BYTES = 4 * 1024 * 1024
    private const val MAX_KEY = 64
    private const val MAX_CELL_TEXT = 32
    private const val MAX_LINE_TEXT = 120
    private const val MIN_HEIGHT_DP = 80
    private const val MAX_HEIGHT_DP = 640

    fun sanitize(decoded: DecodedPayload): DecodedPayload {
        val items = decoded.payload.items
            .filter { it.key.isNotBlank() }
            .distinctBy { it.key.take(MAX_KEY) }
            .take(MAX_ITEMS)
            .mapNotNull(::item)
        val cards = decoded.payload.briefCards
            .filter { it.key.isNotBlank() }
            .distinctBy { it.key.take(MAX_KEY) }
            .take(MAX_CARDS)
            .map(::card)
        return DecodedPayload(IslandPayload(decoded.payload.schema, items, cards), assets(decoded.assets))
    }

    private fun assets(source: Map<String, Bitmap>): Map<String, Bitmap> {
        var total = 0
        val result = LinkedHashMap<String, Bitmap>()
        for ((id, bitmap) in source) {
            if (result.size >= MAX_ASSETS) break
            if (bitmap.width > MAX_ICON_PX || bitmap.height > MAX_ICON_PX) continue
            total += bitmap.byteCount
            if (total > MAX_ASSET_BYTES) break
            result[id.take(MAX_KEY)] = bitmap
        }
        return result
    }

    private fun item(item: IslandItemPayload): IslandItemPayload? {
        if (item.compact.isEmpty()) return null
        return item.copy(
            key = item.key.take(MAX_KEY),
            compact = item.compact.take(2).map(::cell),
            line = item.line?.let(::line),
            expanded = item.expanded?.let { IslandExpandedPayload(it.heightDp.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP), it.scrollable) },
            interactions = IslandInteractionsPayload(
                onTap = item.interactions.onTap?.let(::action),
                onLongPress = item.interactions.onLongPress?.let(::action),
                onExpandedTap = item.interactions.onExpandedTap?.let(::action),
                onOpen = item.interactions.onOpen?.let(::action),
            ),
        )
    }

    private fun cell(cell: IslandCellPayload) = cell.copy(
        key = cell.key.take(MAX_KEY),
        iconId = cell.iconId?.take(MAX_KEY),
        text = cell.text?.take(MAX_CELL_TEXT),
        tone = tone(cell.tone),
    )

    private fun line(line: IslandLinePayload) = line.copy(
        start = line.start.take(MAX_LINE_TEXT),
        end = line.end.take(MAX_LINE_TEXT),
        iconId = line.iconId?.take(MAX_KEY),
        tone = tone(line.tone),
    )

    private fun card(card: BriefCardPayload) = card.copy(
        key = card.key.take(MAX_KEY),
        title = card.title.take(MAX_CELL_TEXT),
        subtitle = card.subtitle?.take(MAX_LINE_TEXT),
        iconId = card.iconId?.take(MAX_KEY),
        tone = tone(card.tone),
        trailingIconId = card.trailingIconId?.take(MAX_KEY),
        trailingText = card.trailingText?.take(16),
        trailingTone = tone(card.trailingTone),
        pageHeightDp = card.pageHeightDp?.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP),
    )

    private fun action(action: IslandAction) = action.copy(target = action.target?.take(MAX_KEY))

    private fun tone(tone: String): String = if (tone in TONES) tone else "none"

    private val TONES = setOf("accent", "alert", "none")
}
