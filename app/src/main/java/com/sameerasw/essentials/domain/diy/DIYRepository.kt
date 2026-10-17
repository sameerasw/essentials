/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: DIYRepository.kt
 * Description: Domain model and business logic entry for DIYRepository.kt.
 */

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
    private const val KEY_MACROS = "saved_macros"

    private val _automations = MutableStateFlow<List<Automation>>(emptyList())
    val automations = _automations.asStateFlow()

    private val _macros = MutableStateFlow<List<Macro>>(emptyList())
    val macros = _macros.asStateFlow()

    private var prefs: SharedPreferences? = null
    private val gson =
        GsonBuilder()
            .registerTypeAdapter(Trigger::class.java, SealedAdapter(Trigger::class))
            .registerTypeAdapter(State::class.java, SealedAdapter(State::class))
            .registerTypeAdapter(Action::class.java, SealedAdapter(Action::class))
            .create()

    private class SealedAdapter<T : Any>(
        private val kClass: KClass<T>,
    ) : JsonSerializer<T>,
        JsonDeserializer<T> {
        override fun serialize(
            src: T,
            typeOfSrc: java.lang.reflect.Type,
            context: JsonSerializationContext,
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
            context: JsonDeserializationContext,
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

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        reloadAutomations()
        reloadMacros()
    }

    fun reloadAutomations() {
        val json = prefs?.getString(KEY_AUTOMATIONS, null)
        val loadedList: List<Automation> =
            if (json != null) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<Automation>>() {}.type
                    gson.fromJson(json, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        _automations.value = loadedList
        updateActionShortcutLauncherState()
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
        updateActionShortcutLauncherState()
    }

    fun updateAutomation(automation: Automation) {
        val current = _automations.value.toMutableList()
        val index = current.indexOfFirst { it.id == automation.id }
        if (index != -1) {
            current[index] = automation
            _automations.value = current
            saveToPrefs()
            updateActionShortcutLauncherState()
        }
    }

    fun removeAutomation(id: String) {
        val current = _automations.value.toMutableList()
        current.removeAll { it.id == id }
        _automations.value = current
        saveToPrefs()
        updateActionShortcutLauncherState()
    }

    fun getAutomation(id: String): Automation? = _automations.value.find { it.id == id }

    fun reloadMacros() {
        val json = prefs?.getString(KEY_MACROS, null)
        _macros.value = if (json != null) macrosFromJson(json) else emptyList()
    }

    private fun saveMacrosToPrefs() {
        val json = macrosToJson(_macros.value)
        prefs?.edit()?.putString(KEY_MACROS, json)?.apply()
    }

    fun addMacro(macro: Macro) {
        val current = _macros.value.toMutableList()
        current.add(macro)
        _macros.value = current
        saveMacrosToPrefs()
    }

    fun updateMacro(macro: Macro) {
        val current = _macros.value.toMutableList()
        val index = current.indexOfFirst { it.id == macro.id }
        if (index != -1) {
            current[index] = macro
            _macros.value = current
            saveMacrosToPrefs()
        }
    }

    fun removeMacro(id: String) {
        val current = _macros.value.toMutableList()
        current.removeAll { it.id == id }
        _macros.value = current
        saveMacrosToPrefs()
    }

    fun getMacro(id: String): Macro? = _macros.value.find { it.id == id }

    internal fun macrosToJson(macros: List<Macro>): String = gson.toJson(macros)

    internal fun macrosFromJson(json: String): List<Macro> =
        try {
            val type = object : TypeToken<List<Macro>>() {}.type
            val list: List<Macro> = gson.fromJson(json, type) ?: emptyList()
            // Gson puts nulls in the list for unknown types despite the non-null type
            list.filterNotNull().map { macro ->
                macro.copy(actions = macro.actionList.filterNotNull())
            }
        } catch (_: Exception) {
            emptyList()
        }

    private fun updateActionShortcutLauncherState() {
        val context = appContext ?: return
        val showLauncher =
            _automations.value.any { it.type == Automation.Type.ACTION_SHORTCUT && it.isEnabled }
        val componentName =
            android.content.ComponentName(
                context,
                "com.sameerasw.essentials.ActionShortcutLauncher",
            )
        try {
            val targetState =
                if (showLauncher) {
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
            val currentState = context.packageManager.getComponentEnabledSetting(componentName)
            if (currentState != targetState) {
                context.packageManager.setComponentEnabledSetting(
                    componentName,
                    targetState,
                    android.content.pm.PackageManager.DONT_KILL_APP,
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
