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
import com.sameerasw.essentials.domain.model.NotificationLightingStyle
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationLightingStylePicker(
    selectedStyle: NotificationLightingStyle,
    onStyleSelected: (NotificationLightingStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    val styles = listOf(
        NotificationLightingStyle.STROKE,
        NotificationLightingStyle.GLOW,
        NotificationLightingStyle.INDICATOR
    )
    val icons = listOf(
        R.drawable.rounded_rounded_corner_24,
        R.drawable.rounded_blur_linear_24,
        R.drawable.rounded_circles_24
    )
    val view = LocalView.current

    val selectedIndex = styles.indexOf(selectedStyle).coerceAtLeast(0)

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
            val modifiers = List(styles.size) { Modifier.weight(1f) }

            styles.forEachIndexed { index, style ->
                ToggleButton(
                    checked = selectedIndex == index,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onStyleSelected(style)
                    },
                    modifier = modifiers[index].semantics { role = Role.RadioButton },
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        styles.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) {
                    Icon(
                        painter = painterResource(id = icons[index]),
                        contentDescription = style.name,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
