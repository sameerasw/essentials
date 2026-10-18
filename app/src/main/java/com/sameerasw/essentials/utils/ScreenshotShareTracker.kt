/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Utilities - Screenshots
 * File: ScreenshotShareTracker.kt
 * Description: Follows a screenshot share from the preview's Share tap through the share sheet to the target app.
 */

package com.sameerasw.essentials.utils

class ScreenshotShareTracker(
    private val shareSheetPackage: String,
    private val imageEditorPackage: String,
    private val timeoutMs: Long = SHARE_TIMEOUT_MS,
) {
    enum class Stage { SHARE_TAPPED, CHOOSER_OPEN, IN_TARGET }

    var stage: Stage? = null
        private set
    var sharedAt = 0L
        private set
    private var originPackage: String? = null
    private var targetPackage: String? = null

    fun onShareTapped(
        now: Long,
        foregroundPackage: String?,
    ) {
        stage = Stage.SHARE_TAPPED
        sharedAt = now
        originPackage = foregroundPackage
        targetPackage = null
    }

    fun onForegroundPackage(
        packageName: String,
        now: Long,
    ): Boolean {
        val current = stage ?: return false
        if (current != Stage.IN_TARGET && now - sharedAt > timeoutMs) {
            reset()
            return false
        }
        when (current) {
            Stage.SHARE_TAPPED -> if (packageName == shareSheetPackage) stage = Stage.CHOOSER_OPEN

            Stage.CHOOSER_OPEN ->
                when (packageName) {
                    shareSheetPackage -> Unit
                    originPackage, imageEditorPackage -> reset()
                    else -> {
                        targetPackage = packageName
                        stage = Stage.IN_TARGET
                    }
                }

            Stage.IN_TARGET ->
                if (packageName != targetPackage) {
                    reset()
                    return true
                }
        }
        return false
    }

    fun onScreenOff() {
        if (stage != Stage.IN_TARGET) reset()
    }

    fun reset() {
        stage = null
        originPackage = null
        targetPackage = null
    }

    companion object {
        const val SHARE_TIMEOUT_MS = 30_000L
    }
}
