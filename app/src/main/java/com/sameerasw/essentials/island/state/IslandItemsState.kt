package com.sameerasw.essentials.island.state

import com.sameerasw.essentials.island.model.IslandItem
import kotlinx.coroutines.flow.MutableStateFlow

enum class IslandPresence { Shown, Hidden }

class IslandItemEntry(val item: IslandItem, val shown: Boolean)

object IslandItemsState {
    val entries = MutableStateFlow<List<IslandItemEntry>>(emptyList())

    val presence = MutableStateFlow<Map<String, IslandPresence>>(emptyMap())

    fun publishPresence(itemsBySource: Map<String, List<IslandItem>>, shownKeys: Set<String>) {
        val result = HashMap<String, IslandPresence>()
        itemsBySource.forEach { (source, items) ->
            items.forEach { item ->
                val group = item.priorityGroup ?: source
                if (item.key in shownKeys) result[group] = IslandPresence.Shown
                else if (group !in result) result[group] = IslandPresence.Hidden
            }
        }
        if (presence.value != result) presence.value = result
    }

    fun publish(items: Map<String, IslandItem>, shownKeys: Set<String>) {
        val candidates = items.values.filter { it.key !in EXCLUDED && (it.compactVisible || it.line != null) }
        val shown = candidates.filter { it.key in shownKeys }.sortedBy { it.effectivePriority }
        val hidden = candidates.filter { it.key !in shownKeys }.sortedBy { it.effectivePriority }
        entries.value = shown.map { IslandItemEntry(it, true) } + hidden.map { IslandItemEntry(it, false) }
    }

    private val EXCLUDED = setOf("brief", "time", "battery", "alarm", "media", "notifications")
}
