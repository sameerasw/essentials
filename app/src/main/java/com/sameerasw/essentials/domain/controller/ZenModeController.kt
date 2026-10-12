/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Controllers
 * File: ZenModeController.kt
 * Description: Lists and switches the system Modes (Do Not Disturb rules) through the system modes service over Shizuku.
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

    // Android 17 moves modes to their own service when refactor_modes_code is on; the old calls then throw
    private const val MODES_SERVICE = "contextual_mode"
    private const val MODES_MANAGER = "android.app.modes.IContextualModeManager"

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

    // Older releases return a map; newer ones a ParceledListSlice of AutomaticZenRule.AzrWithId
    @Suppress("UNCHECKED_CAST")
    private fun rules(): Map<String, AutomaticZenRule> =
        when (val result = invoke("getAutomaticZenRules", emptyArray())) {
            is Map<*, *> -> result as Map<String, AutomaticZenRule>
            null -> emptyMap()
            else -> {
                val list = result.javaClass.getMethod("getList").invoke(result) as List<Any>
                list.associate { item ->
                    val cls = item.javaClass
                    val id = cls.getDeclaredField("mId").apply { isAccessible = true }.get(item) as String
                    val rule = cls.getDeclaredField("mRule").apply { isAccessible = true }.get(item) as AutomaticZenRule
                    id to rule
                }
            }
        }

    private fun invoke(
        name: String,
        types: Array<Class<*>>,
        vararg args: Any?,
    ): Any? {
        SystemServiceHelper.getSystemService(MODES_SERVICE)?.let { binder ->
            try {
                return invoke(binder, MODES_MANAGER, name, types, *args)
            } catch (_: IllegalStateException) {
                // Registered but not handling modes yet, so use the notification service
            }
        }
        val binder = requireNotNull(SystemServiceHelper.getSystemService("notification")) { "notification unavailable" }
        return invoke(binder, MANAGER, name, types, *args)
    }

    private fun invoke(
        binder: IBinder,
        managerName: String,
        name: String,
        types: Array<Class<*>>,
        vararg args: Any?,
    ): Any? {
        val manager =
            Class.forName("$managerName\$Stub")
                .getMethod("asInterface", IBinder::class.java)
                .invoke(null, ShizukuBinderWrapper(binder))!!
        val method = Class.forName(managerName).getMethod(name, *types)
        return try {
            method.invoke(manager, *args)
        } catch (e: InvocationTargetException) {
            throw (e.cause as? Exception ?: e)
        }
    }
}
