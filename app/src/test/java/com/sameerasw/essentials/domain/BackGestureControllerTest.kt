package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.controller.BackGestureController
import com.sameerasw.essentials.domain.controller.BackGestureStore
import com.sameerasw.essentials.domain.model.BackGestureAppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackGestureControllerTest {
    private class FakeStore(
        var left: Float = 1f,
        var right: Float = 1f,
    ) : BackGestureStore {
        var owner: String? = null
        val restore = mutableMapOf(true to 1f, false to 1f)
        var writes = 0
        var failWrites = false

        override fun readSystemScale(left: Boolean) = if (left) this.left else right

        override fun writeSystemScale(
            left: Boolean,
            scale: Float,
        ): Boolean {
            if (failWrites) return false
            writes++
            if (left) this.left = scale else right = scale
            return true
        }

        override fun getOverrideOwner() = owner

        override fun setOverrideOwner(owner: String?) {
            this.owner = owner
        }

        override fun getRestoreScale(left: Boolean) = restore.getValue(left)

        override fun setRestoreScale(
            left: Boolean,
            scale: Float,
        ) {
            restore[left] = scale
        }
    }

    private val configs = mapOf("game" to BackGestureAppConfig("game", leftScale = 0f, rightScale = 0.5f))

    @Test
    fun appOverrideRestoresSystemWidthsOnLeave() {
        val store = FakeStore(left = 0.8f, right = 0.9f)
        val controller = BackGestureController(store)

        controller.onAppChanged("game", configs)
        assertEquals(0f, store.left)
        assertEquals(0.5f, store.right)
        assertEquals("game", store.owner)

        controller.onAppChanged("launcher", configs)
        assertEquals(0.8f, store.left)
        assertEquals(0.9f, store.right)
        assertNull(store.owner)
    }

    @Test
    fun overrideSurvivesRestartBecauseOwnerIsPersisted() {
        val store = FakeStore(left = 0.8f, right = 0.9f)
        BackGestureController(store).onAppChanged("game", configs)

        BackGestureController(store).onAppChanged("launcher", configs)
        assertEquals(0.8f, store.left)
        assertEquals(0.9f, store.right)
    }

    @Test
    fun switchingBetweenUnlistedAppsWritesNothing() {
        val store = FakeStore()
        val controller = BackGestureController(store)

        controller.onAppChanged("a", configs)
        controller.onAppChanged("b", configs)
        assertEquals(0, store.writes)
    }

    @Test
    fun reenteringTheSameAppSkipsUnchangedWrites() {
        val store = FakeStore()
        val controller = BackGestureController(store)

        controller.onAppChanged("game", configs)
        val writes = store.writes
        controller.onAppChanged("game", configs)
        assertEquals(writes, store.writes)
    }

    @Test
    fun removingTheAppliedAppRestores() {
        val store = FakeStore(left = 0.7f, right = 0.7f)
        val controller = BackGestureController(store)

        controller.onAppChanged("game", configs)
        controller.onAppRemoved("game")
        assertEquals(0.7f, store.left)
        assertEquals(0.7f, store.right)
        assertNull(store.owner)
    }

    @Test
    fun systemChangesAreReadLiveWhenNoOverride() {
        val store = FakeStore()
        val controller = BackGestureController(store)

        store.left = 0.6f
        assertEquals(0.6f, controller.defaultScale(left = true))
    }

    @Test
    fun defaultChangesDuringOverrideApplyOnRestore() {
        val store = FakeStore()
        val controller = BackGestureController(store)

        controller.onAppChanged("game", configs)
        controller.setDefaultScale(left = true, 0.4f)
        assertEquals(0f, store.left)
        assertEquals(0.4f, controller.defaultScale(left = true))

        controller.onAppChanged("launcher", configs)
        assertEquals(0.4f, store.left)
    }

    @Test
    fun failedRestoreKeepsOwnerForRetry() {
        val store = FakeStore()
        val controller = BackGestureController(store)

        controller.onAppChanged("game", configs)
        store.failWrites = true
        controller.onAppChanged("launcher", configs)
        assertEquals("game", store.owner)

        store.failWrites = false
        controller.onAppChanged("launcher", configs)
        assertEquals(1f, store.left)
        assertNull(store.owner)
    }

    @Test
    fun automationOffWinsOverAppsUntilTurnedOn() {
        val store = FakeStore()
        val controller = BackGestureController(store)

        controller.onAppChanged("game", configs)
        controller.turnOff()
        assertEquals(0f, store.right)

        controller.onAppChanged("launcher", configs)
        assertEquals(0f, store.left)
        assertEquals(0f, store.right)

        controller.turnOn()
        assertEquals(1f, store.left)
        assertEquals(1f, store.right)
        assertNull(store.owner)
    }

    @Test
    fun turnOnLeavesAppOverrideAlone() {
        val store = FakeStore()
        val controller = BackGestureController(store)

        controller.onAppChanged("game", configs)
        controller.turnOn()
        assertEquals("game", store.owner)
        assertEquals(0f, store.left)
    }
}
