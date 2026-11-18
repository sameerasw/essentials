package com.sameerasw.essentials.island.remote

import com.sameerasw.island.sdk.IslandOptionPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

object RemoteProviderOptions {
    private val setters = HashMap<String, (String, Boolean) -> Unit>()
    private val state = MutableStateFlow<Map<String, List<IslandOptionPayload>>>(emptyMap())

    val options: StateFlow<Map<String, List<IslandOptionPayload>>> = state

    fun set(packageName: String, options: List<IslandOptionPayload>, setter: (String, Boolean) -> Unit) {
        if (options.isEmpty()) {
            clear(packageName)
            return
        }
        synchronized(setters) { setters[packageName] = setter }
        state.update { it + (packageName to options) }
    }

    fun clear(packageName: String) {
        synchronized(setters) { setters.remove(packageName) }
        state.update { it - packageName }
    }

    fun has(packageName: String): Boolean = state.value[packageName]?.isNotEmpty() == true

    fun change(packageName: String, key: String, checked: Boolean) {
        val setter = synchronized(setters) { setters[packageName] } ?: return
        state.update { map ->
            val current = map[packageName] ?: return@update map
            map + (packageName to current.map { if (it.key == key) it.copy(checked = checked) else it })
        }
        setter(key, checked)
    }
}
