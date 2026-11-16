package com.sameerasw.essentials.island.ui

object IslandColorBridge {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var color: Int? = null
        private set

    @Volatile
    var listener: ((Int?) -> Unit)? = null

    fun publish(value: Int?) {
        if (!enabled || color == value) return
        color = value
        runCatching { listener?.invoke(value) }
    }
}
