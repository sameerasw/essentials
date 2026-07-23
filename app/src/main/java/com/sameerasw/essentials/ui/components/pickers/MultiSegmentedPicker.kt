package com.sameerasw.essentials.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.translation.ui.TranslationMenuItems
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> MultiSegmentedPicker(
    items: List<T>,
    selectedItems: Set<T>,
    onItemsSelected: (Set<T>) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    allowEmpty: Boolean = true,
    title: Any? = null,
    description: Any? = null
) {
    val view = LocalView.current
    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled

    var activeMenuIndex by remember { mutableStateOf<Int?>(null) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier
            .clip(shape)
            .background(color = MaterialTheme.colorScheme.surfaceBright)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        val modifiers = List(items.size) { Modifier.weight(1f) }

        items.forEachIndexed { index, item ->
            val isSelected = selectedItems.contains(item)
            val label = labelProvider(item)

            val boxModifier = if (isTranslationModeActive) {
                modifiers[index].pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Initial)
                        val downTime = System.currentTimeMillis()
                        var longPressed = false
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            if (System.currentTimeMillis() - downTime >= 400L && !longPressed) {
                                longPressed = true
                                change.consume()
                                HapticUtil.performHeavyHaptic(view)
                                activeMenuIndex = index
                            }
                        }
                        if (longPressed) {
                            currentEvent.changes.forEach { it.consume() }
                        }
                    }
                }
            } else {
                modifiers[index]
            }

            Box(modifier = boxModifier) {
                ToggleButton(
                    checked = isSelected,
                    onCheckedChange = { checked ->
                        HapticUtil.performUIHaptic(view)
                        val newSelection = if (checked) {
                            selectedItems + item
                        } else {
                            if (allowEmpty || selectedItems.size > 1) selectedItems - item else selectedItems
                        }
                        onItemsSelected(newSelection)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.Checkbox },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Text(label)
                }

                if (activeMenuIndex == index) {
                    SegmentedDropdownMenu(
                        expanded = activeMenuIndex == index,
                        onDismissRequest = { activeMenuIndex = null }
                    ) {
                        TranslationMenuItems(
                            title = title ?: label,
                            description = description,
                            options = items.map { labelProvider(it) },
                            onSelectKey = { key ->
                                activeMenuIndex = null
                                translationSheetKey = key
                            }
                        )
                    }
                }
            }
        }
    }

    if (translationSheetKey != null) {
        TranslationBottomSheet(
            stringKey = translationSheetKey!!,
            onDismissRequest = { translationSheetKey = null }
        )
    }
}
