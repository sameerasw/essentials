/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Controllers
 * File: ZenModeController.kt
 * Description: Lists and switches the system Modes (Do Not Disturb rules) through the notification service over Shizuku.
 */

package com.sameerasw.essentials.domain.controller

import android.app.AutomaticZenRule
import android.os.Build
import android.os.IBinder
import android.service.notification.Condition
import androidx.annotation.RequiresApi
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException

// Apps can only switch modes they own; shell holds MANAGE_NOTIFICATIONS, which covers every mode
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal object ZenModeController {
    private const val MANAGER = "android.app.INotificationManager"

    // Rules apps create when they call setInterruptionFilter, not modes the user sees in Settings
    private const val IMPLICIT_RULE_PREFIX = "implicit_"

    data class Mode(
        val id: String,
        val name: String,
        val isEnabled: Boolean,
    )

    fun getModes(): List<Mode> =
        rules()
            .filterKeys { !it.startsWith(IMPLICIT_RULE_PREFIX) }
            .map { (id, rule) -> Mode(id, rule.name?.toString().orEmpty(), rule.isEnabled) }
            .sortedBy { it.name.lowercase() }

    // Falls back to the name so the action survives the mode being recreated with a new id
    fun setActive(
        modeId: String,
        modeName: String,
        active: Boolean,
    ): Boolean {
        val rules = rules()
        val (id, rule) =
            rules.entries.firstOrNull { it.key == modeId }?.toPair()
                ?: rules.entries.firstOrNull { it.value.name?.toString() == modeName }?.toPair()
                ?: return false
        val condition =
            Condition(
                rule.conditionId,
                rule.name?.toString().orEmpty(),
                if (active) Condition.STATE_TRUE else Condition.STATE_FALSE,
                Condition.SOURCE_USER_ACTION,
            )
        invoke("setAutomaticZenRuleState", arrayOf(String::class.java, Condition::class.java), id, condition)
        return true
    }

    @Suppress("UNCHECKED_CAST")
    private fun rules(): Map<String, AutomaticZenRule> =
        invoke("getAutomaticZenRules", emptyArray()) as? Map<String, AutomaticZenRule> ?: emptyMap()

    private fun invoke(
        name: String,
        types: Array<Class<*>>,
        vararg args: Any?,
    ): Any? {
        val binder = requireNotNull(SystemServiceHelper.getSystemService("notification")) { "notification unavailable" }
        val manager =
            Class.forName("$MANAGER\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(binder))!!
        val method = Class.forName(MANAGER).getMethod(name, *types)
        return try {
            method.invoke(manager, *args)
        } catch (e: InvocationTargetException) {
            throw (e.cause as? Exception ?: e)
        }
    }
}
