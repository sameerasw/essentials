package com.sameerasw.essentials.translation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem

@Composable
fun TranslationLongPressMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    stringKey: String,
    fullText: String,
    onTranslateClick: () -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp)
) {
    SegmentedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset
    ) {
        // Info Header Entry with background matching SegmentedDropdownMenuItem
        SegmentedDropdownMenuItem(
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        text = fullText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Key: $stringKey",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            onClick = {},
            enabled = false
        )

        // Action 1: Translate
        SegmentedDropdownMenuItem(
            text = { Text(stringResource(R.string.translation_menu_translate)) },
            onClick = onTranslateClick,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.rounded_translate_24),
                    contentDescription = null
                )
            }
        )

        // Action 2: View All
        SegmentedDropdownMenuItem(
            text = { Text(stringResource(R.string.translation_menu_view)) },
            onClick = onViewAllClick,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.rounded_visibility_24),
                    contentDescription = null
                )
            }
        )

        // Action 3: Dismiss
        SegmentedDropdownMenuItem(
            text = { Text(stringResource(R.string.translation_menu_dismiss)) },
            onClick = onDismissRequest,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.rounded_close_24),
                    contentDescription = null
                )
            }
        )
    }
}
