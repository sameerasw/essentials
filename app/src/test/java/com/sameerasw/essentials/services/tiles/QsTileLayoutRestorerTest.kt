/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 */

package com.sameerasw.essentials.services.tiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QsTileLayoutRestorerTest {
    private val pkg = "com.sameerasw.essentials"
    private val caffeinate = "$pkg.services.tiles.CaffeinateTileService"
    private val sound = "$pkg.services.tiles.SoundModeTileService"
    private fun spec(cls: String) = QsTileLayoutRestorer.customSpec(pkg, cls)

    @Test
    fun customClass_parsesShortAndFullForms() {
        assertEquals(caffeinate, QsTileLayoutRestorer.customClass("custom($pkg/.services.tiles.CaffeinateTileService)"))
        assertEquals(caffeinate, QsTileLayoutRestorer.customClass("custom($pkg/$caffeinate)"))
        assertNull(QsTileLayoutRestorer.customClass("wifi"))
        assertNull(QsTileLayoutRestorer.customClass("custom(broken)"))
    }

    @Test
    fun customSpec_usesShortFormLikeSystemUi() {
        assertEquals("custom($pkg/.services.tiles.CaffeinateTileService)", spec(caffeinate))
        assertEquals("custom($pkg/other.Tile)", QsTileLayoutRestorer.customSpec(pkg, "other.Tile"))
    }

    @Test
    fun merge_putsTilesBackAfterTheirOldNeighbour() {
        val saved = listOf("wifi", spec(caffeinate), "bt", "flashlight", spec(sound), "dnd")
        val current = listOf("wifi", "bt", "flashlight", "dnd")
        val merged = QsTileLayoutRestorer.merge(current, saved, setOf(caffeinate, sound), pkg)
        assertEquals(listOf("wifi", spec(caffeinate), "bt", "flashlight", spec(sound), "dnd"), merged)
    }

    @Test
    fun merge_keepsCurrentOrderAndUsesNextNeighbourWhenNoPrevious() {
        val saved = listOf(spec(caffeinate), "bt", "wifi")
        val current = listOf("dnd", "wifi", "bt")
        val merged = QsTileLayoutRestorer.merge(current, saved, setOf(caffeinate), pkg)
        assertEquals(listOf("dnd", "wifi", spec(caffeinate), "bt"), merged)
    }

    @Test
    fun merge_appendsTilesMissingFromSavedLayout() {
        val merged = QsTileLayoutRestorer.merge(listOf("wifi"), emptyList(), setOf(sound, caffeinate), pkg)
        assertEquals(listOf("wifi", spec(caffeinate), spec(sound)), merged)
    }

    @Test
    fun merge_doesNotDuplicateTilesAlreadyPresentInAnyForm() {
        val current = listOf("wifi", "custom($pkg/$caffeinate)")
        val merged = QsTileLayoutRestorer.merge(current, listOf(spec(caffeinate), "wifi"), setOf(caffeinate), pkg)
        assertEquals(current, merged)
    }

    @Test
    fun tilesToRestore_prefersSavedLayoutOverStaleFlags() {
        assertEquals(setOf(caffeinate), QsTileLayoutRestorer.tilesToRestore(setOf(caffeinate), setOf(caffeinate, sound)))
        assertEquals(setOf(caffeinate, sound), QsTileLayoutRestorer.tilesToRestore(emptySet(), setOf(caffeinate, sound)))
    }

    @Test
    fun alreadyAdded_trustsLiveLayoutOverStaleFlags() {
        assertEquals(setOf(caffeinate), QsTileLayoutRestorer.alreadyAdded(setOf(caffeinate), setOf(caffeinate, sound)))
        assertEquals(setOf(caffeinate, sound), QsTileLayoutRestorer.alreadyAdded(null, setOf(caffeinate, sound)))
    }
}
