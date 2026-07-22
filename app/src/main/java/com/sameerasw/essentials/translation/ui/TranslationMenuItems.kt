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
    options: List<Any> = emptyList(),
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

    options.forEach { option ->
        val keyOpt = remember(option) { TranslationManager.resolveKey(context, option) }
        val labelOpt = remember(option) {
            when (option) {
                is Int -> try { context.getString(option) } catch (e: Exception) { option.toString() }
                is String -> option
                else -> option.toString()
            }
        }
        if (keyOpt != null && keyOpt != keyTitle && keyOpt != keyDesc) {
            SegmentedDropdownMenuItem(
                text = { Text("Translate Option '$labelOpt' ($keyOpt)") },
                onClick = { onSelectKey(keyOpt) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_translate_24),
                        contentDescription = null
                    )
                }
            )
        }
    }

    val hasAnyKey = keyTitle != null || keyDesc != null || options.any { TranslationManager.resolveKey(context, it) != null }
    if (!hasAnyKey) {
        SegmentedDropdownMenuItem(
            text = { Text("No string key found") },
            onClick = {}
        )
    }
}
