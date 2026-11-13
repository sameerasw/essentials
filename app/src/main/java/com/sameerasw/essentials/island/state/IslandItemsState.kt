package com.sameerasw.essentials.island.state

import com.sameerasw.essentials.island.model.IslandItem
import kotlinx.coroutines.flow.MutableStateFlow

class IslandItemEntry(val item: IslandItem, val shown: Boolean)

object IslandItemsState {
    val entries = MutableStateFlow<List<IslandItemEntry>>(emptyList())

    fun publish(items: Map<String, IslandItem>, shownKeys: Set<String>) {
        val candidates = items.values.filter { it.key !in EXCLUDED && (it.compactVisible || it.line != null) }
        val shown = candidates.filter { it.key in shownKeys }.sortedBy { it.effectivePriority }
        val hidden = candidates.filter { it.key !in shownKeys }.sortedBy { it.effectivePriority }
        entries.value = shown.map { IslandItemEntry(it, true) } + hidden.map { IslandItemEntry(it, false) }
    }

    private val EXCLUDED = setOf("brief", "time", "battery", "alarm", "media", "notifications")
}
