/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - General
 * File: LanguageUtils.kt
 * Description: Utility helper for LanguageUtils.kt.
 */

package com.sameerasw.essentials.utils

import java.util.Locale

object LanguageUtils {
    val languages =
        listOf(
            Language("en", "English", "English"),
            Language("si", "Sinhala", "සිංහල"),
            Language("ach", "Acholi", "Luo"),
            Language("af", "Afrikaans", "Afrikaans"),
            Language("ar", "Arabic", "العربية"),
            Language("bn-BD", "Bengali", "বাংলা"),
            Language("ca", "Catalan", "Català"),
            Language("cs", "Czech", "Čeština"),
            Language("da", "Danish", "Dansk"),
            Language("de", "German", "Deutsch"),
            Language("el", "Greek", "Ελληνικά"),
            Language("es", "Spanish", "Español"),
            Language("fi", "Finnish", "Suomi"),
            Language("fil-PH", "Filipino", "Filipino"),
            Language("fr", "French", "Français"),
            Language("he", "Hebrew", "עברית"),
            Language("hi-IN", "Hindi", "हिन्दी"),
            Language("hu", "Hungarian", "Magyar"),
            Language("id", "Indonesian", "Bahasa Indonesia"),
            Language("it", "Italian", "Italiano"),
            Language("ja", "Japanese", "日本語"),
            Language("kk", "Kazakh", "Қазақша"),
            Language("ko", "Korean", "한국어"),
            Language("ml-IN", "Malayalam", "മലയാളം"),
            Language("ne-NP", "Nepali", "नेपाली"),
            Language("nl", "Dutch", "Nederlands"),
            Language("no", "Norwegian", "Norsk"),
            Language("pl", "Polish", "Polski"),
            Language("pt-BR", "Portuguese BR", "Português BR"),
            Language("pt-PT", "Portuguese PT", "Português PT"),
            Language("ro", "Romanian", "Română"),
            Language("ru", "Russian", "Русский"),
            Language("sk-SK", "Slovak", "Slovenčina"),
            Language("sr", "Serbian", "Српски"),
            Language("sv", "Swedish", "Svenska"),
            Language("ta-IN", "Tamil", "தமிழ்"),
            Language("tr", "Turkish", "Türkçe"),
            Language("uk", "Ukrainian", "Українська"),
            Language("vi", "Vietnamese", "Tiếng Việt"),
            Language("zh-CN", "Chinese", "简体中文"),
            Language("zh-TW", "Chinese", "繁體中文"),
        )

    data class Language(
        val code: String,
        val name: String,
        val nativeName: String,
    )

    fun getLanguage(code: String): Language =
        languages.find { it.code == code || it.code.startsWith(code) || code.startsWith(it.code) }
            ?: run {
                val parts = code.split("-", "_")
                val loc =
                    if (parts.size > 1) {
                        Locale(parts[0], parts[1].removePrefix("r"))
                    } else {
                        Locale(
                            parts[0],
                        )
                    }
                val name = loc.displayLanguage.ifBlank { code }
                val nativeName = loc.getDisplayLanguage(loc).ifBlank { name }
                Language(code, name, nativeName)
            }
}
