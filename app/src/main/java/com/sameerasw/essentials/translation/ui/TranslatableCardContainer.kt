package com.sameerasw.essentials.translation.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.utils.HapticUtil
import kotlinx.coroutines.withTimeoutOrNull

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

    var showMenu by remember { mutableStateOf(false) }
    var activeKeyForSheet by remember { mutableStateOf<String?>(null) }
    var resolvedKeyTitle by remember { mutableStateOf<String?>(null) }
    var resolvedKeyDesc by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = if (isTranslationModeActive) {
            modifier.pointerInput(title, description) {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Initial)
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    val upOrCancel = withTimeoutOrNull(longPressTimeout) {
                        waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    }
                    if (upOrCancel == null) {
                        // User held down longer than long-press threshold -> open translation menu
                        down.consume()
                        HapticUtil.performHeavyHaptic(view)
                        resolvedKeyTitle = TranslationManager.resolveKey(context, title)
                        resolvedKeyDesc = TranslationManager.resolveKey(context, description)
                        showMenu = true
                    }
                }
            }
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
                val keyT = resolvedKeyTitle
                val keyD = resolvedKeyDesc

                if (keyT != null) {
                    SegmentedDropdownMenuItem(
                        text = { Text("Translate Title ($keyT)") },
                        onClick = {
                            showMenu = false
                            activeKeyForSheet = keyT
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_translate_24),
                                contentDescription = null
                            )
                        }
                    )
                }

                if (keyD != null) {
                    SegmentedDropdownMenuItem(
                        text = { Text("Translate Description ($keyD)") },
                        onClick = {
                            showMenu = false
                            activeKeyForSheet = keyD
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_translate_24),
                                contentDescription = null
                            )
                        }
                    )
                }

                if (keyT == null && keyD == null) {
                    SegmentedDropdownMenuItem(
                        text = { Text("No string key found") },
                        onClick = { showMenu = false }
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
