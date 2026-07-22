package com.sameerasw.essentials.translation.model

import com.google.gson.Gson

data class StringEntry(
    val key: String,
    val locale: String,
    val value: String
)

data class TranslationEdit(
    val key: String,
    val locale: String,
    val originalValue: String,
    val newValue: String
)

data class TranslationSession(
    val edits: MutableList<TranslationEdit> = mutableListOf()
) {
    fun addOrUpdate(edit: TranslationEdit) {
        val existingIndex = edits.indexOfFirst { it.key == edit.key && it.locale == edit.locale }
        if (existingIndex != -1) {
            edits[existingIndex] = edit
        } else {
            edits.add(edit)
        }
    }

    fun remove(key: String, locale: String) {
        edits.removeAll { it.key == key && it.locale == locale }
    }

    fun clear() {
        edits.clear()
    }

    fun toJsonPayload(): String {
        val gson = Gson()
        val payloadList = edits.map {
            mapOf(
                "key" to it.key,
                "locale" to it.locale,
                "value" to it.newValue,
                "original" to it.originalValue
            )
        }
        return gson.toJson(payloadList)
    }
}
