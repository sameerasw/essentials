package com.sameerasw.essentials.ui.components.pickers

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.ScreenOffMethod
import com.sameerasw.essentials.utils.HapticUtil

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScreenOffMethodPicker(
    selectedMethod: ScreenOffMethod,
    onMethodSelected: (ScreenOffMethod) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Pair<Int, ScreenOffMethod>> = listOf(
        R.string.screen_off_method_accessibility to ScreenOffMethod.ACCESSIBILITY,
        R.string.screen_off_method_input to ScreenOffMethod.INPUT
    )
) {
    val labels = options.map { it.first }
    val types = options.map { it.second }

    val selectedIndex = types.indexOf(selectedMethod).coerceAtLeast(0)
    val view = LocalView.current // Get the current View for haptic feedback

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
                    onMethodSelected(types[index])
                    HapticUtil.performLightHaptic(view) // Trigger haptic feedback
                },
                modifier = modifiers[index].semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
            ) {
                Text(
                    stringResource(label),
                    fontSize = dimensionResource(R.dimen.font_small).value.sp,
                    modifier = Modifier.basicMarquee(),
                    maxLines = 1
                )
            }
        }
    }
}
