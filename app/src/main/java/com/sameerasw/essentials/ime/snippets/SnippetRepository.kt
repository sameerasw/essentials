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
import android.util.AtomicFile
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class SnippetRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private val storageFile = File(appContext.filesDir, "snippets.json")
    private val atomicFile = AtomicFile(storageFile)
    private val fileMutex = Mutex()

    private val _snippets = MutableStateFlow<List<Snippet>>(DEFAULT_SNIPPETS)
    val snippets: StateFlow<List<Snippet>> = _snippets.asStateFlow()

    init {
        loadSnippets()
    }

    private fun loadSnippets() {
        scope.launch {
            try {
                if (!storageFile.exists()) {
                    saveToFile(DEFAULT_SNIPPETS)
                    _snippets.value = DEFAULT_SNIPPETS
                    return@launch
                }

                val json = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
                val type = object : TypeToken<List<Snippet>>() {}.type
                val loaded: List<Snippet>? = gson.fromJson(json, type)
                if (!loaded.isNullOrEmpty()) {
                    _snippets.value = loaded
                } else if (loaded != null) {
                    _snippets.value = emptyList()
                } else {
                    Log.w(TAG, "Snippets file read returned null; preserving memory state")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading snippets", e)
            }
        }
    }

    private suspend fun saveToFile(items: List<Snippet>) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            var fos: FileOutputStream? = null
            try {
                val json = gson.toJson(items)
                fos = atomicFile.startWrite()
                fos.write(json.toByteArray(Charsets.UTF_8))
                atomicFile.finishWrite(fos)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving snippets", e)
                fos?.let { atomicFile.failWrite(it) }
            }
        }
    }

    fun addSnippet(snippet: Snippet) {
        val updated = _snippets.updateAndGet { current -> current + snippet }
        scope.launch { saveToFile(updated) }
    }

    fun updateSnippet(snippet: Snippet) {
        val updated = _snippets.updateAndGet { current ->
            current.map { if (it.id == snippet.id) snippet else it }
        }
        scope.launch { saveToFile(updated) }
    }

    fun deleteSnippet(id: String) {
        val updated = _snippets.updateAndGet { current ->
            current.filterNot { it.id == id }
        }
        scope.launch { saveToFile(updated) }
    }

    fun findMatching(word: String): List<Snippet> {
        val query = word.trim()
        if (query.isEmpty()) return emptyList()

        val isTriggerPrefix = query.startsWith("!") || query.startsWith("/") || query.startsWith(":") || query.startsWith(";")
        return _snippets.value.filter { snippet ->
            snippet.keyword.equals(query, ignoreCase = true) ||
                (isTriggerPrefix && snippet.keyword.startsWith(query, ignoreCase = true))
        }
    }

    companion object {
        private const val TAG = "SnippetRepository"

        val DEFAULT_SNIPPETS = listOf(
            Snippet(
                title = "Current Date",
                keyword = "!date",
                content = "{date}",
            ),
            Snippet(
                title = "Current Time",
                keyword = "!time",
                content = "{time}",
            ),
            Snippet(
                title = "Shrug",
                keyword = "!shrug",
                content = "¯\\_(ツ)_/¯",
            ),
        )

        @Volatile
        private var INSTANCE: SnippetRepository? = null

        fun getInstance(context: Context): SnippetRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SnippetRepository(context).also { INSTANCE = it }
            }
        }
    }
}
