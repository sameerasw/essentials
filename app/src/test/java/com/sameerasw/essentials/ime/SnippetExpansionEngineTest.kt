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
            Snippet(id = 1L, keyword = "brb", title = "Be Right Back", content = "Be right back!"),
            Snippet(id = 2L, keyword = "addr", title = "Address", content = "123 Main Street"),
            Snippet(id = 3L, keyword = "email", title = "Email", content = "user@test.com"),
        )

    @Test
    fun testExpansionWithSemicolonPrefix() {
        var prefix = ";"
        var enabled = true
        val engine =
            SnippetExpansionEngine(
                getSnippets = { sampleSnippets },
                getPrefix = { prefix },
                isEnabled = { enabled },
            )

        var deleted = 0
        var replacement = ""

        val matched =
            engine.checkAndExpand("Hello ;brb") { deleteCount, text ->
                deleted = deleteCount
                replacement = text
            }

        assertTrue(matched)
        assertEquals(4, deleted) // ";brb".length == 4
        assertEquals("Be right back!", replacement)
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
        assertEquals(7, deleted) // "@@email".length == 7
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
        assertEquals(5, deleted) // "addr ".length == 5
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
