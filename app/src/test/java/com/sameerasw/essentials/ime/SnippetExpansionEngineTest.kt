/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Test - IME
 * File: SnippetExpansionEngineTest.kt
 * Description: Unit tests for SnippetExpansionEngine.
 */

package com.sameerasw.essentials.ime

import com.sameerasw.essentials.domain.model.Snippet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetExpansionEngineTest {
    private val sampleSnippets =
        listOf(
            Snippet(id = "1", keyword = "brb", title = "Be Right Back", content = "Be right back!"),
            Snippet(id = "2", keyword = "addr", title = "Address", content = "123 Main Street"),
            Snippet(id = "3", keyword = "email", title = "Email", content = "user@test.com"),
            Snippet(id = "4", keyword = "app", title = "App", content = "Application"),
            Snippet(id = "5", keyword = "apple", title = "Apple", content = "Fresh Apple"),
        )

    @Test
    fun testExpansionWithSemicolonPrefix() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { ";" },
                isEnabled = { true },
            )

        var deleted = 0
        var replacement = ""

        val matched =
            engine.checkAndExpand("Hello ;brb") { deleteCount, text ->
                deleted = deleteCount
                replacement = text
            }

        assertTrue(matched)
        assertEquals(4, deleted)
        assertEquals("Be right back!", replacement)
    }

    @Test
    fun testCaseInsensitiveExpansion() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { ";" },
                isEnabled = { true },
            )

        var replacement = ""
        val matched =
            engine.checkAndExpand("Hello ;Brb") { _, text ->
                replacement = text
            }

        assertTrue(matched)
        assertEquals("Be right back!", replacement)
    }

    @Test
    fun testWordBoundaryPreventsUnintendedExpansion() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { ":" },
                isEnabled = { true },
            )

        var called = false
        val matchedTime =
            engine.checkAndExpand("Time is 10:email") { _, _ ->
                called = true
            }
        assertFalse(matchedTime)
        assertFalse(called)

        val matchedBoundary =
            engine.checkAndExpand("Time is :email") { _, text ->
                called = true
            }
        assertTrue(matchedBoundary)
        assertTrue(called)
    }

    @Test
    fun testShortKeywordDoesNotBlockLongerSiblingWithoutDelimiter() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { ";" },
                isEnabled = { true },
            )

        var expandedText = ""

        val matchedShortWithoutDelimiter =
            engine.checkAndExpand("Check ;app") { _, text ->
                expandedText = text
            }
        assertFalse(matchedShortWithoutDelimiter)

        val matchedShortWithDelimiter =
            engine.checkAndExpand("Check ;app ") { _, text ->
                expandedText = text
            }
        assertTrue(matchedShortWithDelimiter)
        assertEquals("Application ", expandedText)

        val matchedLongerImmediate =
            engine.checkAndExpand("Check ;apple") { _, text ->
                expandedText = text
            }
        assertTrue(matchedLongerImmediate)
        assertEquals("Fresh Apple", expandedText)
    }

    @Test
    fun testExpansionWithDoubleAtPrefix() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { "@@" },
                isEnabled = { true },
            )

        var deleted = 0
        var replacement = ""

        val matched =
            engine.checkAndExpand("Contact me at @@email") { deleteCount, text ->
                deleted = deleteCount
                replacement = text
            }

        assertTrue(matched)
        assertEquals(7, deleted)
        assertEquals("user@test.com", replacement)
    }

    @Test
    fun testExpansionOnSpaceWhenPrefixIsEmpty() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { "" },
                isEnabled = { true },
            )

        var deleted = 0
        var replacement = ""

        val matched =
            engine.checkAndExpand("My address is addr ") { deleteCount, text ->
                deleted = deleteCount
                replacement = text
            }

        assertTrue(matched)
        assertEquals(5, deleted)
        assertEquals("123 Main Street ", replacement)
    }

    @Test
    fun testDisabledEngineDoesNotExpand() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { ";" },
                isEnabled = { false },
            )

        var called = false
        val matched =
            engine.checkAndExpand(";brb") { _, _ ->
                called = true
            }

        assertFalse(matched)
        assertFalse(called)
    }

    @Test
    fun testNonMatchingPrefixDoesNotExpand() {
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { ";" },
                isEnabled = { true },
            )

        var called = false
        val matched =
            engine.checkAndExpand(":brb") { _, _ ->
                called = true
            }

        assertFalse(matched)
        assertFalse(called)
    }
}
