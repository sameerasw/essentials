/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: IME - Snippets
 * File: SnippetBackup.kt
 * Description: JSON export and validated import of snippets so they survive device changes.
 */

package com.sameerasw.essentials.ime.snippets

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import java.io.InputStream

object SnippetBackup {
    const val VERSION = 1
    const val MAX_BYTES = 2 * 1024 * 1024

    data class ImportResult(
        val imported: List<Snippet>,
        val skipped: Int,
    )

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** Reads at most [maxBytes] of UTF-8 text. Returns null if the stream is larger. */
    fun read(
        input: InputStream,
        maxBytes: Int = MAX_BYTES,
    ): String? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) return null
            out.write(buffer, 0, read)
        }
        return out.toString(Charsets.UTF_8.name())
    }

    fun toJson(snippets: List<Snippet>): String {
        val array = JsonArray()
        snippets.forEach { snippet ->
            array.add(
                JsonObject().apply {
                    addProperty("title", snippet.title)
                    addProperty("keyword", snippet.keyword)
                    addProperty("content", snippet.content)
                    addProperty("autoExpandOnSpace", snippet.autoExpandOnSpace)
                    addProperty("createdAt", snippet.createdAt)
                },
            )
        }
        val root =
            JsonObject().apply {
                addProperty("version", VERSION)
                add("snippets", array)
            }
        return gson.toJson(root)
    }

    /**
     * Parses a backup (either the versioned object or a bare array). Entries with an invalid
     * or duplicate keyword, or empty content, are skipped. Returns null if the JSON is not a
     * snippet backup at all.
     */
    fun parse(
        json: String,
        existing: List<Snippet>,
    ): ImportResult? {
        val array =
            try {
                val root = JsonParser.parseString(json)
                when {
                    root.isJsonArray -> root.asJsonArray
                    root.isJsonObject && root.asJsonObject.get("snippets")?.isJsonArray == true ->
                        root.asJsonObject.getAsJsonArray("snippets")
                    else -> return null
                }
            } catch (_: JsonParseException) {
                return null
            }

        val accepted = mutableListOf<Snippet>()
        var skipped = 0
        array.forEach { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject
            val keyword = obj?.string("keyword")?.trim().orEmpty()
            val content = obj?.string("content").orEmpty()
            val problem = SnippetKeywordRules.validate(keyword, existing + accepted)
            if (obj == null || problem != null || content.isBlank()) {
                skipped++
                return@forEach
            }
            accepted +=
                Snippet(
                    title = obj.string("title").orEmpty().ifBlank { keyword },
                    keyword = keyword,
                    content = content,
                    autoExpandOnSpace = obj.boolean("autoExpandOnSpace") ?: true,
                    createdAt = obj.long("createdAt") ?: System.currentTimeMillis(),
                )
        }
        return ImportResult(accepted, skipped)
    }

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

    private fun JsonObject.boolean(key: String): Boolean? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isBoolean }?.asBoolean

    private fun JsonObject.long(key: String): Long? =
        get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asLong
}
