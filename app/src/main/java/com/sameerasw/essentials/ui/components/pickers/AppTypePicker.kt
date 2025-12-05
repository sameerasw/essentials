package com.sameerasw.essentials.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class AppType {
    DOWNLOADED,
    SYSTEM;

    val label: String
        get() = when (this) {
            DOWNLOADED -> "Downloaded"
            SYSTEM -> "System"
        }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTypePicker(
    selectedType: AppType,
    onTypeSelected: (AppType) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("Downloaded", "System")
    val types = listOf(AppType.DOWNLOADED, AppType.SYSTEM)

    Row(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
            )
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        val modifiers = List(labels.size) { Modifier.weight(1f) }

        labels.forEachIndexed { index, label ->
            val type = types[index]
            val isSelected = selectedType == type

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { checked ->
                    if (checked) {
                        onTypeSelected(type)
                    }
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
