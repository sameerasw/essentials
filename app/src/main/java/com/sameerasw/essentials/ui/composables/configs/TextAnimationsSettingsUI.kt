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
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.sameerasw.essentials.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TextAnimationsSettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        RoundedCardContainer(spacing = 2.dp) {
            SegmentedPicker(
                items = listOf("default", "glove"),
                selectedItem = viewModel.scaleAnimationsMode.value,
                onItemSelected = { viewModel.switchScaleAnimationsMode(it) },
                labelProvider = {
                    when (it) {
                        "glove" -> context.getString(R.string.label_mode_glove)
                        else -> context.getString(R.string.label_mode_default)
                    }
                },
                iconProvider = {
                    when (it) {
                        "glove" -> Icon(painterResource(R.drawable.round_front_hand_24), null)
                        else -> Icon(painterResource(R.drawable.rounded_front_hand_24), null)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        RoundedCardContainer {
            com.sameerasw.essentials.ui.components.cards.IconToggleItem(
                title = stringResource(R.string.label_increase_touch_sensitivity),
                subtitle = stringResource(R.string.desc_increase_touch_sensitivity),
                icon = R.drawable.rounded_touch_app_24,
                checked = viewModel.isTouchSensitivityEnabled.value,
                onCheckedChange = { viewModel.setTouchSensitivityEnabled(it) }
            )
        }

        // Text Section
        Text(
            text = stringResource(R.string.settings_section_text),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            ConfigSliderItem(
                title = stringResource(R.string.label_font_scale),
                value = viewModel.fontScale.floatValue,
                onValueChange = {
                    viewModel.updateFontScale(it)
                    HapticUtil.performSliderHaptic(view)
                },
                onValueChangeFinished = {
                    viewModel.saveFontScale()
                },
                valueRange = 0.25f..5.0f,
                steps = 0,
                increment = 0.05f,
                valueFormatter = { String.format("%.2fx", it) }
            )

            ConfigSliderItem(
                title = stringResource(R.string.label_font_weight),
                value = viewModel.fontWeight.intValue.toFloat(),
                onValueChange = {
                    viewModel.setFontWeight(it.toInt())
                    HapticUtil.performSliderHaptic(view)
                },
                valueRange = 0f..500f,
                steps = 10,
                increment = 10f,
                valueFormatter = { it.toInt().toString() }
            )

            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        viewModel.resetTextToDefault()
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
            }
        }

        // Scale Section
        Text(
            text = stringResource(R.string.settings_section_scale),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            val isEnabled = viewModel.hasShizukuPermission.value
            
            ConfigSliderItem(
                title = stringResource(R.string.label_smallest_width),
                value = viewModel.smallestWidth.intValue.toFloat(),
                onValueChange = {
                    viewModel.updateSmallestWidth(it.toInt())
                    HapticUtil.performSliderHaptic(view)
                },
                onValueChangeFinished = {
                    viewModel.saveSmallestWidth()
                },
                valueRange = 300f..1000f,
                steps = 0,
                increment = 10f,
                valueFormatter = { String.format("%d dp", it.toInt()) },
                enabled = isEnabled
            )

            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = if (isEnabled) Arrangement.End else Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isEnabled) {
                    Button(
                        onClick = {
                            viewModel.resetScaleToDefault()
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
                        text = stringResource(R.string.msg_shizuku_permission_required),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
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

        // Animations Section
        Text(
            text = stringResource(R.string.settings_section_animations),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RoundedCardContainer(spacing = 2.dp) {
            ConfigSliderItem(
                title = stringResource(R.string.label_animator_duration_scale),
                value = viewModel.animatorDurationScale.floatValue,
                onValueChange = {
                    viewModel.setAnimationScale(android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, it)
                    HapticUtil.performSliderHaptic(view)
                },
                valueRange = 0f..10f,
                steps = 0,
                increment = 0.05f,
                valueFormatter = { String.format("%.2fx", it) }
            )

            ConfigSliderItem(
                title = stringResource(R.string.label_transition_animation_scale),
                value = viewModel.transitionAnimationScale.floatValue,
                onValueChange = {
                    viewModel.setAnimationScale(android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, it)
                    HapticUtil.performSliderHaptic(view)
                },
                valueRange = 0f..10f,
                steps = 0,
                increment = 0.05f,
                valueFormatter = { String.format("%.2fx", it) }
            )

            ConfigSliderItem(
                title = stringResource(R.string.label_window_animation_scale),
                value = viewModel.windowAnimationScale.floatValue,
                onValueChange = {
                    viewModel.setAnimationScale(android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, it)
                    HapticUtil.performSliderHaptic(view)
                },
                valueRange = 0f..10f,
                steps = 0,
                increment = 0.05f,
                valueFormatter = { String.format("%.2fx", it) }
            )

            Row(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(MaterialTheme.shapes.extraSmall.bottomEnd)
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        viewModel.resetAnimationsToDefault()
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
            }
        }
    }
}
