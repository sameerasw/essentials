/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Module
 * File: KeyboardLayoutBuilder.kt
 * Description: Places keys with zero-gap logical tiles, edge expansion, and spacebar boundary stealing.
 */

package com.sameerasw.essentials.ui.ime.touch

internal object KeyboardLayoutBuilder {
    fun place(
        rows: List<RowDef>,
        width: Float,
        totalHeight: Float,
        insetH: Float,
        insetV: Float,
    ): KeyboardLayout {
        if (rows.isEmpty() || width <= 0f || totalHeight <= 0f) {
            return KeyboardLayout(emptyList(), width, totalHeight, 0f, 0f, rows.size)
        }

        val rowHeight = totalHeight / rows.size.toFloat()
        val specs = ArrayList<KeySpec>(48)

        rows.forEachIndexed { rowIndex, row ->
            val y = rowIndex * rowHeight
            val totalWeight = row.startSpacerWeight + row.endSpacerWeight + row.keys.sumOf { it.weight.toDouble() }.toFloat()
            if (totalWeight <= 0f) return@forEachIndexed

            var currentX = (row.startSpacerWeight / totalWeight) * width
            val keyCount = row.keys.size

            row.keys.forEachIndexed { keyIndex, def ->
                val cellWidth = (def.weight / totalWeight) * width
                val visualLeft = currentX
                val visualRight = currentX + cellWidth

                var logicalLeft = visualLeft
                var logicalRight = visualRight

                if (row.expandEdges && keyIndex == 0 && row.startSpacerWeight == 0f) {
                    logicalLeft = 0f
                }
                if (row.expandEdges && keyIndex == keyCount - 1 && row.endSpacerWeight == 0f) {
                    logicalRight = width
                }

                val logical = Bounds(logicalLeft, y, logicalRight, y + rowHeight)
                val visual = Bounds(
                    visualLeft + insetH,
                    y + insetV,
                    visualRight - insetH,
                    y + rowHeight - insetV,
                )

                specs += KeySpec(
                    id = def.id,
                    label = def.label,
                    output = def.output,
                    action = def.action,
                    logical = logical,
                    visual = visual,
                    row = rowIndex,
                    secondary = def.secondary,
                    iconRes = def.iconRes,
                )

                currentX += cellWidth
            }
        }

        val letterWidth = specs.filter { it.action == KeyAction.CHAR }
            .map { it.logical.width }
            .average()
            .toFloat()
            .takeIf { it.isFinite() && it > 0f }
            ?: (width * KeyGeometry.LETTER)

        stealSpaceHits(specs, fraction = KeyGeometry.SPACE_STEAL)

        return KeyboardLayout(specs, width, totalHeight, rowHeight, letterWidth, rows.size)
    }

    /**
     * Spacebar is tapped far more frequently than comma, dot, or symbols;
     * steal a portion of the hit area from neighboring keys in the same row.
     */
    private fun stealSpaceHits(specs: ArrayList<KeySpec>, fraction: Float = KeyGeometry.SPACE_STEAL) {
        val spaceIndices = specs.indices.filter { specs[it].action == KeyAction.SPACE }
        for (spaceIndex in spaceIndices) {
            val space = specs[spaceIndex]
            val row = specs.mapIndexed { index, key -> index to key }
                .filter { it.second.row == space.row }
                .sortedBy { it.second.logical.left }

            val position = row.indexOfFirst { it.first == spaceIndex }
            if (position < 0) continue

            // Steal from left neighbor
            if (position > 0) {
                stealFromNeighbor(specs, spaceIndex, row[position - 1].first, fromRight = true, fraction)
            }
            // Steal from right neighbor
            if (position < row.lastIndex) {
                stealFromNeighbor(specs, spaceIndex, row[position + 1].first, fromRight = false, fraction)
            }
        }
    }

    private fun stealFromNeighbor(
        specs: ArrayList<KeySpec>,
        spaceIndex: Int,
        neighborIndex: Int,
        fromRight: Boolean,
        fraction: Float,
    ) {
        val space = specs[spaceIndex]
        val neighbor = specs[neighborIndex]
        val amount = neighbor.logical.width * fraction
        if (amount <= 0f) return

        if (fromRight) {
            specs[neighborIndex] = neighbor.copy(
                logical = neighbor.logical.copy(right = neighbor.logical.right - amount),
            )
            specs[spaceIndex] = space.copy(
                logical = space.logical.copy(left = space.logical.left - amount),
            )
        } else {
            specs[neighborIndex] = neighbor.copy(
                logical = neighbor.logical.copy(left = neighbor.logical.left + amount),
            )
            specs[spaceIndex] = space.copy(
                logical = space.logical.copy(right = space.logical.right + amount),
            )
        }
    }
}
