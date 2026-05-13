package com.sameerasw.essentials.ui.composables.configs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.RefreshRateUtils
import com.sameerasw.essentials.viewmodels.MainViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshRateSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isEnabled = viewModel.isShizukuPermissionGranted.value
    val isFixedMode = viewModel.refreshRateMode.value == RefreshRateUtils.MODE_FIXED
    val systemLabel = stringResource(R.string.refresh_rate_system_default)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.refresh_rate_section_mode),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            SegmentedPicker(
                items = listOf(RefreshRateUtils.MODE_FIXED, RefreshRateUtils.MODE_RANGE),
                selectedItem = viewModel.refreshRateMode.value,
                onItemSelected = { viewModel.setRefreshRateMode(it) },
                labelProvider = {
                    when (it) {
                        RefreshRateUtils.MODE_RANGE -> context.getString(R.string.refresh_rate_mode_range)
                        else -> context.getString(R.string.refresh_rate_mode_fixed)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = stringResource(R.string.refresh_rate_section_values),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            if (isFixedMode) {
                ConfigSliderItem(
                    title = stringResource(R.string.refresh_rate_fixed_title),
                    description = stringResource(R.string.refresh_rate_fixed_desc),
                    value = viewModel.fixedRefreshRate.floatValue,
                    onValueChange = {
                        viewModel.updateFixedRefreshRate(it.roundToInt().toFloat())
                        HapticUtil.performSliderHaptic(view)
                    },
                    onValueChangeFinished = {
                        viewModel.applyFixedRefreshRate(context)
                    },
                    valueRange = 0f..120f,
                    steps = 11,
                    increment = 10f,
                    valueFormatter = { formatRefreshRateLabel(it, systemLabel) },
                    icon = R.drawable.rounded_shutter_speed_24,
                    enabled = isEnabled
                )
            } else {
                ConfigSliderItem(
                    title = stringResource(R.string.refresh_rate_min_title),
                    description = stringResource(R.string.refresh_rate_min_desc),
                    value = viewModel.minRefreshRate.floatValue,
                    onValueChange = {
                        viewModel.updateMinRefreshRate(it.roundToInt().toFloat())
                        HapticUtil.performSliderHaptic(view)
                    },
                    onValueChangeFinished = {
                        viewModel.applyRefreshRateRange(context)
                    },
                    valueRange = 0f..120f,
                    steps = 11,
                    increment = 10f,
                    valueFormatter = { formatRefreshRateLabel(it, systemLabel) },
                    icon = R.drawable.rounded_keyboard_arrow_down_24,
                    enabled = isEnabled
                )

                ConfigSliderItem(
                    title = stringResource(R.string.refresh_rate_peak_title),
                    description = stringResource(R.string.refresh_rate_peak_desc),
                    value = viewModel.peakRefreshRate.floatValue,
                    onValueChange = {
                        viewModel.updatePeakRefreshRate(it.roundToInt().toFloat())
                        HapticUtil.performSliderHaptic(view)
                    },
                    onValueChangeFinished = {
                        viewModel.applyRefreshRateRange(context)
                    },
                    valueRange = 0f..120f,
                    steps = 11,
                    increment = 10f,
                    valueFormatter = { formatRefreshRateLabel(it, systemLabel) },
                    icon = R.drawable.rounded_keyboard_arrow_up_24,
                    enabled = isEnabled
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = if (isEnabled) Arrangement.SpaceBetween else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEnabled) {
                    Text(
                        text = stringResource(R.string.refresh_rate_reset_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.resetRefreshRate(context)
                            HapticUtil.performSliderHaptic(view)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_reset_default),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.msg_refresh_rate_permission_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.requestShizukuPermission()
                            HapticUtil.performSliderHaptic(view)
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_grant_permission),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatRefreshRateLabel(value: Float, systemLabel: String): String {
    return if (value <= 0f) {
        systemLabel
    } else {
        "${value.roundToInt()} Hz"
    }
}
