/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Background Services & Receivers
 * File: StateEntries.kt
 * Description: Background service component for StateEntries.kt.
 */

package com.sameerasw.essentials.services.automation.modules

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.Automation

// Tracks which state automations have run their entry actions, so every entry gets exactly one exit
class StateEntries {
    // The automation as it was when it entered, so its own exit actions run even after an edit or delete
    private val entered = mutableMapOf<String, Automation>()

    // Returns the action lists to run, in order: exits for automations that are gone or no longer active, then entries
    @Synchronized
    fun sync(
        automations: List<Automation>,
        isActive: (Automation) -> Boolean,
    ): List<List<Action>> {
        val exits = mutableListOf<List<Action>>()
        val entries = mutableListOf<List<Action>>()

        val owned = automations.filter { it.type == Automation.Type.STATE && it.isEnabled }
        val ownedMap = owned.associateBy { it.id }

        val removedIds = entered.keys.filter { it !in ownedMap }
        for (id in removedIds) {
            val old = entered.remove(id)
            if (old != null && old.exitActionList.isNotEmpty()) {
                exits.add(old.exitActionList)
            }
        }

        for (auto in owned) {
            val active = isActive(auto)
            val isEntered = entered.containsKey(auto.id)
            if (active && !isEntered) {
                entered[auto.id] = auto
                if (auto.entryActionList.isNotEmpty()) {
                    entries.add(auto.entryActionList)
                }
            } else if (!active && isEntered) {
                val old = entered.remove(auto.id)
                if (old != null && old.exitActionList.isNotEmpty()) {
                    exits.add(old.exitActionList)
                }
            } else if (active && isEntered) {
                entered[auto.id] = auto
            }
        }

        return exits + entries
    }

    // Exits for everything entered, then forgets them
    @Synchronized
    fun closeAll(): List<List<Action>> {
        val exits =
            entered.values
                .map { it.exitActionList }
                .filter { it.isNotEmpty() }
        entered.clear()
        return exits
    }
}
