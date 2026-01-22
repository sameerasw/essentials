package com.sameerasw.essentials.domain.model

data class SnoozeChannel(
    val id: String,
    val name: String,
    val isBlocked: Boolean = false
)
