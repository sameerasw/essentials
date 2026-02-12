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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil

enum class NetworkType {
    NETWORK_5G,
    NETWORK_4G,
    NETWORK_3G,
    NETWORK_OTHER;

}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NetworkTypePicker(
    selectedTypes: Set<NetworkType>,
    onTypesSelected: (Set<NetworkType>) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val labels = listOf(
        stringResource(R.string.network_type_5g),
        stringResource(R.string.network_type_4g),
        stringResource(R.string.network_type_3g),
        stringResource(R.string.network_type_other)
    )
    val types = listOf(
        NetworkType.NETWORK_5G,
        NetworkType.NETWORK_4G,
        NetworkType.NETWORK_3G,
        NetworkType.NETWORK_OTHER
    )

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
            val isSelected = selectedTypes.contains(type)

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { checked ->
                    HapticUtil.performUIHaptic(view)
                    val newSelection = if (checked) {
                        selectedTypes + type
                    } else {
                        selectedTypes - type
                    }
                    println("Selected Types: $newSelection , current mode: ")
                    onTypesSelected(newSelection)
                },
                modifier = modifiers[index].semantics { role = Role.Checkbox },
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

