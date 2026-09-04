/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: KeyGeometry.kt
 * Description: Geometry constants and bounds models for intelligent touch decoding.
 */

package com.sameerasw.essentials.ui.ime.touch

internal object KeyGeometry {
    const val LETTER = 0.10f
    const val HYSTERESIS = 0.14f
    const val SEARCH_KEYS = 1.15f
    const val SIGMA_X = 0.45f
    const val SIGMA_Y = 0.52f
    const val CLEAR_CENTER = 0.70f
    const val SPACE_STEAL = 0.28f
    const val LONG_PRESS_MS = 400L
    const val DELETE_REPEAT_START_MS = 420L
    const val DELETE_REPEAT_MS = 60L
    const val SPACE_DRAG_DP = 12f
    const val DELETE_SWIPE_DP = 20f
    const val SWEEP_STEP_PX = 25f
}

internal data class Bounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f

    fun contains(x: Float, y: Float): Boolean = x >= left && x < right && y >= top && y < bottom

    fun inset(dx: Float, dy: Float): Bounds = Bounds(left + dx, top + dy, right - dx, bottom - dy)
}

internal enum class KeyAction {
    CHAR,
    SHIFT,
    DELETE,
    SPACE,
    ENTER,
    SYMBOLS,
    COMMA,
    DOT,
    EMOJI,
}

internal data class KeyDef(
    val id: String,
    val label: String,
    val output: String,
    val action: KeyAction = KeyAction.CHAR,
    val weight: Float = 1f,
    val secondary: String? = null,
    val iconRes: Int? = null,
)

internal data class RowDef(
    val keys: List<KeyDef>,
    val startSpacerWeight: Float = 0f,
    val endSpacerWeight: Float = 0f,
    val expandEdges: Boolean = true,
)

internal data class KeySpec(
    val id: String,
    val label: String,
    val output: String,
    val action: KeyAction,
    val logical: Bounds,
    val visual: Bounds,
    val row: Int,
    val secondary: String? = null,
    val iconRes: Int? = null,
) {
    val geometricCenterX: Float get() = visual.centerX
    val geometricCenterY: Float get() = visual.centerY
}

internal data class KeyboardLayout(
    val keys: List<KeySpec>,
    val width: Float,
    val height: Float,
    val rowHeight: Float,
    val letterWidth: Float,
    val rows: Int,
) {
    fun keyAtLogical(x: Float, y: Float): KeySpec? = keys.firstOrNull { it.logical.contains(x, y) }
    fun keyById(id: String): KeySpec? = keys.firstOrNull { it.id == id }
    fun rowKeys(row: Int): List<KeySpec> = keys.filter { it.row == row }
}

internal data class ScoredCandidate(
    val key: KeySpec,
    val spatial: Float,
    val distance: Float,
)

internal data class DecodeResult(
    val selected: KeySpec?,
    val candidates: List<ScoredCandidate>,
    val containing: KeySpec?,
    val clearCenter: Boolean,
)
