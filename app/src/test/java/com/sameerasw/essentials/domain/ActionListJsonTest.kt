package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionListJsonTest {
    @Test
    fun listRoundTripKeepsOrderAndDuplicates() {
        val actions = listOf(Action.ToggleFlashlight, Action.ToggleFlashlight)
        assertEquals(actions, ActionGsonAdapter.listFromJson(ActionGsonAdapter.listToJson(actions)))
    }

    @Test
    fun unknownEntriesAreSkipped() {
        val json = """[{"type":"RemovedAction"},{"type":"ToggleFlashlight"}]"""
        assertEquals(listOf(Action.ToggleFlashlight), ActionGsonAdapter.listFromJson(json))
    }

    @Test
    fun invalidJsonGivesEmptyList() {
        assertEquals(emptyList<Action>(), ActionGsonAdapter.listFromJson("not json"))
    }

    @Test
    fun sendNotificationRoundTrip() {
        val actions = listOf(Action.SendNotification(notificationTitle = "Title", body = "Body", label = "Label"))
        assertEquals(actions, ActionGsonAdapter.listFromJson(ActionGsonAdapter.listToJson(actions)))
    }

    @Test
    fun sendNotificationMissingFieldsLoadEmpty() {
        val singleAction = ActionGsonAdapter.fromJson("""{"type":"SendNotification","title":"Hi"}""")
        assertEquals(Action.SendNotification(notificationTitle = "Hi", body = "", label = ""), singleAction)

        val json = """[{"type":"SendNotification","title":"Hi"}]"""
        val expected = listOf(Action.SendNotification(notificationTitle = "Hi", body = "", label = ""))
        assertEquals(expected, ActionGsonAdapter.listFromJson(json))
    }
}
