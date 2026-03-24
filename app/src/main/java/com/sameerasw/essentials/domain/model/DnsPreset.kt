package com.sameerasw.essentials.domain.model

data class DnsPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val hostname: String,
    val isDefault: Boolean = false
)
