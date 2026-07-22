package com.sameerasw.essentials.translation.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem

@Composable
fun TranslationMenuItems(
    title: Any?,
    description: Any? = null,
    onSelectKey: (String) -> Unit
) {
    val context = LocalContext.current
    val keyTitle = remember(title) { TranslationManager.resolveKey(context, title) }
    val keyDesc = remember(description) { TranslationManager.resolveKey(context, description) }

    if (keyTitle != null) {
        SegmentedDropdownMenuItem(
            text = { Text("Translate Title ($keyTitle)") },
            onClick = { onSelectKey(keyTitle) },
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
            onClick = { onSelectKey(keyDesc) },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_translate_24),
                    contentDescription = null
                )
            }
        )
    }

    if (keyTitle == null && keyDesc == null) {
        SegmentedDropdownMenuItem(
            text = { Text("No string key found") },
            onClick = {}
        )
    }
}
