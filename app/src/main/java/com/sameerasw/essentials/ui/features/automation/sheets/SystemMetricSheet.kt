/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Core UI Sheets
 * File: SystemMetricSheet.kt
 * Description: Bottom sheet editor for system metric triggers and states.
 */

package com.sameerasw.essentials.ui.core.sheets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Comparison
import com.sameerasw.essentials.domain.diy.Metric
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import kotlin.math.roundToInt
import com.sameerasw.essentials.domain.diy.State as DIYState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SystemMetricSheet(
    initialTrigger: Trigger.SystemMetric?,
    initialState: DIYState.SystemMetric?,
    onDismiss: () -> Unit,
    onTriggerChange: (Trigger.SystemMetric) -> Unit,
    onStateChange: (DIYState.SystemMetric) -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current
    val isTrigger = initialTrigger != null

    val metric = initialTrigger?.metric ?: initialState?.metric ?: Metric.BATTERY_LEVEL
    var comparison by remember {
        mutableStateOf(initialTrigger?.comparison ?: initialState?.comparison ?: Comparison())
    }
    var cooldownSeconds by remember {
        mutableIntStateOf(initialTrigger?.cooldownSeconds ?: Trigger.DEFAULT_VALUE_COOLDOWN_SECONDS)
    }
    var advancedExpanded by remember { mutableStateOf(false) }

    fun notifyChange() {
        if (isTrigger) {
            onTriggerChange(
                Trigger.SystemMetric(
                    metric = metric,
                    comparison = comparison,
                    cooldownSeconds = cooldownSeconds,
                ),
            )
        } else {
            onStateChange(
                DIYState.SystemMetric(
                    metric = metric,
                    comparison = comparison,
                ),
            )
        }
    }

    var currentBatteryPercent by remember {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, filter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val p = if (level >= 0 && scale > 0) level * 100 / scale else null
        mutableStateOf(p)
    }

    DisposableEffect(context) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    c: Context,
                    intent: Intent,
                ) {
                    if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                        if (level >= 0 && scale > 0) {
                            currentBatteryPercent = level * 100 / scale
                        }
                    }
                }
            }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
        }
    }

    EssentialsBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(metric.title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )

            val bigText =
                if (comparison.operator.isRange) {
                    stringResource(
                        R.string.diy_battery_level_range,
                        comparison.value.roundToInt(),
                        (comparison.upper ?: comparison.value).roundToInt(),
                    )
                } else {
                    stringResource(R.string.diy_metric_percent, comparison.value.roundToInt())
                }
            Text(
                text = bigText,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )

            val batteryLevelValue = currentBatteryPercent
            if (batteryLevelValue != null) {
                Text(
                    text =
                        stringResource(
                            R.string.diy_metric_now,
                            stringResource(R.string.diy_metric_percent, batteryLevelValue),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            val operators =
                if (isTrigger) {
                    Comparison.Operator.entries
                } else {
                    Comparison.Operator.entries.filter { it.forStates }
                }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                operators.forEach { op ->
                    FilterChip(
                        selected = comparison.operator == op,
                        onClick = {
                            if (comparison.operator != op) {
                                HapticUtil.performVirtualKeyHaptic(view)
                                val newUpper =
                                    if (op.isRange) {
                                        comparison.upper ?: minOf(comparison.value + 20.0, metric.max)
                                    } else {
                                        comparison.upper
                                    }
                                val newValue =
                                    if (op.isDelta && comparison.value < metric.step) {
                                        metric.step
                                    } else {
                                        comparison.value
                                    }
                                comparison = comparison.copy(operator = op, upper = newUpper, value = newValue)
                                notifyChange()
                            }
                        },
                        label = { Text(stringResource(operatorLabel(op))) },
                    )
                }
            }

            if (comparison.operator.isRange) {
                val start = comparison.value.toFloat()
                val end = (comparison.upper ?: comparison.value).toFloat()
                val steps = ((metric.max - metric.min) / metric.step).roundToInt() - 1
                RangeSlider(
                    value = start..end,
                    onValueChange = { range ->
                        val newStart =
                            range.start
                                .toDouble()
                                .roundToInt()
                                .toDouble()
                        val newEnd =
                            range.endInclusive
                                .toDouble()
                                .roundToInt()
                                .toDouble()
                        if (newStart != comparison.value || newEnd != comparison.upper) {
                            HapticUtil.performSliderHaptic(view)
                            comparison = comparison.copy(value = newStart, upper = newEnd)
                            notifyChange()
                        }
                    },
                    valueRange = metric.min.toFloat()..metric.max.toFloat(),
                    steps = steps.coerceAtLeast(0),
                )
            } else {
                val minVal = if (comparison.operator.isDelta) metric.step else metric.min
                val maxVal = metric.max
                val steps = ((maxVal - minVal) / metric.step).roundToInt() - 1
                Slider(
                    value = comparison.value.toFloat().coerceIn(minVal.toFloat(), maxVal.toFloat()),
                    onValueChange = { v ->
                        val newV = v.toDouble().roundToInt().toDouble()
                        if (newV != comparison.value) {
                            HapticUtil.performSliderHaptic(view)
                            comparison = comparison.copy(value = newV)
                            notifyChange()
                        }
                    },
                    valueRange = minVal.toFloat()..maxVal.toFloat(),
                    steps = steps.coerceAtLeast(0),
                )
            }

            val showHold = !comparison.operator.isDelta
            val showCooldown = isTrigger
            val showHysteresis = !comparison.operator.isDelta && comparison.operator != Comparison.Operator.NEQ
            val advancedCount = (if (showHold) 1 else 0) + (if (showCooldown) 1 else 0) + (if (showHysteresis) 1 else 0)

            if (advancedCount > 0) {
                CategoryExpandableSection(
                    title = stringResource(R.string.diy_metric_advanced),
                    itemCount = advancedCount,
                    isExpanded = advancedExpanded,
                    onToggleExpand = { advancedExpanded = !advancedExpanded },
                ) {
                    RoundedCardContainer(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        if (showHold) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.diy_metric_hold),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text =
                                            if (comparison.holdSeconds == 0) {
                                                stringResource(R.string.diy_metric_hold_off)
                                            } else {
                                                stringResource(R.string.diy_metric_seconds, comparison.holdSeconds)
                                            },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Slider(
                                    value = comparison.holdSeconds.toFloat(),
                                    onValueChange = { v ->
                                        val stepped = (v / 5f).roundToInt() * 5
                                        if (stepped != comparison.holdSeconds) {
                                            HapticUtil.performSliderHaptic(view)
                                            comparison = comparison.copy(holdSeconds = stepped)
                                            notifyChange()
                                        }
                                    },
                                    valueRange = 0f..300f,
                                    steps = 59,
                                )
                            }
                        }

                        if (showCooldown) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.diy_metric_cooldown),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.diy_metric_seconds, cooldownSeconds),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Slider(
                                    value = cooldownSeconds.toFloat(),
                                    onValueChange = { v ->
                                        val stepped = (v / 5f).roundToInt() * 5
                                        val clamped = stepped.coerceIn(Trigger.MIN_VALUE_COOLDOWN_SECONDS, 600)
                                        if (clamped != cooldownSeconds) {
                                            HapticUtil.performSliderHaptic(view)
                                            cooldownSeconds = clamped
                                            notifyChange()
                                        }
                                    },
                                    valueRange = Trigger.MIN_VALUE_COOLDOWN_SECONDS.toFloat()..600f,
                                    steps = 118,
                                )
                            }
                        }

                        if (showHysteresis) {
                            val currentHysteresis = (comparison.hysteresis ?: metric.defaultHysteresis).roundToInt()
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.diy_metric_hysteresis),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = stringResource(R.string.diy_metric_percent, currentHysteresis),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Slider(
                                    value = currentHysteresis.toFloat(),
                                    onValueChange = { v ->
                                        val stepped = v.roundToInt()
                                        if (stepped != currentHysteresis) {
                                            HapticUtil.performSliderHaptic(view)
                                            comparison = comparison.copy(hysteresis = stepped.toDouble())
                                            notifyChange()
                                        }
                                    },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun operatorLabel(op: Comparison.Operator): Int =
    when (op) {
        Comparison.Operator.GT -> R.string.diy_metric_op_gt
        Comparison.Operator.GTE -> R.string.diy_metric_op_gte
        Comparison.Operator.LT -> R.string.diy_metric_op_lt
        Comparison.Operator.LTE -> R.string.diy_metric_op_lte
        Comparison.Operator.EQ -> R.string.diy_metric_op_eq
        Comparison.Operator.NEQ -> R.string.diy_metric_op_neq
        Comparison.Operator.BETWEEN -> R.string.diy_metric_op_between
        Comparison.Operator.OUTSIDE -> R.string.diy_metric_op_outside
        Comparison.Operator.RISES_ABOVE -> R.string.diy_metric_op_rises_above
        Comparison.Operator.FALLS_BELOW -> R.string.diy_metric_op_falls_below
        Comparison.Operator.CHANGES_BY -> R.string.diy_metric_op_changes_by
        Comparison.Operator.INCREASES_BY -> R.string.diy_metric_op_increases_by
        Comparison.Operator.DECREASES_BY -> R.string.diy_metric_op_decreases_by
    }
