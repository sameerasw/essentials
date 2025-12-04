package com.sameerasw.essentials.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.utils.HapticFeedbackType

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HapticFeedbackPicker(
    selectedFeedback: HapticFeedbackType,
    onFeedbackSelected: (HapticFeedbackType) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("None", "Subtle", "Double", "Click")
    val types = listOf(
        HapticFeedbackType.NONE,
        HapticFeedbackType.SUBTLE,
        HapticFeedbackType.DOUBLE,
        HapticFeedbackType.CLICK
    )

    var selectedIndex by remember {
        mutableIntStateOf(types.indexOf(selectedFeedback))
    }

    Row(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        val modifiers = List(labels.size) { Modifier.weight(1f) }

        labels.forEachIndexed { index, label ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = {
                    selectedIndex = index
                    onFeedbackSelected(types[index])
                },
                modifier = modifiers[index].semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(label)
            }
        }
    }
}


