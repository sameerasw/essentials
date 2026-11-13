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
    fun checkAndExpand(
        textBeforeCursor: String,
        onExpand: (deleteCount: Int, replacement: String) -> Unit,
    ): Boolean {
        if (!isEnabled() || textBeforeCursor.isEmpty()) return false
        val prefix = getPrefix()
        val snippets = getSnippets()
        if (snippets.isEmpty()) return false

        val sorted = snippets.sortedByDescending { it.keyword.length }

        for (snippet in sorted) {
            if (snippet.keyword.isEmpty()) continue

            if (prefix.isNotEmpty()) {
                val trigger = "$prefix${snippet.keyword}"
                val hasDelimiter = textBeforeCursor.endsWith(" ") || textBeforeCursor.endsWith("\t")
                val candidate = if (hasDelimiter) textBeforeCursor.dropLast(1) else textBeforeCursor

                if (candidate.endsWith(trigger, ignoreCase = true)) {
                    val triggerStart = candidate.length - trigger.length
                    val isBoundary = triggerStart == 0 || candidate[triggerStart - 1].isWhitespace()
                    if (!isBoundary) continue

                    if (!hasDelimiter) {
                        val hasLongerSibling = sorted.any {
                            it.keyword.length > snippet.keyword.length &&
                                it.keyword.startsWith(snippet.keyword, ignoreCase = true)
                        }
                        if (hasLongerSibling) continue
                    }

                    val matchLen = if (hasDelimiter) trigger.length + 1 else trigger.length
                    val replacement = if (hasDelimiter) snippet.content + " " else snippet.content
                    onExpand(matchLen, replacement)
                    return true
                }
            } else {
                val lastChar = textBeforeCursor.last()
                if (lastChar.isWhitespace()) {
                    val trimmed = textBeforeCursor.dropLast(1)
                    if (trimmed.endsWith(snippet.keyword, ignoreCase = true)) {
                        val beforeIndex = trimmed.length - snippet.keyword.length - 1
                        val isBoundary = beforeIndex < 0 || trimmed[beforeIndex].isWhitespace()
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
