package com.sameerasw.essentials.island.model

object IslandPriorityEntries {
    class Entry(val id: String, val pluginIds: Set<String>, val defaultPriority: Int)

    val all = listOf(
        Entry("notifications", setOf("notifications"), IslandPriority.NOTIFICATION),
        Entry("flashlight", setOf("flashlight"), IslandPriority.FLASHLIGHT),
        Entry("timer", setOf("timer"), IslandPriority.TIMER),
        Entry("media", setOf("media"), IslandPriority.MEDIA),
        Entry("conscious_gate", setOf("conscious_gate"), IslandPriority.CONSCIOUS_GATE),
        Entry("caffeinate", setOf("caffeinate"), IslandPriority.CAFFEINATE),
        Entry("travel", setOf("travel"), IslandPriority.TRAVEL),
        Entry("calendar", setOf("calendar"), IslandPriority.CALENDAR),
        Entry("sound_mode", setOf("sound_mode"), IslandPriority.SOUND_MODE),
        Entry("network", setOf("network", "signal"), IslandPriority.SIGNAL),
        Entry("weather", setOf("weather"), IslandPriority.WEATHER),
        Entry("devices", setOf("devices"), IslandPriority.DEVICES),
        Entry("alarm", setOf("alarm"), IslandPriority.ALARM),
    ).sortedBy { it.defaultPriority }

    val defaultOrder: List<String> = all.map { it.id }

    fun normalize(saved: List<String>): List<String> {
        val known = saved.filter { it in defaultOrder }.distinct()
        return known + defaultOrder.filter { it !in known }
    }

    fun resolve(saved: List<String>?): Map<String, Int> {
        if (saved.isNullOrEmpty()) return emptyMap()
        val slots = all.map { it.defaultPriority }
        val byId = all.associateBy { it.id }
        val result = HashMap<String, Int>()
        normalize(saved).forEachIndexed { index, id ->
            byId.getValue(id).pluginIds.forEach { result[it] = slots[index] }
        }
        return result
    }
}
