/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: IME - Snippets
 * File: SnippetReplacement.kt
 * Description: Pure text replacement used by every snippet expansion path.
 */

package com.sameerasw.essentials.ime.snippets

object SnippetReplacement {
    data class Result(
        val newText: String,
        val cursor: Int,
    )

    /**
     * Replaces [target] with [replacement] where [target] ends at [cursor], leaving all other
     * text untouched. A negative [cursor] means the field did not report a selection; then only
     * a [target] at the very end of [fullText] (ignoring trailing whitespace) is replaced.
     * Returns null when [target] is not where it is expected, so callers never edit the wrong text.
     */
    fun compute(
        fullText: String,
        cursor: Int,
        target: String,
        replacement: String,
    ): Result? {
        if (target.isEmpty()) return null

        val range =
            if (cursor >= 0) {
                matchEndingAt(fullText, cursor, target)
            } else {
                matchEndingAt(fullText, fullText.length, target)
                    ?: matchEndingAt(fullText, fullText.trimEnd().length, target.trimEnd())
            } ?: return null

        return Result(
            newText = fullText.substring(0, range.first) + replacement + fullText.substring(range.last + 1),
            cursor = range.first + replacement.length,
        )
    }

    private fun matchEndingAt(
        text: String,
        end: Int,
        target: String,
    ): IntRange? {
        if (target.isEmpty() || end > text.length) return null
        val start = end - target.length
        if (start < 0 || !text.regionMatches(start, target, 0, target.length, ignoreCase = true)) return null
        return start until end
    }
}
