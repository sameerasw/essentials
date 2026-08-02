package com.sameerasw.essentials.translation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.translation.ui.TranslationFocusOutline
import com.sameerasw.essentials.translation.ui.TranslationLongPressMenu
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranslatableText(
    stringResId: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val context = LocalContext.current
    val view = LocalView.current

    val keyName = remember(stringResId) {
        try {
            context.resources.getResourceEntryName(stringResId)
        } catch (e: Exception) {
            "unknown"
        }
    }

    val defaultText = stringResource(id = stringResId)
    val currentLocale = remember { context.resources.configuration.locales[0].language }

    val displayText = remember(keyName, currentLocale, defaultText, TranslationManager.liveOverrides[Pair(keyName, currentLocale)]) {
        TranslationManager.getOverriddenText(keyName, currentLocale, defaultText)
    }

    val isModeEnabled by TranslationManager.isTranslationModeEnabled

    var showMenu by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var initialTargetLocaleForSheet by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = if (isModeEnabled) {
            modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    HapticUtil.performHeavyHaptic(view)
                    TranslationManager.activeTargetKey.value = keyName
                    TranslationManager.activeTargetText.value = displayText
                    showMenu = true
                }
            )
        } else {
            modifier
        }
    ) {
        TranslationFocusOutline(visible = isModeEnabled && showMenu) {
            Text(
                text = displayText,
                style = style,
                maxLines = maxLines,
                overflow = overflow
            )
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
                }
            )
        }
    }

    if (showBottomSheet) {
        TranslationBottomSheet(
            stringKey = keyName,
            initialTargetLocale = initialTargetLocaleForSheet,
            onDismissRequest = { showBottomSheet = false }
        )
    }
}
