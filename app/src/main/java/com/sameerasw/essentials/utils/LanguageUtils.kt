package com.sameerasw.essentials.utils

object LanguageUtils {
    val languages = listOf(
        Language("en", "English", "English"),
        Language("si", "Sinhala", "සිංහල"),
        Language("ar", "Arabic", "العربية"),
        Language("de", "German", "Deutsch"),
        Language("es", "Spanish", "Español"),
        Language("fr", "French", "Français"),
        Language("it", "Italian", "Italiano"),
        Language("ja", "Japanese", "日本語"),
        Language("ko", "Korean", "한국어"),
        Language("pt-BR", "Portuguese BR", "Português BR"),
        Language("pt-PT", "Portuguese PT", "Português PT"),
        Language("ru", "Russian", "Русский"),
        Language("tr", "Turkish", "Türkçe"),
        Language("zh", "Chinese", "中文"),
        Language("uk", "Ukrainian", "Українська"),
        Language("vi", "Vietnamese", "Tiếng Việt"),
        Language("pl", "Polish", "Polski"),
        Language("no", "Norwegian", "Norsk"),
        Language("nl", "Dutch", "Nederlands"),
        Language("hu", "Hungarian", "Magyar"),
        Language("he", "Hebrew", "עברית"),
        Language("fi", "Finnish", "Suomi"),
        Language("el", "Greek", "Ελληνικά"),
        Language("da", "Danish", "Dansk"),
        Language("cs", "Czech", "Čេština"),
        Language("ca", "Catalan", "Català"),
        Language("af", "Afrikaans", "Afrikaans"),
        Language("ach", "Acholi", "Luo"),
        Language("sr", "Serbian", "Српски"),
        Language("sv", "Swedish", "Svenska"),
        Language("ro", "Romanian", "Română")
    )

    data class Language(val code: String, val name: String, val nativeName: String)
}
