package com.sameerasw.essentials.island.ui.components

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection

internal fun String.contentTextDirection(): TextDirection {
    var i = 0
    while (i < length) {
        val cp = codePointAt(i)
        when (Character.getDirectionality(cp)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return TextDirection.Rtl
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return TextDirection.Ltr
        }
        i += Character.charCount(cp)
    }
    return TextDirection.Content
}

internal fun TextStyle.forContent(text: String): TextStyle = copy(textDirection = text.contentTextDirection())
