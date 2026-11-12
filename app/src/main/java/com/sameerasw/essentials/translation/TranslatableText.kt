/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Translation
 * File: TranslatableText.kt
 * Description: Component file for TranslatableText.kt.
 */

package com.sameerasw.essentials.translation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.translation.ui.TranslationFocusOutline
import com.sameerasw.essentials.translation.ui.TranslationLongPressMenu
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun TranslatableText(
    stringResId: Int,
    modifier: Modifier = Modifier,
    formatArgs: List<Any> = emptyList(),
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val context = LocalContext.current
    val keyName =
        remember(stringResId) {
            try {
                context.resources.getResourceEntryName(stringResId)
            } catch (e: Exception) {
                "unknown"
            }
        }

    val rawText = stringResource(id = stringResId)
    val currentLocale = remember { context.resources.configuration.locales[0].language }

    val override = TranslationManager.liveOverrides[Pair(keyName, currentLocale)]
    val displayText =
        remember(keyName, currentLocale, rawText, override, formatArgs) {
            val base = TranslationManager.getOverriddenText(keyName, currentLocale, rawText)
            if (formatArgs.isEmpty()) {
                base
            } else {
                val locale = context.resources.configuration.locales[0]
                try {
                    String.format(locale, base, *formatArgs.toTypedArray())
                } catch (e: Exception) {
                    String.format(locale, rawText, *formatArgs.toTypedArray())
                }
            }
        }

    TranslatableTextHost(keyName, displayText, currentLocale, modifier) { textModifier ->
        Text(
            text = displayText,
            modifier = textModifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
    }
}

@Composable
fun TranslatableText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val context = LocalContext.current
    val isModeEnabled by TranslationManager.isTranslationModeEnabled
    val currentLocale = remember { context.resources.configuration.locales[0].language }
    val keyName =
        if (isModeEnabled) {
            remember(text) { TranslationManager.resolveKey(context, text) }
        } else {
            null
        }
    val displayText = keyName?.let { TranslationManager.liveOverrides[Pair(it, currentLocale)] } ?: text

    TranslatableTextHost(keyName, displayText, currentLocale, modifier) { textModifier ->
        Text(
            text = displayText,
            modifier = textModifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = style,
        )
    }
}

@Composable
private fun TranslatableTextHost(
    keyName: String?,
    displayText: String,
    currentLocale: String,
    modifier: Modifier,
    renderText: @Composable (Modifier) -> Unit,
) {
    val view = LocalView.current
    val isModeEnabled by TranslationManager.isTranslationModeEnabled

    if (!isModeEnabled || keyName == null || !TranslationManager.isTranslatableKey(keyName)) {
        renderText(modifier)
        return
    }

    var showMenu by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var initialTargetLocaleForSheet by remember { mutableStateOf<String?>(null) }

    // Initial pass so a long press opens the menu without swallowing taps meant for a parent button.
    Box(
        modifier =
            modifier.pointerInput(keyName) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    val upOrCancel =
                        withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        }
                    if (upOrCancel == null) {
                        down.consume()
                        HapticUtil.performHeavyHaptic(view)
                        TranslationManager.activeTargetKey.value = keyName
                        TranslationManager.activeTargetText.value = displayText
                        showMenu = true
                    }
                }
            },
        propagateMinConstraints = true,
    ) {
        TranslationFocusOutline(visible = showMenu) {
            renderText(Modifier)
        }

        if (showMenu) {
            TranslationLongPressMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                stringKey = keyName,
                fullText = displayText,
                onTranslateClick = {
                    showMenu = false
                    initialTargetLocaleForSheet = currentLocale
                    showBottomSheet = true
                },
                onViewAllClick = {
                    showMenu = false
                    initialTargetLocaleForSheet = null
                    showBottomSheet = true
                },
            )
        }
    }

    if (showBottomSheet) {
        TranslationBottomSheet(
            stringKey = keyName,
            initialTargetLocale = initialTargetLocaleForSheet,
            onDismissRequest = { showBottomSheet = false },
        )
    }
}
