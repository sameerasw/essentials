/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Unit Tests - IME
 * File: SnippetMatchingTest.kt
 * Description: Unit tests for snippet matching, the typing pre-check and in-field replacement.
 */

package com.sameerasw.essentials.ime

import com.sameerasw.essentials.ime.snippets.Snippet
import com.sameerasw.essentials.ime.snippets.SnippetMatcher
import com.sameerasw.essentials.ime.snippets.SnippetReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetMatchingTest {
    private val snippets =
        listOf(
            Snippet(title = "Current Date", keyword = "!date", content = "{date}"),
            Snippet(title = "Shrug", keyword = "!shrug", content = "¯\\_(ツ)_/¯"),
            Snippet(title = "Email Address", keyword = "/email", content = "user@example.com"),
            Snippet(title = "Phone Number", keyword = ":phone", content = "+123456789"),
            Snippet(title = "Meeting Link", keyword = ";meet", content = "https://meet.google.com/xyz"),
        )

    private fun find(word: String) = SnippetMatcher.findMatching(word, snippets)

    @Test
    fun findMatching_exactKeyword_matchesCaseInsensitive() {
        assertEquals(listOf("!date"), find("!date").map { it.keyword })
        assertEquals(listOf("!date"), find("!DATE").map { it.keyword })
    }

    @Test
    fun findMatching_triggerPrefix_matchesCandidates() {
        assertEquals(2, find("!").size)
        assertEquals(listOf("/email"), find("/em").map { it.keyword })
        assertEquals(listOf(":phone"), find(":ph").map { it.keyword })
        assertEquals(listOf(";meet"), find(";me").map { it.keyword })
    }

    @Test
    fun findMatching_ordinaryText_doesNotTriggerSubstringMatching() {
        assertTrue(find("sh").isEmpty())
        assertTrue(find("Date").isEmpty())
    }

    @Test
    fun findMatching_emptyQuery_returnsEmpty() {
        assertTrue(find("   ").isEmpty())
    }

    @Test
    fun mightTrigger_ordinaryTyping_isFalse() {
        assertFalse(SnippetMatcher.mightTrigger("Hello wor", snippets, autoExpandEnabled = true))
        assertFalse(SnippetMatcher.mightTrigger("see you at 5 ", snippets, autoExpandEnabled = true))
    }

    @Test
    fun mightTrigger_triggerPrefixOrKeyword_isTrue() {
        assertTrue(SnippetMatcher.mightTrigger("hello !da", snippets, autoExpandEnabled = false))
        assertTrue(SnippetMatcher.mightTrigger("hello !date", snippets, autoExpandEnabled = false))
    }

    @Test
    fun mightTrigger_spaceAfterKeyword_onlyWhenAutoExpandOn() {
        assertTrue(SnippetMatcher.mightTrigger("hello !date ", snippets, autoExpandEnabled = true))
        assertFalse(SnippetMatcher.mightTrigger("hello !date ", snippets, autoExpandEnabled = false))
        assertFalse(SnippetMatcher.mightTrigger("hello !unknown ", snippets, autoExpandEnabled = true))
    }

    @Test
    fun replace_cursorAtEnd_replacesKeyword() {
        val text = "Meeting today at !time "
        val result = SnippetReplacement.compute(text, text.length, "!time ", "5:00 pm ")
        assertEquals("Meeting today at 5:00 pm ", result?.newText)
        assertEquals(result?.newText?.length, result?.cursor)
    }

    @Test
    fun replace_keywordAlsoAtEndOfText_doesNotDeleteTextAfterCursor() {
        // Regression: the old logic replaced from the first keyword to the end of the field.
        val text = "!date foo !date"
        val cursor = "!date ".length
        val result = SnippetReplacement.compute(text, cursor, "!date ", "2026-10-18 ")
        assertEquals("2026-10-18 foo !date", result?.newText)
        assertEquals("2026-10-18 ".length, result?.cursor)
    }

    @Test
    fun replace_cursorInMiddle_preservesSurroundingText() {
        val text = "Before !date after"
        val cursor = text.indexOf("!date") + "!date".length
        val result = SnippetReplacement.compute(text, cursor, "!date", "2026-10-18 ")
        assertEquals("Before 2026-10-18  after", result?.newText)
    }

    @Test
    fun replace_laterOccurrence_usesCursorNotLastIndexOf() {
        val text = "!date and some text and !date"
        val firstEnd = "!date".length
        val result = SnippetReplacement.compute(text, firstEnd, "!date", "X")
        assertEquals("X and some text and !date", result?.newText)
    }

    @Test
    fun replace_cursorMismatched_returnsNullAndNeverGuesses() {
        val text = "Here is !date and typing here"
        assertNull(SnippetReplacement.compute(text, text.length, "!date", "X"))
    }

    @Test
    fun replace_noSelectionReported_replacesOnlyTrailingKeyword() {
        val text = "Search !date"
        val result = SnippetReplacement.compute(text, -1, "!date", "2026-10-18 ")
        assertEquals("Search 2026-10-18 ", result?.newText)
    }

    @Test
    fun replace_noSelectionReported_toleratesTrailingWhitespaceOnly() {
        val text = "Search !date  "
        val result = SnippetReplacement.compute(text, -1, "!date", "X")
        assertEquals("Search X  ", result?.newText)
    }

    @Test
    fun replace_noSelectionReported_keywordInMiddle_returnsNull() {
        assertNull(SnippetReplacement.compute("!date and more", -1, "!date", "X"))
    }

    @Test
    fun replace_isCaseInsensitive() {
        val result = SnippetReplacement.compute("say !DATE", "say !DATE".length, "!date", "today")
        assertNotNull(result)
        assertEquals("say today", result?.newText)
    }

    @Test
    fun replace_emptyTarget_returnsNull() {
        assertNull(SnippetReplacement.compute("abc", 3, "", "X"))
    }
}
