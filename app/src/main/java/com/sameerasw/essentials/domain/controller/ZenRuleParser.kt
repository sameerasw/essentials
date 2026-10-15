/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Controllers
 * File: ZenRuleParser.kt
 * Description: Reads the getAutomaticZenRules result, which is a Map on Android 15 and a ParceledListSlice of AzrWithId from Android 16.
 */

package com.sameerasw.essentials.domain.controller

@Suppress("UNCHECKED_CAST")
internal fun <T> parseZenRules(result: Any?): Map<String, T> =
    when (result) {
        null -> emptyMap()
        is Map<*, *> -> result as Map<String, T>
        else -> {
            val list = result.javaClass.getMethod("getList").invoke(result) as List<Any>
            list.associate { item ->
                val cls = item.javaClass
                val id = cls.getDeclaredField("mId").apply { isAccessible = true }.get(item) as String
                val rule = cls.getDeclaredField("mRule").apply { isAccessible = true }.get(item) as T
                id to rule
            }
        }
    }
