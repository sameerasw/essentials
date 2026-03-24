package com.sameerasw.essentials.domain.model

enum class FreezeMode(val value: Int) {
    FREEZE(0),
    SUSPEND(1);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: FREEZE
    }
}
