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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.model.NotificationLightingColorMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationLightingColorModePicker(
    selectedMode: NotificationLightingColorMode,
    onModeSelected: (NotificationLightingColorMode) -> Unit,
    modifier: Modifier = Modifier,
    options: List<Pair<Int, NotificationLightingColorMode>> = listOf(
        R.string.color_mode_system to NotificationLightingColorMode.SYSTEM,
        R.string.color_mode_custom to NotificationLightingColorMode.CUSTOM,
        R.string.color_mode_app_specific to NotificationLightingColorMode.APP_SPECIFIC
    )
) {
    val labels = options.map { stringResource(it.first) }
    val modes = options.map { it.second }

    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)

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
                    onModeSelected(modes[index])
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
