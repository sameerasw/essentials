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
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.core.sheets.EssentialsBottomSheet
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslandPulseShadowOptionsBottomSheet(
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
                text = stringResource(R.string.island_pulse_shadow_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )

            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigSliderItem(
                    title = stringResource(R.string.island_pulse_shadow_blur_title),
                    value = viewModel.islandPulseShadowBlur.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandPulseShadowBlur(it)
                    },
                    valueRange = 0f..48f,
                    increment = 1f,
                    iconRes = R.drawable.rounded_blur_on_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_pulse_shadow_spread_title),
                    value = viewModel.islandPulseShadowSpread.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandPulseShadowSpread(it)
                    },
                    valueRange = 0f..24f,
                    increment = 1f,
                    iconRes = R.drawable.rounded_magnify_fullscreen_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_pulse_shadow_offset_y_title),
                    value = viewModel.islandPulseShadowOffsetY.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandPulseShadowOffsetY(it)
                    },
                    valueRange = -8f..16f,
                    increment = 1f,
                    iconRes = R.drawable.rounded_screenshot_region_24,
                    valueFormatter = { "${it.toInt()} dp" },
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_pulse_shadow_opacity_title),
                    value = viewModel.islandPulseShadowOpacity.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandPulseShadowOpacity(it)
                    },
                    valueRange = 0.1f..1f,
                    increment = 0.02f,
                    iconRes = R.drawable.rounded_invert_colors_24,
                    valueFormatter = { "${(it * 100).roundToInt()}%" },
                )

                ConfigSliderItem(
                    title = stringResource(R.string.island_pulse_shadow_duration_title),
                    value = viewModel.islandPulseShadowDurationMs.floatValue,
                    onValueChange = {
                        HapticUtil.performUIHaptic(view)
                        viewModel.setIslandPulseShadowDurationMs(it)
                    },
                    valueRange = 300f..4000f,
                    increment = 50f,
                    iconRes = R.drawable.rounded_motion_play_24,
                    valueFormatter = { "${it.toInt()} ms" },
                )
            }
        }
    }
}
