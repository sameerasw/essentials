package com.sameerasw.essentials.ui.components.buttons

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.HapticUtil

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.translation.ui.TranslationMenuItems
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListExpandToggleButton(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    title: Any = R.string.action_show_top_apps,
    description: Any? = R.string.action_show_all,
    expandedText: String? = null,
    collapsedText: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled

    var showMenu by remember { mutableStateOf(false) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    val rotationDegree by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "list_expand_chevron_rotation"
    )

    val expText = expandedText ?: when (title) {
        is Int -> stringResource(title)
        is String -> title
        else -> title.toString()
    }
    val colText = collapsedText ?: when (description) {
        is Int -> stringResource(description)
        is String -> description
        null -> expText
        else -> description.toString()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = if (isTranslationModeActive) {
                    {
                        HapticUtil.performVirtualKeyHaptic(view)
                        showMenu = true
                    }
                } else null
            )
    ) {
        Button(
            onClick = {
                HapticUtil.performVirtualKeyHaptic(view)
                onToggle()
            },
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { rotationZ = rotationDegree }
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (isExpanded) expText else colText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        SegmentedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            TranslationMenuItems(
                title = title,
                description = description,
                onSelectKey = { key ->
                    showMenu = false
                    translationSheetKey = key
                }
            )
        }
    }

    val targetKey = translationSheetKey
    if (targetKey != null) {
        val resolvedKey = remember(targetKey) {
            TranslationManager.resolveKey(context, targetKey) ?: targetKey
        }
        TranslationBottomSheet(
            stringKey = resolvedKey,
            onDismissRequest = { translationSheetKey = null }
        )
    }
}
