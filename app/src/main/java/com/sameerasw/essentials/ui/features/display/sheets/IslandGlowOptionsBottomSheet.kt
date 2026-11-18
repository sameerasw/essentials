package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.IconToggleItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.pickers.ColorSwatchPicker
import com.sameerasw.essentials.ui.core.pickers.DUO_PRESET_COLORS
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

private val GLOW_COLORS = DUO_PRESET_COLORS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandGlowOptionsBottomSheet(
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
            TranslatableText(
                stringResId = R.string.island_show_glow_title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_glow_intensity_title),
                    value = viewModel.islandGlowIntensity.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandGlowIntensity(it)
                    },
                    valueRange = 0.1f..2f,
                    increment = 0.1f,
                    iconRes = R.drawable.rounded_blur_on_24,
                    valueFormatter = { "%.1fx".format(it) },
                    allowDecimals = true,
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_glow_size_title),
                    value = viewModel.islandGlowSize.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandGlowSize(it)
                    },
                    valueRange = 0.25f..2f,
                    increment = 0.05f,
                    iconRes = R.drawable.rounded_blur_on_24,
                    valueFormatter = { "%.2fx".format(it) },
                    allowDecimals = true,
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_line_weight_24,
                    title = stringResource(R.string.island_glow_rim_title),
                    isChecked = viewModel.isIslandGlowRim.value,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandGlowRim(it)
                    },
                )

                IconToggleItem(
                    iconRes = R.drawable.rounded_palette_24,
                    title = stringResource(R.string.island_border_outline_dynamic_title),
                    isChecked = viewModel.isIslandGlowDynamic.value,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        viewModel.setIslandGlowDynamic(it)
                    },
                )

                AnimatedVisibility(
                    visible = !viewModel.isIslandGlowDynamic.value,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ColorSwatchPicker(
                        selectedColorHex = viewModel.islandGlowColor.value,
                        onColorSelected = { viewModel.setIslandGlowColor(it) },
                        colors = GLOW_COLORS,
                    )
                }
            }
        }
    }
}
