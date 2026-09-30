/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: IME - Snippets
 * File: SnippetRepository.kt
 * Description: Repository for persisting and managing custom snippets.
 */

package com.sameerasw.essentials.ime.snippets

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SnippetRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private val storageFile = File(appContext.filesDir, "snippets.json")

    private val _snippets = MutableStateFlow<List<Snippet>>(emptyList())
    val snippets: StateFlow<List<Snippet>> = _snippets.asStateFlow()

    init {
        loadSnippets()
    }

    private fun loadSnippets() {
        scope.launch {
            try {
                if (!storageFile.exists()) {
                    val defaults = listOf(
                        Snippet(
                            title = "Current Date",
                            keyword = "!date",
                            content = "{date}",
                        ),
                        Snippet(
                            title = "Quote Clipboard",
                            keyword = "!quote",
                            content = "> {clipboard}",
                        ),
                    )
                    saveToFile(defaults)
                    _snippets.value = defaults
                    return@launch
                }

                val json = storageFile.readText()
                val type = object : TypeToken<List<Snippet>>() {}.type
                val loaded: List<Snippet>? = gson.fromJson(json, type)
                _snippets.value = loaded ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading snippets", e)
            }
        }
    }

    private suspend fun saveToFile(items: List<Snippet>) = withContext(Dispatchers.IO) {
        try {
            val json = gson.toJson(items)
            FileOutputStream(storageFile).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving snippets", e)
        }
    }

    fun addSnippet(snippet: Snippet) {
        val updated = _snippets.value + snippet
        _snippets.value = updated
        scope.launch { saveToFile(updated) }
    }

    fun updateSnippet(snippet: Snippet) {
        val updated = _snippets.value.map { if (it.id == snippet.id) snippet else it }
        _snippets.value = updated
        scope.launch { saveToFile(updated) }
    }

    fun deleteSnippet(id: String) {
        val updated = _snippets.value.filterNot { it.id == id }
        _snippets.value = updated
        scope.launch { saveToFile(updated) }
    }

    /**
     * Finds snippets matching the current typed word prefix or title.
     */
    fun findMatching(word: String): List<Snippet> {
        val query = word.trim()
        if (query.isEmpty()) return emptyList()

        return _snippets.value.filter { snippet ->
            snippet.keyword.equals(query, ignoreCase = true) ||
                snippet.keyword.startsWith(query, ignoreCase = true) ||
                (query.length >= 2 && snippet.title.contains(query, ignoreCase = true))
        }
    }

    companion object {
        private const val TAG = "SnippetRepository"

        @Volatile
        private var INSTANCE: SnippetRepository? = null

        fun getInstance(context: Context): SnippetRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SnippetRepository(context).also { INSTANCE = it }
            }
        }
    }
}
