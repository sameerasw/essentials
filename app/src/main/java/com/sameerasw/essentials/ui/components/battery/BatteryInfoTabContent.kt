package com.sameerasw.essentials.ui.components.battery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.ui.components.containers.RoundedCardContainer
import com.sameerasw.essentials.ui.theme.Shapes
import com.sameerasw.essentials.utils.BatteryDetails
import com.sameerasw.essentials.utils.BatteryInfoUtil
import com.sameerasw.essentials.utils.ThermalInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.ui.components.buttons.ListExpandToggleButton
import com.sameerasw.essentials.ui.components.pickers.MultiSegmentedPicker
import java.util.Locale

@Composable
fun BatteryInfoTabContent(
    batteryDetails: BatteryDetails,
    isLoadingAdvanced: Boolean,
    onRefresh: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current

    // State of Health Dial
    batteryDetails.stateOfHealth?.let { soh ->
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceBright,
                        shape = Shapes.extraSmall
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.label_battery_state_of_health),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$soh%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (soh >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { soh / 100f },
                        modifier = Modifier.size(64.dp),
                        color = if (soh >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        strokeWidth = 6.dp
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_ecg_heart_24),
                        contentDescription = null,
                        tint = if (soh >= 80) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // Health Section
    SectionHeaderTitle(title = R.string.label_battery_section_health)

    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        InfoDetailRow(
            title = R.string.label_battery_health,
            value = BatteryInfoUtil.formatHealth(batteryDetails.health),
            iconRes = R.drawable.rounded_ecg_heart_24
        )
        InfoDetailRow(
            title = R.string.label_battery_temperature,
            value = String.format(LocalLocale.current.platformLocale, "%.1f °C", batteryDetails.temperature / 10.0f),
            iconRes = R.drawable.rounded_device_thermostat_24
        )

        if (isLoadingAdvanced) {
            BatteryLoadingIndicatorCard()
        } else {
            val full = batteryDetails.chargeFull
            val design = batteryDetails.chargeFullDesign

            // Merged Capacity row: "full charge capacity / design capacity mAh" using design icon
            if (full != null && design != null && full > 0 && design > 0) {
                val fullMah = if (full > 10000) full / 1000 else full
                val designMah = if (design > 10000) design / 1000 else design
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_capacity),
                    value = "$fullMah / $designMah mAh",
                    iconRes = R.drawable.battery_android_frame_shield_24px
                )
            } else if (design != null && design > 0) {
                val designMah = if (design > 10000) design / 1000 else design
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_capacity),
                    value = "$designMah mAh",
                    iconRes = R.drawable.battery_android_frame_shield_24px
                )
            } else if (full != null && full > 0) {
                val fullMah = if (full > 10000) full / 1000 else full
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_capacity),
                    value = "$fullMah mAh",
                    iconRes = R.drawable.battery_android_frame_shield_24px
                )
            }

            if (full != null && design != null && design > 0) {
                val healthPct = (full.toDouble() / design.toDouble()) * 100.0
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_capacity_health),
                    value = String.format(Locale.getDefault(), "%.1f %%", healthPct),
                    iconRes = R.drawable.rounded_ecg_heart_24
                )
            }

            batteryDetails.cycleCount?.let { cycles ->
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_cycle_count),
                    value = "$cycles",
                    iconRes = R.drawable.rounded_cycle_24
                )
            }
        }
    }

    // Charging Section
    val isPlugged = batteryDetails.plugged > 0
    SectionHeaderTitle(title = R.string.label_battery_section_charging)

    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        // Plug type renamed to "Mode"
        InfoDetailRow(
            title = stringResource(R.string.label_battery_mode),
            value = BatteryInfoUtil.formatPlugged(batteryDetails.plugged),
            iconRes = R.drawable.rounded_cable_24
        )

        // Dynamically show remaining charging cards only when plugged with expand/collapse animation
        AnimatedVisibility(
            visible = isPlugged,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isLoadingAdvanced) {
                    BatteryLoadingIndicatorCard()
                } else {
                    // chargingState commented out for now

                    batteryDetails.chargingPolicy?.let { policy ->
                        InfoDetailRow(
                            title = stringResource(R.string.label_battery_charging_policy),
                            value = BatteryInfoUtil.formatChargingPolicy(policy),
                            iconRes = R.drawable.rounded_info_24
                        )
                    }

                    batteryDetails.maxChargingCurrent?.let { maxCur ->
                        if (maxCur > 0) {
                            val curMa = if (maxCur > 10000) maxCur / 1000 else maxCur
                            InfoDetailRow(
                                title = stringResource(R.string.label_battery_max_current),
                                value = "$curMa mA",
                                iconRes = R.drawable.rounded_power_input_24
                            )
                        }
                    }

                    batteryDetails.maxChargingVoltage?.let { maxVol ->
                        if (maxVol > 0) {
                            val volMv = if (maxVol > 100000) maxVol / 1000 else maxVol
                            InfoDetailRow(
                                title = stringResource(R.string.label_battery_max_voltage),
                                value = "$volMv mV",
                                iconRes = R.drawable.rounded_power_input_24
                            )
                        }
                    }

                    /*
                    batteryDetails.chargingStatusNew?.let { statusNew ->
                        InfoDetailRow(
                            title = stringResource(R.string.label_battery_charging_status),
                            value = BatteryInfoUtil.formatChargingStatusNew(statusNew),
                            iconRes = R.drawable.rounded_charger_24
                        )
                    }
                    */

                    batteryDetails.chargeTimeRemainingMs?.let { timeMs ->
                        InfoDetailRow(
                            title = stringResource(R.string.label_battery_charge_time_remaining),
                            value = BatteryInfoUtil.formatChargeTimeRemaining(timeMs),
                            iconRes = R.drawable.battery_android_frame_bolt_24px
                        )
                    }
                }
            }
        }
    }

    // Specs Section
    SectionHeaderTitle(title = R.string.label_battery_section_specs)

    RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
        InfoDetailRow(
            title = stringResource(R.string.label_battery_voltage),
            value = "${batteryDetails.voltage} mV",
            iconRes = R.drawable.rounded_power_input_24
        )
        InfoDetailRow(
            title = stringResource(R.string.label_battery_technology),
            value = batteryDetails.technology,
            iconRes = R.drawable.rounded_memory_alt_24
        )

        if (!isLoadingAdvanced) {
            val counter = batteryDetails.chargeCounter
            if (counter != null && counter > 0) {
                val counterMah = if (counter > 10000) counter / 1000 else counter
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_charge_counter),
                    value = "$counterMah mAh",
                    iconRes = R.drawable.battery_android_frame_4_24px
                )
            }

            // capacityLevel commented out

            batteryDetails.currentNowMa?.let { curNow ->
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_current_now),
                    value = "$curNow mA",
                    iconRes = R.drawable.rounded_power_input_24
                )
            }

            batteryDetails.currentAvgMa?.let { curAvg ->
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_current_avg),
                    value = "$curAvg mA",
                    iconRes = R.drawable.rounded_power_input_24
                )
            }

            batteryDetails.remainingEnergyMwh?.let { energy ->
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_remaining_energy),
                    value = "$energy mWh",
                    iconRes = R.drawable.battery_android_frame_4_24px
                )
            }

            // Average Remaining calculation (Current Charge / Average Current)
            val chargeCounterMah = batteryDetails.chargeCounter?.let { if (it > 10000) it / 1000 else it }
            val avgCurrentMa = batteryDetails.currentAvgMa?.let { kotlin.math.abs(it) }?.takeIf { it > 0 }
            if (chargeCounterMah != null && chargeCounterMah > 0 && avgCurrentMa != null) {
                val remainingHours = chargeCounterMah.toDouble() / avgCurrentMa.toDouble()
                val formattedRemaining = if (remainingHours >= 1.0) {
                    val h = remainingHours.toInt()
                    val m = ((remainingHours - h) * 60).toInt()
                    if (m > 0) "${h}h ${m}m" else "${h}h"
                } else {
                    val m = (remainingHours * 60).toInt().coerceAtLeast(1)
                    "${m}m"
                }
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_avg_remaining),
                    value = formattedRemaining,
                    iconRes = R.drawable.rounded_av_timer_24
                )
            }

            batteryDetails.manufacturingDate?.let { mfg ->
                val (dateStr, isSuspicious) = BatteryInfoUtil.formatDate(mfg)
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_manufacturing_date),
                    value = dateStr,
                    iconRes = if (isSuspicious) R.drawable.rounded_release_alert_24 else R.drawable.rounded_info_24
                )
            }

            batteryDetails.firstUsageDate?.let { firstUse ->
                val (dateStr, isSuspicious) = BatteryInfoUtil.formatDate(firstUse)
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_first_usage_date),
                    value = dateStr,
                    iconRes = if (isSuspicious) R.drawable.rounded_release_alert_24 else R.drawable.rounded_info_24
                )
            }

            batteryDetails.serialNumber?.let { serial ->
                if (serial.isNotBlank()) {
                    InfoDetailRow(
                        title = stringResource(R.string.label_battery_serial_number),
                        value = serial,
                        iconRes = R.drawable.rounded_info_24
                    )
                }
            }
            batteryDetails.partStatus?.let { part ->
                InfoDetailRow(
                    title = stringResource(R.string.label_battery_part_status),
                    value = BatteryInfoUtil.formatPartStatus(part),
                    iconRes = R.drawable.battery_android_frame_shield_24px
                )
            }
        }
    }

    // System Thermals Section (Shizuku / Root)
    batteryDetails.thermalInfo?.takeIf { it.items.isNotEmpty() }?.let { thermal ->
        SectionHeaderTitle(title = R.string.label_thermal_system_thermals)

        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            thermal.maxCpuTemp?.let { cpu ->
                InfoDetailRow(
                    title = stringResource(R.string.label_thermal_cpu),
                    value = "${"%.1f".format(Locale.US, cpu)} °C",
                    iconRes = R.drawable.rounded_memory_alt_24
                )
            }

            thermal.maxGpuTemp?.let { gpu ->
                InfoDetailRow(
                    title = stringResource(R.string.label_thermal_gpu),
                    value = "${"%.1f".format(Locale.US, gpu)} °C",
                    iconRes = R.drawable.rounded_memory_alt_24
                )
            }

            thermal.skinTemp?.let { skin ->
                InfoDetailRow(
                    title = stringResource(R.string.label_thermal_skin),
                    value = "${"%.1f".format(Locale.US, skin)} °C",
                    iconRes = R.drawable.rounded_device_thermostat_24
                )
            }

            val statusText = when (thermal.maxThrottlingStatus) {
                ThermalInfo.THROTTLING_NONE -> stringResource(R.string.label_thermal_status_none)
                ThermalInfo.THROTTLING_LIGHT -> stringResource(R.string.label_thermal_status_light)
                ThermalInfo.THROTTLING_MODERATE -> stringResource(R.string.label_thermal_status_moderate)
                ThermalInfo.THROTTLING_SEVERE -> stringResource(R.string.label_thermal_status_severe)
                ThermalInfo.THROTTLING_CRITICAL -> stringResource(R.string.label_thermal_status_critical)
                ThermalInfo.THROTTLING_EMERGENCY -> stringResource(R.string.label_thermal_status_emergency)
                ThermalInfo.THROTTLING_SHUTDOWN -> stringResource(R.string.label_thermal_status_shutdown)
                else -> stringResource(R.string.label_thermal_status_none)
            }

            InfoDetailRow(
                title = stringResource(R.string.label_thermal_throttling_status),
                value = statusText,
                iconRes = if (thermal.maxThrottlingStatus > ThermalInfo.THROTTLING_NONE) R.drawable.rounded_release_alert_24 else R.drawable.rounded_device_thermostat_24
            )
        }
    }

    // Expandable Settings section for Charging mode QS tile options
    var showSettings by remember { mutableStateOf(false) }
    ListExpandToggleButton(
        isExpanded = showSettings,
        onToggle = { showSettings = !showSettings },
        title = R.string.action_charging_qs_tile_options,
        description = null
    )

    if (showSettings) {
        val settingsRepository = remember { SettingsRepository(context) }
        var enableAdaptive by remember {
            mutableStateOf(settingsRepository.getBoolean("charge_opt_toggle_adaptive", true))
        }
        var enableLimit by remember {
            mutableStateOf(settingsRepository.getBoolean("charge_opt_toggle_limit", true))
        }
        var enableDeactivated by remember {
            mutableStateOf(settingsRepository.getBoolean("charge_opt_toggle_deactivated", true))
        }

        val items = listOf("deactivated", "adaptive", "limit")
        val selectedItems = remember(enableDeactivated, enableAdaptive, enableLimit) {
            mutableSetOf<String>().apply {
                if (enableDeactivated) add("deactivated")
                if (enableAdaptive) add("adaptive")
                if (enableLimit) add("limit")
            }.toSet()
        }

        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceBright,
                        shape = Shapes.extraSmall
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.charge_opt_long_press_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.charge_opt_long_press_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                MultiSegmentedPicker(
                    items = items,
                    selectedItems = selectedItems,
                    onItemsSelected = { newSelection ->
                        if (newSelection.size >= 2) {
                            enableDeactivated = newSelection.contains("deactivated")
                            enableAdaptive = newSelection.contains("adaptive")
                            enableLimit = newSelection.contains("limit")

                            settingsRepository.putBoolean("charge_opt_toggle_deactivated", enableDeactivated)
                            settingsRepository.putBoolean("charge_opt_toggle_adaptive", enableAdaptive)
                            settingsRepository.putBoolean("charge_opt_toggle_limit", enableLimit)
                        }
                    },
                    labelProvider = { item ->
                        when (item) {
                            "deactivated" -> context.getString(R.string.deactivated)
                            "adaptive" -> context.getString(R.string.adaptive_charging)
                            "limit" -> context.getString(R.string.limit_to_80)
                            else -> ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    allowEmpty = false
                )
            }
        }
    }

    // Permission Grant Card at the very bottom if BATTERY_STATS permission is missing AND Shizuku/Root is available & permitted
    val hasStatsPerm = BatteryInfoUtil.hasBatteryStatsPermission(context)
    val isShellAvailable = com.sameerasw.essentials.utils.ShellUtils.isAvailable(context) && com.sameerasw.essentials.utils.ShellUtils.hasPermission(context)
    if (!hasStatsPerm && batteryDetails.stateOfHealth == null && batteryDetails.cycleCount == null && android.os.Build.VERSION.SDK_INT >= 34 && isShellAvailable) {
        RoundedCardContainer(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceBright,
                        shape = Shapes.extraSmall
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_info_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.label_battery_grant_stats_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(R.string.label_battery_grant_stats_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (com.sameerasw.essentials.utils.ShizukuUtils.isShizukuAvailable() && com.sameerasw.essentials.utils.ShizukuUtils.hasPermission()) {
                    Button(
                        onClick = {
                            com.sameerasw.essentials.utils.HapticUtil.performVirtualKeyHaptic(view)
                            if (com.sameerasw.essentials.utils.ShizukuUtils.grantBatteryStatsPermission()) {
                                onRefresh()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.action_grant_shizuku))
                    }
                }
            }
        }
    }
}
