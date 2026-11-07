/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: IME - Snippets
 * File: Snippet.kt
 * Description: Data model representing a text replacement snippet.
 */

package com.sameerasw.essentials.ime.snippets

import androidx.annotation.Keep
import java.util.UUID

@Keep
data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val keyword: String,
    val content: String,
    val autoExpandOnSpace: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)
