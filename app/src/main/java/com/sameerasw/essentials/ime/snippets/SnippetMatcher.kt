/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: IME - Snippets
 * File: SnippetMatcher.kt
 * Description: Pure keyword matching shared by the IME and the accessibility handler.
 */

package com.sameerasw.essentials.ime.snippets

object SnippetMatcher {
    fun hasTriggerPrefix(word: String): Boolean =
        word.isNotEmpty() && word.first() in SnippetKeywordRules.TRIGGER_CHARS

    fun findMatching(
        word: String,
        snippets: List<Snippet>,
    ): List<Snippet> {
        val query = word.trim()
        if (query.isEmpty()) return emptyList()

        val isTriggerPrefix = hasTriggerPrefix(query)
        return snippets.filter { snippet ->
            snippet.keyword.equals(query, ignoreCase = true) ||
                (isTriggerPrefix && snippet.keyword.startsWith(query, ignoreCase = true))
        }
    }

    fun findExact(
        word: String,
        snippets: List<Snippet>,
    ): Snippet? = snippets.firstOrNull { it.keyword.equals(word, ignoreCase = true) }

    /**
     * Cheap pre-check on the typed text so callers can skip accessibility node work for
     * ordinary typing. True when the text could show a suggestion or trigger an auto-expand.
     */
    fun mightTrigger(
        text: String,
        snippets: List<Snippet>,
        autoExpandEnabled: Boolean,
    ): Boolean {
        if (text.isEmpty()) return false

        if (text.last() == ' ') {
            if (!autoExpandEnabled) return false
            val word = text.dropLast(1).takeLastWhile { !it.isWhitespace() }
            return word.isNotEmpty() && snippets.any { it.keyword.equals(word, ignoreCase = true) }
        }

        val word = text.takeLastWhile { !it.isWhitespace() }
        return hasTriggerPrefix(word) || snippets.any { it.keyword.equals(word, ignoreCase = true) }
    }
}
