/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Models & Registries
 * File: SystemMetricTracker.kt
 * Description: Pure Kotlin tracker for numeric value triggers, states, holds, and loop breaking.
 */

package com.sameerasw.essentials.domain.diy

import kotlin.math.abs

class SystemMetricTracker(
    private val clock: () -> Long,
    private val scheduler: HoldScheduler,
    private val openStore: OpenStateStore,
    private val listener: Listener,
) {
    fun interface HoldScheduler {
        fun schedule(
            delayMs: Long,
            block: () -> Unit,
        ): () -> Unit
    }

    interface OpenStateStore {
        fun load(): Set<String>

        fun save(ids: Set<String>)
    }

    interface Listener {
        fun onFire(automation: Automation)

        fun onEnter(automation: Automation)

        fun onExit(automation: Automation)

        fun onLoopBreak(automation: Automation)
    }

    private data class ConditionKey(
        val metric: Metric,
        val comparison: Comparison,
        val cooldownSeconds: Int,
    )

    private class AutomationRecord(
        var automation: Automation,
        val metric: Metric,
        val comparison: Comparison,
        val cooldownSeconds: Int,
    ) {
        var hasBaseline: Boolean = false
        var active: Boolean = false
        var open: Boolean = false
        var deltaRef: Double? = null
        var lastFireTime: Long? = null
        var cancelHold: (() -> Unit)? = null
        val recentTimestamps = mutableListOf<Long>()
        var tripped: Boolean = false

        fun conditionKey(): ConditionKey = ConditionKey(metric, comparison, cooldownSeconds)
    }

    private val records = mutableMapOf<String, AutomationRecord>()
    private val latestReadings = mutableMapOf<Metric, Double>()
    private val restoredOpenIds = openStore.load().toMutableSet()
    private var firstUpdate = true

    fun updateAutomations(automations: List<Automation>) {
        val relevantAutomations = automations.filter { extractConfig(it) != null }

        if (firstUpdate) {
            firstUpdate = false
            val relevantIds = relevantAutomations.map { it.id }.toSet()
            val dropped = restoredOpenIds.filter { it !in relevantIds }
            if (dropped.isNotEmpty()) {
                restoredOpenIds.removeAll(dropped.toSet())
                saveOpenState()
            }
        }

        val newRecords = mutableMapOf<String, AutomationRecord>()
        val newIds = relevantAutomations.map { it.id }.toSet()

        for ((id, oldRecord) in records) {
            if (id !in newIds) {
                oldRecord.cancelHold?.invoke()
                oldRecord.cancelHold = null
                if (oldRecord.open) {
                    oldRecord.open = false
                    saveOpenState()
                    listener.onExit(oldRecord.automation)
                } else if (!oldRecord.hasBaseline && id in restoredOpenIds) {
                    restoredOpenIds.remove(id)
                    saveOpenState()
                    listener.onExit(oldRecord.automation)
                }
            }
        }

        for (automation in relevantAutomations) {
            val (metric, comparison) = extractConfig(automation) ?: continue
            val cooldown = (automation.trigger as? Trigger.SystemMetric)?.cooldownSeconds ?: 0
            val newKey = ConditionKey(metric, comparison, cooldown)

            val existing = records[automation.id]
            if (existing != null) {
                if (existing.conditionKey() == newKey) {
                    existing.automation = automation
                    newRecords[automation.id] = existing
                } else {
                    // Condition changed: treat as remove + add
                    existing.cancelHold?.invoke()
                    existing.cancelHold = null
                    if (existing.open) {
                        existing.open = false
                        saveOpenState()
                        listener.onExit(existing.automation)
                    }
                    val newRecord = AutomationRecord(automation, metric, comparison, cooldown)
                    newRecords[automation.id] = newRecord
                    val currentVal = latestReadings[metric]
                    if (currentVal != null) {
                        baselineRecord(newRecord, currentVal)
                    }
                }
            } else {
                val newRecord = AutomationRecord(automation, metric, comparison, cooldown)
                newRecords[automation.id] = newRecord
                val currentVal = latestReadings[metric]
                if (currentVal != null) {
                    baselineRecord(newRecord, currentVal)
                }
            }
        }

        records.clear()
        records.putAll(newRecords)
        saveOpenState()
    }

    fun onReading(
        metric: Metric,
        value: Double,
    ) {
        if (latestReadings[metric] == value) return
        latestReadings[metric] = value

        for (record in records.values) {
            if (record.metric != metric || record.tripped) continue

            if (!record.hasBaseline) {
                baselineRecord(record, value)
                continue
            }

            if (record.comparison.operator.isDelta) {
                evaluateDeltaTrigger(record, value)
            } else {
                evaluateLevelCondition(record, value)
            }
        }
    }

    fun closeAll() {
        for (record in records.values) {
            record.cancelHold?.invoke()
            record.cancelHold = null
            if (record.open) {
                record.open = false
                listener.onExit(record.automation)
            } else if (!record.hasBaseline && record.automation.id in restoredOpenIds) {
                restoredOpenIds.remove(record.automation.id)
                listener.onExit(record.automation)
            }
        }
        records.clear()
        restoredOpenIds.clear()
        openStore.save(emptySet())
    }

    fun currentValue(metric: Metric): Double? = latestReadings[metric]

    private fun extractConfig(automation: Automation): Pair<Metric, Comparison>? {
        return when (automation.type) {
            Automation.Type.TRIGGER -> {
                val trigger = automation.trigger as? Trigger.SystemMetric ?: return null
                Pair(trigger.metric, trigger.comparison)
            }
            Automation.Type.STATE -> {
                when (val state = automation.state) {
                    is State.SystemMetric -> Pair(state.metric, state.comparison)
                    is State.BatteryLevel ->
                        Pair(
                            Metric.BATTERY_LEVEL,
                            Comparison(
                                operator = Comparison.Operator.BETWEEN,
                                value = state.minLevel.toDouble(),
                                upper = state.maxLevel.toDouble(),
                                holdSeconds = 0,
                                hysteresis = 0.0,
                            ),
                        )
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun baselineRecord(
        record: AutomationRecord,
        v: Double,
    ) {
        record.hasBaseline = true
        val isRestored = restoredOpenIds.remove(record.automation.id)

        if (record.comparison.operator.isDelta) {
            record.deltaRef = v
            return
        }

        val isActive = record.comparison.isActive(v, wasActive = false, metric = record.metric)
        record.active = isActive

        if (record.automation.type == Automation.Type.STATE) {
            if (isRestored) {
                if (isActive) {
                    record.open = true
                    saveOpenState()
                } else {
                    record.open = false
                    saveOpenState()
                    listener.onExit(record.automation)
                }
            } else {
                if (isActive) {
                    if (record.comparison.holdSeconds > 0) {
                        scheduleHold(record)
                    } else {
                        executeEnter(record)
                    }
                }
            }
        }
    }

    private fun scheduleHold(record: AutomationRecord) {
        record.cancelHold?.invoke()
        val delayMs = record.comparison.holdSeconds * 1000L
        record.cancelHold =
            scheduler.schedule(delayMs) {
                record.cancelHold = null
                if (!record.tripped && record.active) {
                    if (record.automation.type == Automation.Type.TRIGGER) {
                        executeFire(record)
                    } else if (record.automation.type == Automation.Type.STATE && !record.open) {
                        executeEnter(record)
                    }
                }
            }
    }

    private fun executeFire(record: AutomationRecord) {
        if (!checkLoopBreaker(record)) return
        record.lastFireTime = clock()
        listener.onFire(record.automation)
    }

    private fun executeEnter(record: AutomationRecord) {
        if (!checkLoopBreaker(record)) return
        record.open = true
        saveOpenState()
        listener.onEnter(record.automation)
    }

    private fun checkLoopBreaker(record: AutomationRecord): Boolean {
        val now = clock()
        record.recentTimestamps.retainAll { now - it < LOOP_WINDOW_MS }
        if (record.recentTimestamps.size >= LOOP_LIMIT) {
            record.tripped = true
            if (record.open) {
                record.open = false
                saveOpenState()
                listener.onExit(record.automation)
            }
            listener.onLoopBreak(record.automation)
            return false
        }
        record.recentTimestamps.add(now)
        return true
    }

    private fun evaluateDeltaTrigger(
        record: AutomationRecord,
        v: Double,
    ) {
        val ref =
            record.deltaRef ?: run {
                record.deltaRef = v
                return
            }

        val a = record.comparison.value
        val shouldFire =
            when (record.comparison.operator) {
                Comparison.Operator.CHANGES_BY -> abs(v - ref) >= a
                Comparison.Operator.INCREASES_BY -> {
                    val met = v - ref >= a
                    if (!met) {
                        record.deltaRef = minOf(ref, v)
                    }
                    met
                }
                Comparison.Operator.DECREASES_BY -> {
                    val met = ref - v >= a
                    if (!met) {
                        record.deltaRef = maxOf(ref, v)
                    }
                    met
                }
                else -> false
            }

        if (shouldFire) {
            val now = clock()
            val cooldownMs = maxOf(record.cooldownSeconds, Trigger.MIN_VALUE_COOLDOWN_SECONDS) * 1000L
            val last = record.lastFireTime
            if (last == null || now - last >= cooldownMs) {
                if (checkLoopBreaker(record)) {
                    record.lastFireTime = now
                    record.deltaRef = v
                    listener.onFire(record.automation)
                }
            }
        }
    }

    private fun evaluateLevelCondition(
        record: AutomationRecord,
        v: Double,
    ) {
        val wasActive = record.active
        val isActive = record.comparison.isActive(v, wasActive, record.metric)
        record.active = isActive

        if (!wasActive && isActive) {
            if (record.automation.type == Automation.Type.TRIGGER) {
                val now = clock()
                val cooldownMs = maxOf(record.cooldownSeconds, Trigger.MIN_VALUE_COOLDOWN_SECONDS) * 1000L
                val last = record.lastFireTime
                if (last == null || now - last >= cooldownMs) {
                    if (record.comparison.holdSeconds > 0) {
                        scheduleHold(record)
                    } else {
                        executeFire(record)
                    }
                }
            } else if (record.automation.type == Automation.Type.STATE) {
                if (record.comparison.holdSeconds > 0) {
                    scheduleHold(record)
                } else {
                    executeEnter(record)
                }
            }
        } else if (wasActive && !isActive) {
            record.cancelHold?.invoke()
            record.cancelHold = null

            if (record.automation.type == Automation.Type.STATE && record.open) {
                record.open = false
                saveOpenState()
                listener.onExit(record.automation)
            }
        }
    }

    private fun saveOpenState() {
        val openIds =
            records.values
                .filter { it.open }
                .map { it.automation.id }
                .toSet() + restoredOpenIds
        openStore.save(openIds)
    }

    companion object {
        const val LOOP_LIMIT = 5
        const val LOOP_WINDOW_MS = 60_000L
    }
}
