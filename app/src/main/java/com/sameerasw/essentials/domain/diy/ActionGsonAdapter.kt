/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: ActionGsonAdapter.kt
 * Description: Shared Gson serializer/deserializer for the Action sealed interface.
 *              Used by both DIYRepository and SettingsRepository.
 */

package com.sameerasw.essentials.domain.diy

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlin.reflect.KClass

object ActionGsonAdapter {
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
            val typeName = json.asJsonObject.get("type")?.asString ?: return null
            val subClass = kClass.sealedSubclasses.firstOrNull { it.simpleName == typeName }
            return if (subClass != null) {
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

    private val gson =
        GsonBuilder()
            .registerTypeAdapter(Action::class.java, SealedAdapter(Action::class))
            .create()

    fun toJson(action: Action): String = gson.toJson(action, Action::class.java)

    fun fromJson(json: String): Action? =
        try {
            gson.fromJson(json, Action::class.java)
        } catch (_: Exception) {
            null
        }

    fun listToJson(actions: List<Action>): String {
        val array = JsonArray()
        actions.forEach { array.add(gson.toJsonTree(it, Action::class.java)) }
        return array.toString()
    }

    fun listFromJson(json: String): List<Action> =
        try {
            JsonParser.parseString(json).asJsonArray.mapNotNull { element ->
                try {
                    gson.fromJson(element, Action::class.java)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
}
