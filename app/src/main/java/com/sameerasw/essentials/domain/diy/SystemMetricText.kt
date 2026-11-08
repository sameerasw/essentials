/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: SystemMetricText.kt
 * Description: Formatting functions for system metric conditions.
 */

package com.sameerasw.essentials.domain.diy

import android.content.Context
import com.sameerasw.essentials.R
import kotlin.math.roundToInt

fun systemMetricText(
    context: Context,
    metric: Metric,
    comparison: Comparison,
): String {
    val metricTitle = context.getString(metric.title)
    val formattedValue = context.getString(R.string.diy_metric_percent, comparison.value.roundToInt())
    val formattedUpper =
        comparison.upper?.let {
            context.getString(R.string.diy_metric_percent, it.roundToInt())
        } ?: formattedValue

    val formatRes =
        when (comparison.operator) {
            Comparison.Operator.GT -> R.string.diy_metric_summary_gt
            Comparison.Operator.GTE -> R.string.diy_metric_summary_gte
            Comparison.Operator.LT -> R.string.diy_metric_summary_lt
            Comparison.Operator.LTE -> R.string.diy_metric_summary_lte
            Comparison.Operator.EQ -> R.string.diy_metric_summary_eq
            Comparison.Operator.NEQ -> R.string.diy_metric_summary_neq
            Comparison.Operator.BETWEEN -> R.string.diy_metric_summary_between
            Comparison.Operator.OUTSIDE -> R.string.diy_metric_summary_outside
            Comparison.Operator.RISES_ABOVE -> R.string.diy_metric_summary_rises_above
            Comparison.Operator.FALLS_BELOW -> R.string.diy_metric_summary_falls_below
            Comparison.Operator.CHANGES_BY -> R.string.diy_metric_summary_changes_by
            Comparison.Operator.INCREASES_BY -> R.string.diy_metric_summary_increases_by
            Comparison.Operator.DECREASES_BY -> R.string.diy_metric_summary_decreases_by
        }
    return context.getString(formatRes, metricTitle, formattedValue, formattedUpper)
}

fun conditionText(
    context: Context,
    automation: Automation,
): String =
    when (automation.type) {
        Automation.Type.TRIGGER ->
            when (val trigger = automation.trigger) {
                is Trigger.SystemMetric -> systemMetricText(context, trigger.metric, trigger.comparison)
                else -> ""
            }
        Automation.Type.STATE ->
            when (val state = automation.state) {
                is State.SystemMetric -> systemMetricText(context, state.metric, state.comparison)
                is State.BatteryLevel -> context.getString(R.string.diy_battery_level_range, state.minLevel, state.maxLevel)
                else -> ""
            }
        else -> ""
    }
