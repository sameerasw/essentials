package com.sameerasw.essentials.domain.diy

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KClass

object DIYRepository {
    private const val PREFS_NAME = "diy_automations_prefs"
    private const val KEY_AUTOMATIONS = "saved_automations"

    private val _automations = MutableStateFlow<List<Automation>>(emptyList())
    val automations = _automations.asStateFlow()

    private var prefs: SharedPreferences? = null
    private val gson = GsonBuilder()
        .registerTypeAdapter(Trigger::class.java, SealedAdapter(Trigger::class))
        .registerTypeAdapter(State::class.java, SealedAdapter(State::class))
        .registerTypeAdapter(Action::class.java, SealedAdapter(Action::class))
        .create()

    private class SealedAdapter<T : Any>(private val kClass: KClass<T>) : JsonSerializer<T>,
        JsonDeserializer<T> {
        override fun serialize(
            src: T,
            typeOfSrc: java.lang.reflect.Type,
            context: JsonSerializationContext
        ): JsonElement {
            val element = context.serialize(src)
            if (element.isJsonObject) {
                element.asJsonObject.addProperty("type", src::class.simpleName)
            }
            return element
        }

        override fun deserialize(
            json: JsonElement,
            typeOfT: java.lang.reflect.Type,
            context: JsonDeserializationContext
        ): T? {
            val typeName = json.asJsonObject.get("type").asString
            val subClass = kClass.sealedSubclasses.firstOrNull { it.simpleName == typeName }

            return if (subClass != null) {
                // Determine if it's an object or class
                if (subClass.objectInstance != null) {
                    subClass.objectInstance
                } else {
                    context.deserialize(json, subClass.java)
                }
            } else {
                null
            }
        }
    }

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        reloadAutomations()
    }

    fun reloadAutomations() {
        val json = prefs?.getString(KEY_AUTOMATIONS, null)
        val loadedList: List<Automation> = if (json != null) {
            try {
                val type = object : TypeToken<List<Automation>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        _automations.value = loadedList
    }

    private fun saveToPrefs() {
        val json = gson.toJson(_automations.value)
        prefs?.edit()?.putString(KEY_AUTOMATIONS, json)?.apply()
    }

    fun addAutomation(automation: Automation) {
        val current = _automations.value.toMutableList()
        current.add(automation)
        _automations.value = current
        saveToPrefs()
    }

    fun updateAutomation(automation: Automation) {
        val current = _automations.value.toMutableList()
        val index = current.indexOfFirst { it.id == automation.id }
        if (index != -1) {
            current[index] = automation
            _automations.value = current
            saveToPrefs()
        }
    }

    fun removeAutomation(id: String) {
        val current = _automations.value.toMutableList()
        current.removeAll { it.id == id }
        _automations.value = current
        saveToPrefs()
    }

    fun getAutomation(id: String): Automation? {
        return _automations.value.find { it.id == id }
    }
}
