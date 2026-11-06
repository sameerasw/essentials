/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: IME - Snippets
 * File: SnippetKeywordRules.kt
 * Description: Validation rules for snippet keywords shared by the editor and backup import.
 */

package com.sameerasw.essentials.ime.snippets

object SnippetKeywordRules {
    const val MAX_LENGTH = 32
    val TRIGGER_CHARS = charArrayOf('!', '/', ':', ';')

    enum class Problem {
        Empty,
        ContainsWhitespace,
        MissingTrigger,
        TriggerOnly,
        TooLong,
        Duplicate,
    }

    fun validate(
        rawKeyword: String,
        existing: List<Snippet>,
        editingId: String? = null,
    ): Problem? {
        val keyword = rawKeyword.trim()
        return when {
            keyword.isEmpty() -> Problem.Empty
            keyword.any { it.isWhitespace() } -> Problem.ContainsWhitespace
            keyword.first() !in TRIGGER_CHARS -> Problem.MissingTrigger
            keyword.length == 1 -> Problem.TriggerOnly
            keyword.length > MAX_LENGTH -> Problem.TooLong
            existing.any { it.id != editingId && it.keyword.equals(keyword, ignoreCase = true) } ->
                Problem.Duplicate
            else -> null
        }
    }
}
