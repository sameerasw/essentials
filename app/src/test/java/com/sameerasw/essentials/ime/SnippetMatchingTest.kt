/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - IME
 * File: SnippetMatchingTest.kt
 * Description: Unit tests for Raycast-style snippet prefix matching and case-insensitivity.
 */

package com.sameerasw.essentials.ime

import com.sameerasw.essentials.ime.snippets.Snippet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetMatchingTest {

    private val snippets = listOf(
        Snippet(title = "Current Date", keyword = "!date", content = "{date}"),
        Snippet(title = "Shrug", keyword = "!shrug", content = "¯\\_(ツ)_/¯"),
        Snippet(title = "Email Address", keyword = "/email", content = "user@example.com"),
        Snippet(title = "Phone Number", keyword = ":phone", content = "+123456789"),
        Snippet(title = "Meeting Link", keyword = ";meet", content = "https://meet.google.com/xyz"),
    )

    private fun findMatching(word: String, list: List<Snippet>): List<Snippet> {
        val query = word.trim()
        if (query.isEmpty()) return emptyList()

        val isTriggerPrefix = query.startsWith("!") || query.startsWith("/") || query.startsWith(":") || query.startsWith(";")
        return list.filter { snippet ->
            snippet.keyword.equals(query, ignoreCase = true) ||
                (isTriggerPrefix && snippet.keyword.startsWith(query, ignoreCase = true))
        }
    }

    @Test
    fun findMatching_exactKeyword_matchesCaseInsensitive() {
        val resultsLower = findMatching("!date", snippets)
        assertEquals(1, resultsLower.size)
        assertEquals("!date", resultsLower.first().keyword)

        val resultsUpper = findMatching("!DATE", snippets)
        assertEquals(1, resultsUpper.size)
        assertEquals("!date", resultsUpper.first().keyword)
    }

    @Test
    fun findMatching_triggerPrefix_matchesCandidates() {
        val resultsExclamation = findMatching("!", snippets)
        assertEquals(2, resultsExclamation.size)

        val resultsSlash = findMatching("/em", snippets)
        assertEquals(1, resultsSlash.size)
        assertEquals("/email", resultsSlash.first().keyword)

        val resultsColon = findMatching(":ph", snippets)
        assertEquals(1, resultsColon.size)
        assertEquals(":phone", resultsColon.first().keyword)

        val resultsSemicolon = findMatching(";me", snippets)
        assertEquals(1, resultsSemicolon.size)
        assertEquals(";meet", resultsSemicolon.first().keyword)
    }

    @Test
    fun findMatching_ordinaryText_doesNotTriggerSubstringMatching() {
        // Normal typing like "the", "date", "shrug" should not trigger prefix matching unless explicit trigger prefix is typed
        val resultsOrdinary = findMatching("sh", snippets)
        assertTrue(resultsOrdinary.isEmpty())

        val resultsTitleSubstring = findMatching("Date", snippets)
        assertTrue(resultsTitleSubstring.isEmpty())
    }

    @Test
    fun findMatching_emptyQuery_returnsEmpty() {
        val results = findMatching("   ", snippets)
        assertTrue(results.isEmpty())
    }
}
