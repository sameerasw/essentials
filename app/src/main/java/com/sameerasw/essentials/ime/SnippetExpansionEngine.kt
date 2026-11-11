/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Ime
 * File: SnippetExpansionEngine.kt
 * Description: Engine for detecting snippet prefixes/keywords and expanding text.
 */

package com.sameerasw.essentials.ime

import com.sameerasw.essentials.domain.model.Snippet

class SnippetExpansionEngine(
    private val getSnippets: () -> List<Snippet>,
    private val getPrefix: () -> String,
    private val isEnabled: () -> Boolean,
) {
    /**
     * Checks if the text before cursor matches any active snippet trigger.
     * When matched, calls onExpand with the number of characters to delete and the replacement string.
     *
     * @param textBeforeCursor Text preceding the cursor.
     * @param onExpand Callback returning deleteCount and replacement content.
     * @return True if a snippet was matched and expanded, false otherwise.
     */
    fun checkAndExpand(
        textBeforeCursor: String,
        onExpand: (deleteCount: Int, replacement: String) -> Unit,
    ): Boolean {
        if (!isEnabled() || textBeforeCursor.isEmpty()) return false
        val prefix = getPrefix()
        val snippets = getSnippets()
        if (snippets.isEmpty()) return false

        val sortedSnippets = snippets.sortedByDescending { it.keyword.length }

        for (snippet in sortedSnippets) {
            if (snippet.keyword.isEmpty()) continue

            if (prefix.isNotEmpty()) {
                val trigger = "$prefix${snippet.keyword}"
                if (textBeforeCursor.endsWith(trigger)) {
                    onExpand(trigger.length, snippet.content)
                    return true
                }
            } else {
                val lastChar = textBeforeCursor.last()
                if (lastChar.isWhitespace()) {
                    val trimmed = textBeforeCursor.dropLast(1)
                    if (trimmed.endsWith(snippet.keyword)) {
                        val beforeKeywordIndex = trimmed.length - snippet.keyword.length - 1
                        val isBoundary = beforeKeywordIndex < 0 || !trimmed[beforeKeywordIndex].isLetterOrDigit()
                        if (isBoundary) {
                            onExpand(snippet.keyword.length + 1, snippet.content + lastChar)
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}
