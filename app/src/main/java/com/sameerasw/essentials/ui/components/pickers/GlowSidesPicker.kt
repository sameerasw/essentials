package com.sameerasw.essentials.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.EdgeLightingSide
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import androidx.compose.ui.platform.LocalView
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GlowSidesPicker(
    selectedSides: Set<EdgeLightingSide>,
    onSideToggled: (EdgeLightingSide, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        EdgeLightingSide.LEFT to R.drawable.rounded_border_left_24,
        EdgeLightingSide.TOP to R.drawable.rounded_border_top_24,
        EdgeLightingSide.RIGHT to R.drawable.rounded_border_right_24,
        EdgeLightingSide.BOTTOM to R.drawable.rounded_border_bottom_24
    )
    val view = LocalView.current

        Row(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val modifiers = List(options.size) { Modifier.weight(1f) }

            options.forEachIndexed { index, (side, iconRes) ->
                ToggleButton(
                    checked = selectedSides.contains(side),
                    onCheckedChange = { checked ->
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSideToggled(side, checked)
                    },
                    modifier = modifiers[index].semantics { role = Role.Checkbox },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = side.name,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
}
