package com.sameerasw.essentials.island.model

object IslandPriorityEntries {
    class Entry(val id: String, val pluginIds: Set<String>, val defaultPriority: Int)

    val all = listOf(
        Entry("battery", setOf("battery"), IslandPriority.BATTERY_ALERT),
        Entry("notifications", setOf("notifications"), IslandPriority.NOTIFICATION),
        Entry("flashlight", setOf("flashlight"), IslandPriority.FLASHLIGHT),
        Entry("navigation", setOf("navigation"), IslandPriority.NAVIGATION),
        Entry("timer", setOf("timer"), IslandPriority.TIMER),
        Entry("media", setOf("media"), IslandPriority.MEDIA),
        Entry("conscious_gate", setOf("conscious_gate"), IslandPriority.CONSCIOUS_GATE),
        Entry("caffeinate", setOf("caffeinate"), IslandPriority.CAFFEINATE),
        Entry("travel", setOf("travel"), IslandPriority.TRAVEL),
        Entry("calendar", setOf("calendar"), IslandPriority.CALENDAR),
        Entry("sound_mode", setOf("sound_mode"), IslandPriority.SOUND_MODE),
        Entry("network", setOf("network", "signal"), IslandPriority.SIGNAL),
        Entry("hotspot", setOf("hotspot"), IslandPriority.HOTSPOT),
        Entry("devices", setOf("devices"), IslandPriority.DEVICES),
        Entry("alarm", setOf("alarm"), IslandPriority.ALARM),
    ).sortedBy { it.defaultPriority }

    const val PROVIDER_PREFIX = "provider:"

    fun providerId(packageName: String) = PROVIDER_PREFIX + packageName
    fun isProvider(id: String) = id.startsWith(PROVIDER_PREFIX)
    fun providerPackage(id: String) = id.removePrefix(PROVIDER_PREFIX)
    fun providerEntry(packageName: String, priority: Int) = Entry(providerId(packageName), setOf(providerId(packageName)), priority)

    fun entries(extra: List<Entry> = emptyList()): List<Entry> = (all + extra).sortedBy { it.defaultPriority }

    fun defaultOrder(extra: List<Entry> = emptyList()): List<String> = entries(extra).map { it.id }

    val defaultOrder: List<String> = defaultOrder()

    fun normalize(saved: List<String>, extra: List<Entry> = emptyList()): List<String> {
        val order = defaultOrder(extra)
        val known = saved.filter { it in order }.distinct()
        return known + order.filter { it !in known }
    }

    fun resolve(saved: List<String>?, extra: List<Entry> = emptyList()): Map<String, Int> {
        if (saved.isNullOrEmpty()) return emptyMap()
        val entries = entries(extra)
        val slots = entries.map { it.defaultPriority }
        val byId = entries.associateBy { it.id }
        val result = HashMap<String, Int>()
        normalize(saved, extra).forEachIndexed { index, id ->
            byId.getValue(id).pluginIds.forEach { result[it] = slots[index] }
        }
        return result
    }
}
