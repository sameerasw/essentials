package com.sameerasw.essentials.ui.features.display.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandExpandedOptionsBottomSheet(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
) {
    val view = LocalView.current

    EssentialsBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.island_expanded_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_expanded_width_title),
                    value = viewModel.islandExpandedWidth.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandExpandedWidth(it)
                    },
                    valueRange = 200f..500f,
                    increment = 10f,
                    iconRes = R.drawable.rounded_arrows_outward_24,
                    valueFormatter = { "%.1f dp".format(it) },
                    allowDecimals = true,
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_expanded_scale_title),
                    description = stringResource(R.string.island_expanded_scale_desc),
                    value = viewModel.islandExpandedScale.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandExpandedScale(it)
                    },
                    valueRange = 1f..1.3f,
                    increment = 0.02f,
                    iconRes = R.drawable.rounded_magnify_fullscreen_24,
                    valueFormatter = { "${(it * 100).toInt()}%" },
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_expanded_roundness_title),
                    value = viewModel.islandExpandedRoundness.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandExpandedRoundness(it)
                    },
                    valueRange = 0f..80f,
                    increment = 2f,
                    iconRes = R.drawable.rounded_rounded_corner_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )
            }

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_expanded_padding_title),
                    value = viewModel.islandExpandedPadding.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandExpandedPadding(it)
                    },
                    valueRange = 8f..48f,
                    increment = 2f,
                    iconRes = R.drawable.rounded_screenshot_region_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_expanded_top_padding_title),
                    value = viewModel.islandExpandedTopPadding.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandExpandedTopPadding(it)
                    },
                    valueRange = 0f..40f,
                    increment = 2f,
                    iconRes = R.drawable.rounded_vertical_align_top_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_expanded_bottom_padding_title),
                    value = viewModel.islandExpandedBottomPadding.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandExpandedBottomPadding(it)
                    },
                    valueRange = 0f..40f,
                    increment = 2f,
                    iconRes = R.drawable.rounded_vertical_align_bottom_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )
            }
        }
    }
}
