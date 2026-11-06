/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer Tests
 * File: SystemMetricTrackerTest.kt
 * Description: Unit tests for SystemMetricTracker, Comparison, and state lifecycle.
 */

package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation
import com.sameerasw.essentials.domain.diy.Comparison
import com.sameerasw.essentials.domain.diy.Metric
import com.sameerasw.essentials.domain.diy.SystemMetricTracker
import com.sameerasw.essentials.domain.diy.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.sameerasw.essentials.domain.diy.State as DIYState

class SystemMetricTrackerTest {
    private class FakeClock(
        var timeMs: Long = 1000L,
    ) {
        fun advance(ms: Long) {
            timeMs += ms
        }
    }

    private class FakeScheduler(
        private val clock: FakeClock,
    ) : SystemMetricTracker.HoldScheduler {
        data class Task(
            val triggerTimeMs: Long,
            val block: () -> Unit,
            var cancelled: Boolean = false,
        )

        val tasks = mutableListOf<Task>()

        override fun schedule(
            delayMs: Long,
            block: () -> Unit,
        ): () -> Unit {
            val task = Task(clock.timeMs + delayMs, block)
            tasks.add(task)
            return { task.cancelled = true }
        }

        fun advanceBy(delayMs: Long) {
            val targetTime = clock.timeMs + delayMs
            clock.timeMs = targetTime
            val ready = tasks.filter { !it.cancelled && it.triggerTimeMs <= targetTime }.sortedBy { it.triggerTimeMs }
            for (task in ready) {
                if (!task.cancelled) {
                    task.cancelled = true
                    task.block()
                }
            }
        }
    }

    private class FakeOpenStateStore(
        initial: Set<String> = emptySet(),
    ) : SystemMetricTracker.OpenStateStore {
        var ids: Set<String> = initial

        override fun load(): Set<String> = ids

        override fun save(ids: Set<String>) {
            this.ids = ids
        }
    }

    private class FakeListener : SystemMetricTracker.Listener {
        val fires = mutableListOf<Automation>()
        val enters = mutableListOf<Automation>()
        val exits = mutableListOf<Automation>()

        override fun onFire(automation: Automation) {
            fires.add(automation)
        }

        override fun onEnter(automation: Automation) {
            enters.add(automation)
        }

        override fun onExit(automation: Automation) {
            exits.add(automation)
        }

        fun clear() {
            fires.clear()
            enters.clear()
            exits.clear()
        }
    }

