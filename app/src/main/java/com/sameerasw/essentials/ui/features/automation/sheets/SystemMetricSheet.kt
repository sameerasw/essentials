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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.domain.diy.Comparison
import com.sameerasw.essentials.domain.diy.Metric
import com.sameerasw.essentials.domain.diy.Trigger
import com.sameerasw.essentials.translation.TranslatableText
import com.sameerasw.essentials.ui.components.CategoryExpandableSection
import com.sameerasw.essentials.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.essentials.ui.components.sliders.ConfigSliderItem
import com.sameerasw.essentials.ui.core.cards.ConfigPickerItem
import com.sameerasw.essentials.ui.core.containers.RoundedCardContainer
import com.sameerasw.essentials.utils.HapticUtil
import kotlin.math.roundToInt
import com.sameerasw.essentials.domain.diy.State as DIYState

@OptIn(ExperimentalMaterial3Api::class)
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
        val initial = initialTrigger?.comparison ?: initialState?.comparison ?: Comparison()
        val validOp =
            if (isTrigger && !initial.operator.forTriggers) {
                Comparison.Operator.FALLS_BELOW
            } else if (!isTrigger && !initial.operator.forStates) {
                Comparison.Operator.LTE
            } else {
                initial.operator
            }
        mutableStateOf(initial.copy(operator = validOp))
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
            TranslatableText(
                stringResId = metric.title,
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
                TranslatableText(
                    stringResId = R.string.diy_metric_now,
                    formatArgs = listOf(stringResource(R.string.diy_metric_percent, batteryLevelValue)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            val operators =
                if (isTrigger) {
                    Comparison.Operator.entries.filter { it.forTriggers }
                } else {
                    Comparison.Operator.entries.filter { it.forStates }
                }

            RoundedCardContainer {
                ConfigPickerItem(
                    title = stringResource(R.string.diy_metric_operator),
                    selectedValue = stringResource(operatorLabel(comparison.operator, isTrigger)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    operators.forEach { op ->
                        SegmentedDropdownMenuItem(
                            text = { Text(stringResource(operatorLabel(op, isTrigger))) },
                            onClick = {
                                if (comparison.operator != op) {
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
                        )
                    }
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
                RoundedCardContainer {
                    ConfigSliderItem(
                        title = stringResource(metric.title),
                        value = comparison.value.toFloat().coerceIn(minVal.toFloat(), maxVal.toFloat()),
                        onValueChange = { v ->
                            val newV = v.toDouble().roundToInt().toDouble()
                            if (newV != comparison.value) {
                                comparison = comparison.copy(value = newV)
                                notifyChange()
                            }
                        },
                        valueRange = minVal.toFloat()..maxVal.toFloat(),
                        increment = metric.step.toFloat(),
                        steps = steps.coerceAtLeast(0),
                        valueFormatter = { context.getString(R.string.diy_metric_percent, it.roundToInt()) },
                    )
                }
            }

            val showHold = !comparison.operator.isDelta
            val showCooldown = isTrigger
            val showHysteresis =
                !comparison.operator.isDelta && comparison.operator != Comparison.Operator.NEQ &&
                    comparison.operator != Comparison.Operator.EQ
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
                            ConfigSliderItem(
                                title = stringResource(R.string.diy_metric_hold),
                                value = comparison.holdSeconds.toFloat(),
                                onValueChange = { v ->
                                    val stepped = (v / 5f).roundToInt() * 5
                                    if (stepped != comparison.holdSeconds) {
                                        comparison = comparison.copy(holdSeconds = stepped)
                                        notifyChange()
                                    }
                                },
                                valueRange = 0f..300f,
                                increment = 5f,
                                steps = 59,
                                valueFormatter = {
                                    val sec = it.roundToInt()
                                    if (sec == 0) {
                                        context.getString(R.string.diy_metric_hold_off)
                                    } else {
                                        context.getString(R.string.diy_metric_seconds, sec)
                                    }
                                },
                            )
                        }

                        if (showCooldown) {
                            ConfigSliderItem(
                                title = stringResource(R.string.diy_metric_cooldown),
                                value = cooldownSeconds.toFloat(),
                                onValueChange = { v ->
                                    val stepped = (v / 5f).roundToInt() * 5
                                    val clamped = stepped.coerceIn(Trigger.MIN_VALUE_COOLDOWN_SECONDS, 600)
                                    if (clamped != cooldownSeconds) {
                                        cooldownSeconds = clamped
                                        notifyChange()
                                    }
                                },
                                valueRange = Trigger.MIN_VALUE_COOLDOWN_SECONDS.toFloat()..600f,
                                increment = 5f,
                                steps = 118,
                                valueFormatter = { context.getString(R.string.diy_metric_seconds, it.roundToInt()) },
                            )
                        }

                        if (showHysteresis) {
                            val currentHysteresis = (comparison.hysteresis ?: metric.defaultHysteresis).roundToInt()
                            ConfigSliderItem(
                                title = stringResource(R.string.diy_metric_hysteresis),
                                value = currentHysteresis.toFloat(),
                                onValueChange = { v ->
                                    val stepped = v.roundToInt()
                                    if (stepped != currentHysteresis) {
                                        comparison = comparison.copy(hysteresis = stepped.toDouble())
                                        notifyChange()
                                    }
                                },
                                valueRange = 0f..10f,
                                increment = 1f,
                                steps = 9,
                                valueFormatter = { context.getString(R.string.diy_metric_percent, it.roundToInt()) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun operatorLabel(
    op: Comparison.Operator,
    isTrigger: Boolean,
): Int =
    if (isTrigger) {
        when (op) {
            Comparison.Operator.EQ -> R.string.diy_metric_op_reaches
            Comparison.Operator.BETWEEN -> R.string.diy_metric_op_enters_range
            Comparison.Operator.OUTSIDE -> R.string.diy_metric_op_leaves_range
            Comparison.Operator.RISES_ABOVE -> R.string.diy_metric_op_rises_above
            Comparison.Operator.FALLS_BELOW -> R.string.diy_metric_op_falls_below
            Comparison.Operator.CHANGES_BY -> R.string.diy_metric_op_changes_by
            Comparison.Operator.INCREASES_BY -> R.string.diy_metric_op_increases_by
            Comparison.Operator.DECREASES_BY -> R.string.diy_metric_op_decreases_by
            else -> R.string.diy_metric_op_falls_below
        }
    } else {
        when (op) {
            Comparison.Operator.GT -> R.string.diy_metric_op_gt
            Comparison.Operator.GTE -> R.string.diy_metric_op_gte
            Comparison.Operator.LT -> R.string.diy_metric_op_lt
            Comparison.Operator.LTE -> R.string.diy_metric_op_lte
            Comparison.Operator.EQ -> R.string.diy_metric_op_eq
            Comparison.Operator.NEQ -> R.string.diy_metric_op_neq
            Comparison.Operator.BETWEEN -> R.string.diy_metric_op_between
            Comparison.Operator.OUTSIDE -> R.string.diy_metric_op_outside
            else -> R.string.diy_metric_op_lte
        }
    }
