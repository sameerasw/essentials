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
import com.sameerasw.essentials.domain.model.EdgeLightingStyle
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EdgeLightingStylePicker(
    selectedStyle: EdgeLightingStyle,
    onStyleSelected: (EdgeLightingStyle) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Pair<String, EdgeLightingStyle>> = listOf(
        "Stroke" to EdgeLightingStyle.STROKE,
        "Glow" to EdgeLightingStyle.GLOW
    )
) {
    val labels = options.map { it.first }
    val styles = options.map { it.second }

    val selectedIndex = styles.indexOf(selectedStyle).coerceAtLeast(0)

    RoundedCardContainer(modifier = Modifier){
        Row(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val modifiers = List(labels.size) { Modifier.weight(1f) }

            labels.forEachIndexed { index, label ->
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = {
                        onStyleSelected(styles[index])
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
}
