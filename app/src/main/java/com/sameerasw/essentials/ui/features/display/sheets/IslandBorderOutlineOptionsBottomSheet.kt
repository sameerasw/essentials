package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.pickers.DUO_PRESET_COLORS
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

// Default gray first, then the shared preset palette.
private val OUTLINE_COLORS =
    listOf(SettingsRepository.ISLAND_BORDER_OUTLINE_DEFAULT_COLOR) + DUO_PRESET_COLORS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandBorderOutlineOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_border_outline_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_border_outline_thickness_title),
                    value = viewModel.islandBorderOutlineThickness.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandBorderOutlineThickness(it)
                    },
                    valueRange = 1f..5f,
                    increment = 1f,
                    iconRes = R.drawable.rounded_line_weight_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_visibility_off_24,
                    title = stringResource(R.string.island_border_outline_hide_expanded_title),
                    description = stringResource(R.string.island_border_outline_hide_expanded_desc),
                    isChecked = viewModel.isIslandBorderOutlineHiddenWhenExpanded.value,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandBorderOutlineHiddenWhenExpanded(it)
                    },
                )

                ColorSwatchPicker(
                    selectedColorHex = viewModel.islandBorderOutlineColor.value,
                    onColorSelected = { viewModel.setIslandBorderOutlineColor(it) },
                    colors = OUTLINE_COLORS,
                )
            }
        }
    }
}
