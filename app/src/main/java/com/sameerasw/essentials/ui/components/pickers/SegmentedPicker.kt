package com.sameerasw.essentials.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslationManager
import com.sameerasw.essentials.translation.ui.TranslationBottomSheet
import com.sameerasw.essentials.translation.ui.TranslationMenuItems
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenu
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> SegmentedPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    iconProvider: (@Composable (T) -> Unit)? = null,
    modifier: Modifier = Modifier,
    cornerShape: CornerSize = MaterialTheme.shapes.extraSmall.bottomEnd,
    containerColor: Color = MaterialTheme.colorScheme.surfaceBright,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    title: Any? = null,
    description: Any? = null
) {
    val view = LocalView.current
    val isTranslationModeActive by TranslationManager.isTranslationModeEnabled

    var activeMenuIndex by remember { mutableStateOf<Int?>(null) }
    var translationSheetKey by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(cornerShape))
            .background(color = containerColor)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        val modifiers = List(items.size) { Modifier.weight(1f) }

        items.forEachIndexed { index, item ->
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
                    checked = selectedItem == item,
                    onCheckedChange = {
                        HapticUtil.performUIHaptic(view)
                        onItemSelected(item)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { role = Role.RadioButton },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (iconProvider != null) {
                            iconProvider(item)
                            Spacer(Modifier.padding(end = 8.dp))
                        }
                        Text(
                            label,
                            fontSize = dimensionResource(R.dimen.font_small).value.sp,
                            modifier = Modifier.basicMarquee(),
                            maxLines = 1
                        )
                    }
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
