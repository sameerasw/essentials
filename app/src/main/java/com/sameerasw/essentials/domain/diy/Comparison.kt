/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: Comparison.kt
 * Description: Comparison operators and threshold evaluations with hysteresis.
 */

package com.sameerasw.essentials.domain.diy

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlin.math.abs

@Keep
data class Comparison(
    @SerializedName("operator") val operator: Operator = Operator.LTE,
    @SerializedName("value") val value: Double = 20.0,
    @SerializedName("upper") val upper: Double? = null,
    @SerializedName("holdSeconds") val holdSeconds: Int = 0,
    @SerializedName("hysteresis") val hysteresis: Double? = null,
) {
    @Keep
    enum class Operator(
        val forStates: Boolean,
        val isDelta: Boolean,
        val isRange: Boolean,
    ) {
        @SerializedName("GT")
        GT(true, false, false),

        @SerializedName("GTE")
        GTE(true, false, false),

        @SerializedName("LT")
        LT(true, false, false),

        @SerializedName("LTE")
        LTE(true, false, false),

        @SerializedName("EQ")
        EQ(true, false, false),

        @SerializedName("NEQ")
        NEQ(true, false, false),

        @SerializedName("BETWEEN")
        BETWEEN(true, false, true),

        @SerializedName("OUTSIDE")
        OUTSIDE(true, false, true),

        @SerializedName("RISES_ABOVE")
        RISES_ABOVE(false, false, false),

        @SerializedName("FALLS_BELOW")
        FALLS_BELOW(false, false, false),

        @SerializedName("CHANGES_BY")
        CHANGES_BY(false, true, false),

        @SerializedName("INCREASES_BY")
        INCREASES_BY(false, true, false),

        @SerializedName("DECREASES_BY")
        DECREASES_BY(false, true, false),
    }

    fun isActive(
        v: Double,
        wasActive: Boolean,
        metric: Metric,
    ): Boolean {
        val a = value
        val b = upper ?: value
        val h = hysteresis ?: metric.defaultHysteresis
        val e = metric.step / 2.0

        return if (!wasActive) {
            when (operator) {
                Operator.GT, Operator.RISES_ABOVE -> v > a
                Operator.GTE -> v >= a
                Operator.LT, Operator.FALLS_BELOW -> v < a
                Operator.LTE -> v <= a
                Operator.EQ -> abs(v - a) <= e
                Operator.NEQ -> abs(v - a) > e
                Operator.BETWEEN -> v in a..b
                Operator.OUTSIDE -> v < a || v > b
                Operator.CHANGES_BY, Operator.INCREASES_BY, Operator.DECREASES_BY -> false
            }
        } else {
            when (operator) {
                Operator.GT, Operator.RISES_ABOVE -> v > a - h
                Operator.GTE -> v >= a - h
                Operator.LT, Operator.FALLS_BELOW -> v < a + h
                Operator.LTE -> v <= a + h
                Operator.EQ -> abs(v - a) <= e + h
                Operator.NEQ -> abs(v - a) > e
                Operator.BETWEEN -> v >= a - h && v <= b + h
                Operator.OUTSIDE -> {
                    val effH = if (b - a <= 2.0 * h) 0.0 else h
                    v < a + effH || v > b - effH
                }
                Operator.CHANGES_BY, Operator.INCREASES_BY, Operator.DECREASES_BY -> false
            }
        }
    }
}
