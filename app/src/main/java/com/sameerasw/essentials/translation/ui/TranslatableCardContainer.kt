package com.sameerasw.essentials.translation.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranslatableCardContainer(
    title: Any?,
    description: Any? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled

    val keyTitle = remember(title) { TranslationManager.resolveKey(context, title) }
    val keyDesc = remember(description) { TranslationManager.resolveKey(context, description) }

    var showMenu by remember { mutableStateOf(false) }
    var activeKeyForSheet by remember { mutableStateOf<String?>(null) }

    val hasTranslatableKeys = isTranslationModeActive && (keyTitle != null || keyDesc != null)

    Box(
        modifier = if (hasTranslatableKeys) {
            modifier.combinedClickable(
                onClick = {},
                onLongClick = {
                    HapticUtil.performHeavyHaptic(view)
                    showMenu = true
                }
            )
        } else {
            modifier
        }
    ) {
        TranslationFocusOutline(visible = showMenu) {
            content()
        }

        if (showMenu) {
            SegmentedDropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (keyTitle != null) {
                    SegmentedDropdownMenuItem(
                        text = { Text("Translate Title ($keyTitle)") },
                        onClick = {
                            showMenu = false
                            activeKeyForSheet = keyTitle
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_translate_24),
                                contentDescription = null
                            )
                        }
                    )
                }

                if (keyDesc != null) {
                    SegmentedDropdownMenuItem(
                        text = { Text("Translate Description ($keyDesc)") },
                        onClick = {
                            showMenu = false
                            activeKeyForSheet = keyDesc
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_translate_24),
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }

    if (activeKeyForSheet != null) {
        TranslationBottomSheet(
            stringKey = activeKeyForSheet!!,
            onDismissRequest = { activeKeyForSheet = null }
        )
    }
}
