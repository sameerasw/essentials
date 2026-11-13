/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Model Layer
 * File: Snippet.kt
 * Description: Data class representing an expandable text snippet with trigger keyword.
 */

package com.sameerasw.essentials.domain.model

import java.util.UUID

data class Snippet(
    val id: String = UUID.randomUUID().toString(),
    val keyword: String,
    val title: String = "",
    val content: String,
)