    @Test
    fun comparisonIsActiveForEveryOperator() {
        // GT and RISES_ABOVE
        val gt = Comparison(operator = Comparison.Operator.GT, value = 50.0, hysteresis = 2.0)
        assertFalse(gt.isActive(50.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(gt.isActive(50.1, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(gt.isActive(48.1, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(gt.isActive(48.0, wasActive = true, Metric.BATTERY_LEVEL))

        val risesAbove = Comparison(operator = Comparison.Operator.RISES_ABOVE, value = 50.0, hysteresis = 2.0)
        assertFalse(risesAbove.isActive(50.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(risesAbove.isActive(50.1, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(risesAbove.isActive(48.1, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(risesAbove.isActive(48.0, wasActive = true, Metric.BATTERY_LEVEL))

        // GTE
        val gte = Comparison(operator = Comparison.Operator.GTE, value = 50.0, hysteresis = 2.0)
        assertFalse(gte.isActive(49.9, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(gte.isActive(50.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(gte.isActive(48.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(gte.isActive(47.9, wasActive = true, Metric.BATTERY_LEVEL))

        // LT and FALLS_BELOW
        val lt = Comparison(operator = Comparison.Operator.LT, value = 20.0, hysteresis = 2.0)
        assertFalse(lt.isActive(20.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(lt.isActive(19.9, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(lt.isActive(21.9, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(lt.isActive(22.0, wasActive = true, Metric.BATTERY_LEVEL))

        val fallsBelow = Comparison(operator = Comparison.Operator.FALLS_BELOW, value = 20.0, hysteresis = 2.0)
        assertFalse(fallsBelow.isActive(20.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(fallsBelow.isActive(19.9, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(fallsBelow.isActive(21.9, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(fallsBelow.isActive(22.0, wasActive = true, Metric.BATTERY_LEVEL))

        // LTE
        val lte = Comparison(operator = Comparison.Operator.LTE, value = 20.0, hysteresis = 2.0)
        assertFalse(lte.isActive(20.1, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(lte.isActive(20.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(lte.isActive(22.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(lte.isActive(22.1, wasActive = true, Metric.BATTERY_LEVEL))

        // EQ is exact (79 and 81 aren't equal to 80), no hysteresis
        val eq = Comparison(operator = Comparison.Operator.EQ, value = 80.0, hysteresis = 2.0)
        assertFalse(eq.isActive(79.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertFalse(eq.isActive(81.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(eq.isActive(80.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertFalse(eq.isActive(79.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(eq.isActive(81.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertTrue(eq.isActive(80.0, wasActive = true, Metric.BATTERY_LEVEL))

        // NEQ is exact, no hysteresis
        val neq = Comparison(operator = Comparison.Operator.NEQ, value = 80.0, hysteresis = 2.0)
        assertTrue(neq.isActive(79.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(neq.isActive(81.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertFalse(neq.isActive(80.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(neq.isActive(79.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertTrue(neq.isActive(81.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(neq.isActive(80.0, wasActive = true, Metric.BATTERY_LEVEL))

        // BETWEEN
        val between = Comparison(operator = Comparison.Operator.BETWEEN, value = 20.0, upper = 80.0, hysteresis = 2.0)
        assertFalse(between.isActive(19.9, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(between.isActive(20.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(between.isActive(80.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertFalse(between.isActive(80.1, wasActive = false, Metric.BATTERY_LEVEL))
        // while active: a - h .. b + h -> 18.0 .. 82.0
        assertTrue(between.isActive(18.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(between.isActive(17.9, wasActive = true, Metric.BATTERY_LEVEL))
        assertTrue(between.isActive(82.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(between.isActive(82.1, wasActive = true, Metric.BATTERY_LEVEL))

        // OUTSIDE: b - a > 2h
        val outsideWide = Comparison(operator = Comparison.Operator.OUTSIDE, value = 20.0, upper = 80.0, hysteresis = 2.0)
        assertTrue(outsideWide.isActive(19.9, wasActive = false, Metric.BATTERY_LEVEL))
        assertFalse(outsideWide.isActive(20.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertFalse(outsideWide.isActive(80.0, wasActive = false, Metric.BATTERY_LEVEL))
        assertTrue(outsideWide.isActive(80.1, wasActive = false, Metric.BATTERY_LEVEL))
        // while active: v < a + h or v > b - h -> v < 22 or v > 78
        assertTrue(outsideWide.isActive(21.9, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(outsideWide.isActive(22.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(outsideWide.isActive(78.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertTrue(outsideWide.isActive(78.1, wasActive = true, Metric.BATTERY_LEVEL))

        // OUTSIDE: b - a <= 2h uses h = 0
        val outsideNarrow = Comparison(operator = Comparison.Operator.OUTSIDE, value = 50.0, upper = 53.0, hysteresis = 2.0)
        assertTrue(outsideNarrow.isActive(49.9, wasActive = false, Metric.BATTERY_LEVEL))
        assertFalse(outsideNarrow.isActive(50.0, wasActive = false, Metric.BATTERY_LEVEL))
        // while active with h = 0
        assertTrue(outsideNarrow.isActive(49.9, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(outsideNarrow.isActive(50.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertFalse(outsideNarrow.isActive(53.0, wasActive = true, Metric.BATTERY_LEVEL))
        assertTrue(outsideNarrow.isActive(53.1, wasActive = true, Metric.BATTERY_LEVEL))
    }

    @Test
    fun deltaOperatorsTrackingAndReset() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        // CHANGES_BY
        val changesByAuto =
            Automation(
                id = "c1",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.CHANGES_BY, value = 5.0),
                        cooldownSeconds = 5,
                    ),
            )
        tracker.updateAutomations(listOf(changesByAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // baseline
        assertEquals(0, listener.fires.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 53.0)
        assertEquals(0, listener.fires.size)

        clock.advance(10_000)
        tracker.onReading(Metric.BATTERY_LEVEL, 55.0) // +5 from ref 50
        assertEquals(1, listener.fires.size)

        clock.advance(10_000)
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // -5 from ref 55
        assertEquals(2, listener.fires.size)

        // INCREASES_BY: tracks minimum while not fired
        val incByAuto =
            Automation(
                id = "inc1",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.INCREASES_BY, value = 10.0),
                        cooldownSeconds = 5,
                    ),
            )
        tracker.updateAutomations(listOf(incByAuto))
        listener.clear()
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // baseline
        assertEquals(0, listener.fires.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 40.0) // drops to 40, ref becomes 40
        assertEquals(0, listener.fires.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 48.0) // +8 from min 40
        assertEquals(0, listener.fires.size)

        clock.advance(10_000)
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // +10 from min 40, fires!
        assertEquals(1, listener.fires.size)

        // DECREASES_BY: tracks maximum while not fired
        val decByAuto =
            Automation(
                id = "dec1",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.DECREASES_BY, value = 10.0),
                        cooldownSeconds = 5,
                    ),
            )
        tracker.updateAutomations(listOf(decByAuto))
        listener.clear()
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // baseline
        assertEquals(0, listener.fires.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 60.0) // rises to 60, ref becomes 60
        assertEquals(0, listener.fires.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 52.0) // -8 from max 60
        assertEquals(0, listener.fires.size)

        clock.advance(10_000)
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // -10 from max 60, fires!
        assertEquals(1, listener.fires.size)
    }

    @Test
    fun deDupeSameValueTwiceEvaluatesOnce() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val triggerAuto =
            Automation(
                id = "t1",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.CHANGES_BY, value = 5.0),
                        cooldownSeconds = 0,
                    ),
            )
        tracker.updateAutomations(listOf(triggerAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0)
        tracker.onReading(Metric.BATTERY_LEVEL, 55.0)
        assertEquals(1, listener.fires.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 55.0) // duplicate reading
        assertEquals(1, listener.fires.size)
    }

    @Test
    fun baselineBehaviorBothOrders() {
        val triggerAuto =
            Automation(
                id = "t1",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                    ),
            )
        val stateAuto =
            Automation(
                id = "s1",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                    ),
            )

        // Order 1: automations first, then reading
        run {
            val clock = FakeClock()
            val scheduler = FakeScheduler(clock)
            val store = FakeOpenStateStore()
            val listener = FakeListener()
            val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

            tracker.updateAutomations(listOf(triggerAuto, stateAuto))
            tracker.onReading(Metric.BATTERY_LEVEL, 15.0)

            assertEquals(0, listener.fires.size) // trigger never fires on baseline
            assertEquals(1, listener.enters.size) // state enters on baseline
        }

        // Order 2: reading first, then automations
        run {
            val clock = FakeClock()
            val scheduler = FakeScheduler(clock)
            val store = FakeOpenStateStore()
            val listener = FakeListener()
            val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

            tracker.onReading(Metric.BATTERY_LEVEL, 15.0)
            tracker.updateAutomations(listOf(triggerAuto, stateAuto))

            assertEquals(0, listener.fires.size) // trigger never fires on baseline
            assertEquals(1, listener.enters.size) // state enters on baseline
            assertEquals(setOf("s1"), store.ids)
        }
    }

    @Test
    fun holdDelaysExecutionAndCanBeCancelled() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val stateAuto =
            Automation(
                id = "s1",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0, holdSeconds = 5),
                    ),
            )
        tracker.updateAutomations(listOf(stateAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // baseline inactive
        assertEquals(0, listener.enters.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 15.0) // active, schedules hold 5s
        assertEquals(0, listener.enters.size)

        scheduler.advanceBy(4_000)
        assertEquals(0, listener.enters.size)

        scheduler.advanceBy(1_000)
        assertEquals(1, listener.enters.size) // 5s reached, enters

        // Exit is immediate
        tracker.onReading(Metric.BATTERY_LEVEL, 25.0)
        assertEquals(1, listener.exits.size)

        // False before hold cancels hold
        tracker.onReading(Metric.BATTERY_LEVEL, 15.0) // becomes active again
        scheduler.advanceBy(2_000)
        tracker.onReading(Metric.BATTERY_LEVEL, 25.0) // goes false before 5s
        scheduler.advanceBy(4_000)
        assertEquals(1, listener.enters.size) // still 1 enter, hold was cancelled
    }

    @Test
    fun cooldownSkipsIntermediateEdges() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val triggerAuto =
            Automation(
                id = "t1",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0, hysteresis = 2.0),
                        cooldownSeconds = 10,
                    ),
            )
        tracker.updateAutomations(listOf(triggerAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 25.0) // baseline inactive

        tracker.onReading(Metric.BATTERY_LEVEL, 20.0) // fires (edge 25 -> 20)
        assertEquals(1, listener.fires.size)

        // Re-arm: goes to 23 (inactive with hysteresis), then back to 20 inside cooldown (5s < 10s)
        clock.advance(5_000)
        tracker.onReading(Metric.BATTERY_LEVEL, 23.0)
        tracker.onReading(Metric.BATTERY_LEVEL, 20.0)
        assertEquals(1, listener.fires.size) // skipped due to cooldown

        // Re-arm again after cooldown expires (15s > 10s)
        clock.advance(10_000)
        tracker.onReading(Metric.BATTERY_LEVEL, 23.0)
        tracker.onReading(Metric.BATTERY_LEVEL, 20.0)
        assertEquals(2, listener.fires.size) // allowed after cooldown
    }

    @Test
    fun triggerRisesAboveAndFallsBelowFireOnceAndRearmWithMargin() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val risesAboveAuto =
            Automation(
                id = "t_rises",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.RISES_ABOVE, value = 50.0, hysteresis = 2.0),
                        cooldownSeconds = 5,
                    ),
            )

        // Baseline at 40 (not active)
        tracker.updateAutomations(listOf(risesAboveAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 40.0)
        assertEquals(0, listener.fires.size)

        // Rises to 51 -> fires once
        tracker.onReading(Metric.BATTERY_LEVEL, 51.0)
        assertEquals(1, listener.fires.size)

        // 52 -> no fire (still active)
        tracker.onReading(Metric.BATTERY_LEVEL, 52.0)
        assertEquals(1, listener.fires.size)

        // Drops to 49 (within hysteresis a - h = 48 -> still active)
        clock.advance(10_000L)
        tracker.onReading(Metric.BATTERY_LEVEL, 49.0)
        assertEquals(1, listener.fires.size)

        // Goes back to 51 -> does not fire because it never went false (not re-armed)
        tracker.onReading(Metric.BATTERY_LEVEL, 51.0)
        assertEquals(1, listener.fires.size)

        // Drops to 47 (<= 48, so it deactivates and re-arms)
        tracker.onReading(Metric.BATTERY_LEVEL, 47.0)
        assertEquals(1, listener.fires.size)

        // Rises above 50 again -> fires second time
        clock.advance(10_000L)
        tracker.onReading(Metric.BATTERY_LEVEL, 51.0)
        assertEquals(2, listener.fires.size)
    }

    @Test
    fun pauseRunsNoExitActionsAndKeepsOpenSet() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker1 = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val stateAuto =
            Automation(
                id = "s_pause",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                    ),
            )

        tracker1.updateAutomations(listOf(stateAuto))
        tracker1.onReading(Metric.BATTERY_LEVEL, 15.0)
        assertEquals(1, listener.enters.size)
        assertEquals(setOf("s_pause"), store.ids)

        // Pause tracker: no exits, open set preserved
        tracker1.pause()
        assertEquals(0, listener.exits.size)
        assertEquals(setOf("s_pause"), store.ids)

        // New tracker with same store: does NOT re-enter state that is still true
        val listener2 = FakeListener()
        val tracker2 = SystemMetricTracker(clock::timeMs, scheduler, store, listener2)
        tracker2.updateAutomations(listOf(stateAuto))
        tracker2.onReading(Metric.BATTERY_LEVEL, 15.0)
        assertEquals(0, listener2.enters.size)
        assertEquals(0, listener2.exits.size)
        assertEquals(setOf("s_pause"), store.ids)
    }

    @Test
    fun removalThroughUpdateAutomationsEmptyListRunsExitOnce() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val stateAuto =
            Automation(
                id = "s_remove",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                    ),
            )

        tracker.updateAutomations(listOf(stateAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 15.0)
        assertEquals(1, listener.enters.size)
        assertEquals(setOf("s_remove"), store.ids)

        tracker.updateAutomations(emptyList())
        assertEquals(1, listener.exits.size)
        assertEquals(emptySet<String>(), store.ids)
    }

    @Test
    fun restoredOpenIdDisabledExitsOnceAndMissingAutomationDropped() {
        val disabledAuto =
            Automation(
                id = "s_disabled",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                    ),
                isEnabled = false,
            )

        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore(setOf("s_disabled", "s_missing"))
        val listener = FakeListener()

        val tracker =
            SystemMetricTracker(
                clock = clock::timeMs,
                scheduler = scheduler,
                openStore = store,
                listener = listener,
                findAutomation = { id -> if (id == "s_disabled") disabledAuto else null },
            )

        tracker.updateAutomations(emptyList())

        assertEquals(1, listener.exits.size)
        assertEquals("s_disabled", listener.exits.first().id)
        assertEquals(emptySet<String>(), store.ids)
    }

    @Test
    fun closeGuaranteeTransitions() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val action1 = Action.ToggleFlashlight
        val action2 = Action.SendNotification(notificationTitle = "Exit", body = "Body")
        val stateAuto =
            Automation(
                id = "s1",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                    ),
                exitActions = listOf(action1),
            )

        // 1. Enter state
        tracker.updateAutomations(listOf(stateAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 15.0)
        assertEquals(1, listener.enters.size)
        assertEquals(setOf("s1"), store.ids)

        // 2. Value exit
        tracker.onReading(Metric.BATTERY_LEVEL, 25.0)
        assertEquals(1, listener.exits.size)
        assertEquals(emptySet<String>(), store.ids)

        // 3. Enter again, then remove while open -> exit with old actions
        tracker.onReading(Metric.BATTERY_LEVEL, 15.0)
        assertEquals(2, listener.enters.size)
        assertEquals(setOf("s1"), store.ids)

        tracker.updateAutomations(emptyList()) // removed/disabled
        assertEquals(2, listener.exits.size)
        assertEquals(listOf(action1), listener.exits.last().exitActionList)
        assertEquals(emptySet<String>(), store.ids)

        // 4. Condition edited -> exit then re-baseline
        tracker.updateAutomations(listOf(stateAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 15.0) // open again
        assertEquals(3, listener.enters.size)

        val editedCondition =
            stateAuto.copy(
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.GTE, value = 80.0),
                    ),
            )
        tracker.updateAutomations(listOf(editedCondition))
        assertEquals(3, listener.exits.size) // exited old condition
        assertEquals(emptySet<String>(), store.ids) // 15 is not >= 80, so new condition is not entered

        // 5. Actions-only edit -> no exit, later exit uses new actions
        val reentered = stateAuto.copy(exitActions = listOf(action1))
        tracker.updateAutomations(listOf(reentered)) // 15 <= 20, enters
        assertEquals(4, listener.enters.size)
        val initialExitCount = listener.exits.size

        val editedActionsOnly = reentered.copy(exitActions = listOf(action2))
        tracker.updateAutomations(listOf(editedActionsOnly))
        assertEquals(initialExitCount, listener.exits.size) // no exit on actions edit

        tracker.onReading(Metric.BATTERY_LEVEL, 25.0)
        assertEquals(initialExitCount + 1, listener.exits.size)
        assertEquals(listOf(action2), listener.exits.last().exitActionList)

        // 6. updateAutomations(emptyList()) exits all and saves empty set
        tracker.onReading(Metric.BATTERY_LEVEL, 15.0)
        assertEquals(5, listener.enters.size)
        assertEquals(setOf("s1"), store.ids)

        tracker.updateAutomations(emptyList())
        assertEquals(initialExitCount + 2, listener.exits.size)
        assertEquals(emptySet<String>(), store.ids)

        // 7. Exit never called for a never-entered state
        val neverEntered =
            Automation(
                id = "s_never",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.GTE, value = 90.0),
                    ),
            )
        tracker.updateAutomations(listOf(neverEntered))
        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // not >= 90
        val exitCountBeforeRemove = listener.exits.size
        tracker.updateAutomations(emptyList()) // removed
        assertEquals(exitCountBeforeRemove, listener.exits.size) // no exit called

        // 8. Same list twice -> no callbacks
        tracker.updateAutomations(listOf(stateAuto))
        val currentEnters = listener.enters.size
        val currentExits = listener.exits.size
        tracker.updateAutomations(listOf(stateAuto))
        assertEquals(currentEnters, listener.enters.size)
        assertEquals(currentExits, listener.exits.size)
    }

    @Test
    fun restoredOpenStateHandling() {
        val stateAuto =
            Automation(
                id = "s_restored",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                    ),
            )

        // Case A: restored open + still true -> no enter callback, marked open
        run {
            val clock = FakeClock()
            val scheduler = FakeScheduler(clock)
            val store = FakeOpenStateStore(setOf("s_restored"))
            val listener = FakeListener()
            val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

            tracker.updateAutomations(listOf(stateAuto))
            tracker.onReading(Metric.BATTERY_LEVEL, 15.0) // still true

            assertEquals(0, listener.enters.size) // no enter!
            assertEquals(0, listener.exits.size)
            assertEquals(setOf("s_restored"), store.ids)
        }

        // Case B: restored open + now false -> exit callback
        run {
            val clock = FakeClock()
            val scheduler = FakeScheduler(clock)
            val store = FakeOpenStateStore(setOf("s_restored"))
            val listener = FakeListener()
            val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

            tracker.updateAutomations(listOf(stateAuto))
            tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // false!

            assertEquals(0, listener.enters.size)
            assertEquals(1, listener.exits.size) // exit called!
            assertEquals(emptySet<String>(), store.ids)
        }

        // Case C: restored but missing from first update -> dropped, no callbacks
        run {
            val clock = FakeClock()
            val scheduler = FakeScheduler(clock)
            val store = FakeOpenStateStore(setOf("s_restored"))
            val listener = FakeListener()
            val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

            tracker.updateAutomations(emptyList()) // missing from list
            tracker.onReading(Metric.BATTERY_LEVEL, 50.0)

            assertEquals(0, listener.enters.size)
            assertEquals(0, listener.exits.size)
            assertEquals(emptySet<String>(), store.ids)
        }

        // Restored open unbaselined, removed from updateAutomations -> exit callback
        run {
            val clock = FakeClock()
            val scheduler = FakeScheduler(clock)
            val store = FakeOpenStateStore(setOf("s_restored"))
            val listener = FakeListener()
            val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

            tracker.updateAutomations(listOf(stateAuto))
            tracker.updateAutomations(emptyList())

            assertEquals(0, listener.enters.size)
            assertEquals(1, listener.exits.size)
            assertEquals(emptySet<String>(), store.ids)
        }
    }

    @Test
    fun firstFireAllowedWhenClockStartsAtZero() {
        val clock = FakeClock(timeMs = 0L)
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val triggerAuto =
            Automation(
                id = "t_zero",
                type = Automation.Type.TRIGGER,
                trigger =
                    Trigger.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison = Comparison(operator = Comparison.Operator.LTE, value = 20.0),
                        cooldownSeconds = 60,
                    ),
            )
        tracker.updateAutomations(listOf(triggerAuto))
        tracker.onReading(Metric.BATTERY_LEVEL, 25.0)
        tracker.onReading(Metric.BATTERY_LEVEL, 20.0)
        assertEquals(1, listener.fires.size)
    }

    @Test
    fun stateBatteryLevelMappedThroughTracker() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val batteryLevelState =
            Automation(
                id = "bl1",
                type = Automation.Type.STATE,
                state = DIYState.BatteryLevel(minLevel = 20, maxLevel = 80),
            )

        tracker.updateAutomations(listOf(batteryLevelState))
        tracker.onReading(Metric.BATTERY_LEVEL, 10.0) // out of range
        assertEquals(0, listener.enters.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 50.0) // in range: enters!
        assertEquals(1, listener.enters.size)
        assertEquals(setOf("bl1"), store.ids)

        tracker.updateAutomations(emptyList()) // removed: exits!
        assertEquals(1, listener.exits.size)
        assertEquals(emptySet<String>(), store.ids)
    }

    @Test
    fun legacyBatteryLevelStateTransitionToSystemMetricKeepsStateOpen() {
        val clock = FakeClock()
        val scheduler = FakeScheduler(clock)
        val store = FakeOpenStateStore()
        val listener = FakeListener()
        val tracker = SystemMetricTracker(clock::timeMs, scheduler, store, listener)

        val legacy =
            Automation(
                id = "bl_convert",
                type = Automation.Type.STATE,
                state = DIYState.BatteryLevel(minLevel = 80, maxLevel = 100),
            )

        tracker.updateAutomations(listOf(legacy))
        tracker.onReading(Metric.BATTERY_LEVEL, 85.0)
        assertEquals(1, listener.enters.size)
        assertEquals(0, listener.exits.size)
        assertEquals(setOf("bl_convert"), store.ids)

        val converted =
            Automation(
                id = "bl_convert",
                type = Automation.Type.STATE,
                state =
                    DIYState.SystemMetric(
                        metric = Metric.BATTERY_LEVEL,
                        comparison =
                            Comparison(
                                operator = Comparison.Operator.BETWEEN,
                                value = 80.0,
                                upper = 100.0,
                                hysteresis = 0.0,
                            ),
                    ),
            )

        tracker.updateAutomations(listOf(converted))
        assertEquals(1, listener.enters.size)
        assertEquals(0, listener.exits.size)

        tracker.onReading(Metric.BATTERY_LEVEL, 70.0)
        assertEquals(1, listener.enters.size)
        assertEquals(1, listener.exits.size)
        assertEquals(emptySet<String>(), store.ids)
    }
}
