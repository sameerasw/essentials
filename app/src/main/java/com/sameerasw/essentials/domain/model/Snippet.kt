/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Model Layer
 * File: Snippet.kt
 * Description: Data class representing an expandable text snippet with trigger keyword.
 */

package com.sameerasw.essentials.domain.model

data class Snippet(
    val id: Long = System.currentTimeMillis(),
    val keyword: String,
    val title: String = "",
    val content: String,
)
