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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationLightingSystemModePicker(
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = listOf(0, 1, 2) // 0: Charging, 1: Auth, 2: Custom
    val icons = listOf(
        R.drawable.rounded_keyboard_double_arrow_up_24,
        R.drawable.rounded_center_focus_strong_24,
        R.drawable.rounded_my_location_24
    )
    val labels = listOf("Charging", "Auth", "Custom") // For semantics/descriptions

    val view = LocalView.current

    RoundedCardContainer(modifier = Modifier) {
        Row(
            modifier = modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            val modifiers = List(modes.size) { Modifier.weight(1f) }

            modes.forEachIndexed { index, mode ->
                ToggleButton(
                    checked = selectedMode == mode,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onModeSelected(mode)
                    },
                    modifier = modifiers[index].semantics { role = Role.RadioButton },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = icons[index]),
                        contentDescription = labels[index],
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
