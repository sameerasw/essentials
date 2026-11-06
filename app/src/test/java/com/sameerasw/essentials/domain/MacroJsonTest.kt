package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import com.sameerasw.essentials.domain.diy.DIYRepository
import com.sameerasw.essentials.domain.diy.Macro
import org.junit.Assert.assertEquals
import org.junit.Test

class MacroJsonTest {
    @Test
    fun runMacroInListRoundTripsThroughActionGsonAdapter() {
        val actions = listOf(Action.RunMacro("abc"))
        assertEquals(actions, ActionGsonAdapter.listFromJson(ActionGsonAdapter.listToJson(actions)))
    }

    @Test
    fun typeOnlyLoadsAsDefaultEmptyId() {
        val json = """[{"type":"RunMacro"}]"""
        assertEquals(listOf(Action.RunMacro("")), ActionGsonAdapter.listFromJson(json))
    }

    @Test
    fun twoMacrosRoundTripThroughDIYRepository() {
        val macros =
            listOf(
                Macro(
                    id = "macro-1",
                    name = "Turn on light and wait",
                    actions = listOf(Action.TurnOnFlashlight, Action.Wait(3)),
                ),
                Macro(
                    id = "macro-2",
                    name = "Turn off light",
                    actions = listOf(Action.TurnOffFlashlight),
                ),
            )
        val json = DIYRepository.macrosToJson(macros)
        assertEquals(macros, DIYRepository.macrosFromJson(json))
    }

    @Test
    fun invalidJsonGivesEmptyList() {
        assertEquals(emptyList<Macro>(), DIYRepository.macrosFromJson("not json"))
    }

    @Test
    fun unknownActionTypesAreSkippedInsideMacro() {
        val json =
            """[{"id":"m1","name":"Test","actions":[{"type":"RemovedAction"},{"type":"ToggleFlashlight"}]}]"""
        val loaded = DIYRepository.macrosFromJson(json)
        val expected =
            listOf(
                Macro(
                    id = "m1",
                    name = "Test",
                    actions = listOf(Action.ToggleFlashlight),
                ),
            )
        assertEquals(expected, loaded)
    }
}
