package com.sameerasw.essentials.domain

import com.sameerasw.essentials.domain.diy.Action
import com.sameerasw.essentials.domain.diy.ActionGsonAdapter
import com.sameerasw.essentials.domain.diy.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTriggerTest {
    private val chat = "com.example.chat"

    @Test
    fun appOnlyMatchesAnyContentFromThatApp() {
        val trigger = Trigger.NotificationReceived(packageName = chat)
        assertTrue(trigger.matches(chat, "Alex", "hey"))
        assertFalse(trigger.matches("com.example.mail", "Alex", "hey"))
    }

    @Test
    fun textMatchesTitleOrBodyIgnoringCase() {
        val trigger = Trigger.NotificationReceived(textContains = "code")
        assertTrue(trigger.matches("any.app", "Your CODE", ""))
        assertTrue(trigger.matches("any.app", "Bank", "Verification code: 1234"))
        assertFalse(trigger.matches("any.app", "Bank", "Payment received"))
    }

    @Test
    fun appAndTextMustBothMatch() {
        val trigger = Trigger.NotificationReceived(packageName = chat, textContains = "urgent")
        assertTrue(trigger.matches(chat, "Boss", "Urgent: call me"))
        assertFalse(trigger.matches(chat, "Boss", "lunch?"))
        assertFalse(trigger.matches("com.example.mail", "Boss", "urgent"))
    }

    @Test
    fun waitKeepsItsDurationThroughJson() {
        val actions = listOf(Action.Wait(seconds = 30), Action.TurnOnCaffeinate)
        assertEquals(actions, ActionGsonAdapter.listFromJson(ActionGsonAdapter.listToJson(actions)))
    }
}
