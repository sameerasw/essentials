package com.sameerasw.essentials.ui.features.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandHideIconsToggle(
    viewModel: MainViewModel,
    hasShellPermission: Boolean,
    onNeedPermission: () -> Unit,
    highlighted: Boolean,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var enabled by remember { mutableStateOf(settings.getBoolean(SettingsRepository.KEY_ISLAND_HIDE_ICONS_ENABLED, false)) }
    var selected by remember { mutableStateOf(settings.getIslandHideIcons()) }

    IconToggleItem(
        iconRes = R.drawable.rounded_visibility_off_24,
        title = stringResource(R.string.island_hide_icons_title),
        isChecked = enabled && hasShellPermission,
        onCheckedChange = { checked ->
            HapticUtil.performVirtualKeyHaptic(view)
            if (checked && !hasShellPermission) {
                onNeedPermission()
            } else {
                enabled = checked
                settings.putBoolean(SettingsRepository.KEY_ISLAND_HIDE_ICONS_ENABLED, checked)
            }
        },
    )

    if (enabled && hasShellPermission) {
        val options =
            listOf(
                SettingsRepository.ISLAND_HIDE_ICON_SYSTEM to R.string.island_hide_icon_system,
                SettingsRepository.ISLAND_HIDE_ICON_NOTIFICATIONS to R.string.island_hide_icon_notifications,
                SettingsRepository.ISLAND_HIDE_ICON_CLOCK to R.string.island_hide_icon_clock,
            )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceBright, Shapes.extraSmall)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEach { (id, label) ->
                    FilterChip(
                        selected = id in selected,
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            selected = if (id in selected) selected - id else selected + id
                            settings.setIslandHideIcons(selected)
                        },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
    }
}
