package com.sameerasw.essentials.ui.composables.configs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.cards.IconToggleItem
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.components.pickers.SegmentedPicker
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.viewmodels.MainViewModel

@Composable
fun PowerAndBatterySettingsUI(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    highlightSetting: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isEnabled = viewModel.isWriteSecureSettingsEnabled.value

    val constants = viewModel.batterySaverConstants.value

    // Helpers to get current value with standard default fallbacks
    fun getBool(key: String, default: Boolean): Boolean {
        val raw = constants[key] ?: return default
        return raw.toBoolean()
    }

    fun getFloat(key: String, default: Float): Float {
        val raw = constants[key] ?: return default
        return raw.toFloatOrNull() ?: default
    }

    fun getInt(key: String, default: Int): Int {
        val raw = constants[key] ?: return default
        return raw.toIntOrNull() ?: default
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- SECTION: GENERAL ---
        Text(
            text = stringResource(R.string.title_battery_saver_general),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoundedCardContainer(spacing = 2.dp) {
            ConfigSliderItem(
                title = stringResource(R.string.label_low_power_trigger_level),
                description = stringResource(R.string.desc_low_power_trigger_level),
                value = viewModel.lowPowerTriggerLevel.intValue.toFloat(),
                onValueChange = {
                    viewModel.setLowPowerTriggerLevel(context, it.toInt())
                    HapticUtil.performSliderHaptic(view)
                },
                valueRange = 0f..100f,
                increment = 5f,
                valueFormatter = { "${it.toInt()}%" },
                iconRes = R.drawable.rounded_battery_charging_60_24,
                enabled = isEnabled
            )
            IconToggleItem(
                title = stringResource(R.string.label_advertise_is_enabled),
                description = stringResource(R.string.desc_advertise_is_enabled),
                isChecked = getBool("advertise_is_enabled", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "advertise_is_enabled", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_info_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_enable_firewall),
                description = stringResource(R.string.desc_enable_firewall),
                isChecked = getBool("enable_firewall", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "enable_firewall", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_security_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_force_all_apps_standby),
                description = stringResource(R.string.desc_force_all_apps_standby),
                isChecked = getBool("force_all_apps_standby", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "force_all_apps_standby", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_do_not_disturb_on_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_force_background_check),
                description = stringResource(R.string.desc_force_background_check),
                isChecked = getBool("force_background_check", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "force_background_check", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_task_alt_24
            )
        }

        // --- SECTION: DISPLAY & BRIGHTNESS ---
        Text(
            text = stringResource(R.string.title_battery_saver_display),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoundedCardContainer(spacing = 2.dp) {
            val brightnessAdjust = getBool("enable_brightness_adjustment", true)
            IconToggleItem(
                title = stringResource(R.string.label_enable_brightness_adjustment),
                description = stringResource(R.string.desc_enable_brightness_adjustment),
                isChecked = brightnessAdjust,
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "enable_brightness_adjustment", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_brightness_medium_24
            )
            AnimatedVisibility(
                visible = brightnessAdjust,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ConfigSliderItem(
                    title = stringResource(R.string.label_adjust_brightness_factor),
                    value = getFloat("adjust_brightness_factor", 0.8f) * 100f,
                    onValueChange = {
                        val factor = it / 100f
                        viewModel.updateBatterySaverConstant(context, "adjust_brightness_factor", factor.toString())
                        HapticUtil.performSliderHaptic(view)
                    },
                    valueRange = 10f..100f,
                    increment = 5f,
                    valueFormatter = { "${it.toInt()}%" },
                    iconRes = R.drawable.rounded_brightness_medium_24,
                    enabled = isEnabled
                )
            }
            IconToggleItem(
                title = stringResource(R.string.label_disable_animation),
                description = stringResource(R.string.desc_disable_animation),
                isChecked = getBool("disable_animation", false),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "disable_animation", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_blur_on_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_disable_aod),
                description = stringResource(R.string.desc_disable_aod),
                isChecked = getBool("disable_aod", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "disable_aod", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_mobile_off_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_enable_night_mode),
                description = stringResource(R.string.desc_enable_night_mode),
                isChecked = getBool("enable_night_mode", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "enable_night_mode", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_nightlight_24
            )
        }

        // --- SECTION: POWER & PERFORMANCE ---
        Text(
            text = stringResource(R.string.title_battery_saver_power),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                title = stringResource(R.string.label_disable_launch_boost),
                description = stringResource(R.string.desc_disable_launch_boost),
                isChecked = getBool("disable_launch_boost", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "disable_launch_boost", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_bolt_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_enable_quick_doze),
                description = stringResource(R.string.desc_enable_quick_doze),
                isChecked = getBool("enable_quick_doze", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "enable_quick_doze", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_nightlight_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_disable_optional_sensors),
                description = stringResource(R.string.desc_disable_optional_sensors),
                isChecked = getBool("disable_optional_sensors", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "disable_optional_sensors", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_memory_alt_24
            )
        }

        // --- SECTION: NETWORK & SYNC ---
        Text(
            text = stringResource(R.string.title_battery_saver_network),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                title = stringResource(R.string.label_enable_datasaver),
                description = stringResource(R.string.desc_enable_datasaver),
                isChecked = getBool("enable_datasaver", false),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "enable_datasaver", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_data_usage_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_defer_full_backup),
                description = stringResource(R.string.desc_defer_full_backup),
                isChecked = getBool("defer_full_backup", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "defer_full_backup", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_save_24
            )
            IconToggleItem(
                title = stringResource(R.string.label_defer_keyvalue_backup),
                description = stringResource(R.string.desc_defer_keyvalue_backup),
                isChecked = getBool("defer_keyvalue_backup", true),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "defer_keyvalue_backup", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_sync_24
            )
        }

        // --- SECTION: SOUND & SENSORS ---
        Text(
            text = stringResource(R.string.title_battery_saver_sound_sensors),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoundedCardContainer(spacing = 2.dp) {
            IconToggleItem(
                title = stringResource(R.string.label_disable_vibration),
                description = stringResource(R.string.desc_disable_vibration),
                isChecked = getBool("disable_vibration", false),
                onCheckedChange = {
                    viewModel.updateBatterySaverConstant(context, "disable_vibration", it.toString())
                    HapticUtil.performUIHaptic(view)
                },
                enabled = isEnabled,
                iconRes = R.drawable.rounded_mobile_vibrate_24
            )


            // Sound Trigger Mode Selector
            val soundTriggerValue = getInt("soundtrigger_mode", 1)
            com.sameerasw.essentials.ui.components.cards.ConfigPickerItem(
                title = stringResource(R.string.label_soundtrigger_mode),
                description = stringResource(R.string.desc_soundtrigger_mode),
                selectedValue = when (soundTriggerValue) {
                    0 -> stringResource(R.string.soundtrigger_mode_allow)
                    1 -> stringResource(R.string.soundtrigger_mode_restrict)
                    else -> stringResource(R.string.soundtrigger_mode_critical)
                },
                iconRes = R.drawable.rounded_volume_up_24,
                isEnabled = isEnabled
            ) {
                listOf(0, 1, 2).forEach { option ->
                    val optionLabel = when (option) {
                        0 -> stringResource(R.string.soundtrigger_mode_allow)
                        1 -> stringResource(R.string.soundtrigger_mode_restrict)
                        else -> stringResource(R.string.soundtrigger_mode_critical)
                    }
                    com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            viewModel.updateBatterySaverConstant(context, "soundtrigger_mode", option.toString())
                            HapticUtil.performUIHaptic(view)
                        }
                    )
                }
            }
        }

        // --- SECTION: LOCATION ---
        Text(
            text = stringResource(R.string.title_battery_saver_location),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RoundedCardContainer(spacing = 2.dp) {
            val locationModeValue = getInt("location_mode", 3)
            val selectedLabel = when (locationModeValue) {
                0 -> stringResource(R.string.location_mode_no_change)
                1 -> stringResource(R.string.location_mode_gps_disabled)
                2 -> stringResource(R.string.location_mode_all_disabled_when_screen_off)
                3 -> stringResource(R.string.location_mode_foreground_only)
                else -> stringResource(R.string.location_mode_throttle)
            }

            com.sameerasw.essentials.ui.components.cards.ConfigPickerItem(
                title = stringResource(R.string.label_location_mode),
                description = stringResource(R.string.desc_location_mode),
                selectedValue = selectedLabel,
                iconRes = R.drawable.rounded_location_on_24,
                isEnabled = isEnabled
            ) {
                listOf(0, 1, 2, 3, 4).forEach { option ->
                    val optionLabel = when (option) {
                        0 -> stringResource(R.string.location_mode_no_change)
                        1 -> stringResource(R.string.location_mode_gps_disabled)
                        2 -> stringResource(R.string.location_mode_all_disabled_when_screen_off)
                        3 -> stringResource(R.string.location_mode_foreground_only)
                        else -> stringResource(R.string.location_mode_throttle)
                    }
                    com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            viewModel.updateBatterySaverConstant(context, "location_mode", option.toString())
                            HapticUtil.performUIHaptic(view)
                        }
                    )
                }
            }
        }

        // Reset Button
        RoundedCardContainer {
            Button(
                onClick = {
                    viewModel.resetBatterySaverConstants(context)
                    HapticUtil.performUIHaptic(view)
                    Toast.makeText(context, R.string.msg_battery_saver_reset_success, Toast.LENGTH_SHORT).show()
                },
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(text = stringResource(R.string.action_reset_battery_saver))
            }
        }
    }
}
