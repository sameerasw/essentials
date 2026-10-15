/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Controllers
 * File: BackGestureController.kt
 * Description: Applies per-app and automation back gesture widths and restores the system widths afterwards.
 */

package com.sameerasw.essentials.domain.controller

import com.sameerasw.essentials.domain.model.BackGestureAppConfig
import kotlin.math.abs

interface BackGestureStore {
    fun readSystemScale(left: Boolean): Float

    fun writeSystemScale(
        left: Boolean,
        scale: Float,
    ): Boolean

    fun getOverrideOwner(): String?

    fun setOverrideOwner(owner: String?)

    fun getRestoreScale(left: Boolean): Float

    fun setRestoreScale(
        left: Boolean,
        scale: Float,
    )
}

class BackGestureController(
    private val store: BackGestureStore,
) {
    val overrideOwner: String?
        get() = store.getOverrideOwner()

    fun defaultScale(left: Boolean): Float = if (overrideOwner != null) store.getRestoreScale(left) else store.readSystemScale(left)

    fun setDefaultScale(
        left: Boolean,
        scale: Float,
    ) {
        if (overrideOwner != null) store.setRestoreScale(left, scale) else store.writeSystemScale(left, scale)
    }

    fun applyOverride(
        owner: String,
        left: Float,
        right: Float,
    ) {
        if (overrideOwner == null) {
            store.setRestoreScale(left = true, store.readSystemScale(left = true))
            store.setRestoreScale(left = false, store.readSystemScale(left = false))
        }
        store.setOverrideOwner(owner)
        writeIfChanged(left = true, left)
        writeIfChanged(left = false, right)
    }

    fun restore() {
        if (overrideOwner == null) return
        val leftOk = writeIfChanged(left = true, store.getRestoreScale(left = true))
        val rightOk = writeIfChanged(left = false, store.getRestoreScale(left = false))
        if (leftOk && rightOk) store.setOverrideOwner(null)
    }

    fun onAppChanged(
        packageName: String,
        configs: Map<String, BackGestureAppConfig>,
    ) {
        val owner = overrideOwner
        if (owner == AUTOMATION_OWNER) return
        val config = configs[packageName]
        when {
            config != null -> applyOverride(packageName, config.leftScale, config.rightScale)
            owner != null -> restore()
        }
    }

    fun onAppRemoved(packageName: String) {
        if (overrideOwner == packageName) restore()
    }

    fun turnOff() = applyOverride(AUTOMATION_OWNER, 0f, 0f)

    fun turnOn() {
        if (overrideOwner == AUTOMATION_OWNER) restore()
    }

    private fun writeIfChanged(
        left: Boolean,
        scale: Float,
    ): Boolean = abs(store.readSystemScale(left) - scale) < 0.001f || store.writeSystemScale(left, scale)

    companion object {
        const val AUTOMATION_OWNER = "essentials:automation"
    }
}
