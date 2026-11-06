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

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal object ZenModeController {
    private const val MANAGER = "android.app.INotificationManager"
    private const val MODES_SERVICE = "contextual_mode"
    private const val MODES_MANAGER = "android.app.modes.IContextualModeManager"
    private const val IMPLICIT_RULE_PREFIX = "implicit_"

    data class Mode(
        val id: String,
        val name: String,
        val isEnabled: Boolean,
        val type: Int = AutomaticZenRule.TYPE_UNKNOWN,
    )

    enum class Result { DONE, NOT_FOUND, NO_CONDITION }

    fun getModes(): List<Mode> =
        modeRules()
            .map { (id, rule) -> Mode(id, rule.name?.toString().orEmpty(), rule.isEnabled, rule.type) }
            .sortedBy { it.name.lowercase() }

    fun activeModes(): List<Mode> =
        modeRules()
            .filter { (id, rule) ->
                rule.isEnabled &&
                    (invoke("getAutomaticZenRuleState", arrayOf(String::class.java), id) as? Int) == Condition.STATE_TRUE
            }.map { (id, rule) -> Mode(id, rule.name?.toString().orEmpty(), rule.isEnabled, rule.type) }

    fun setActive(
        modeId: String,
        modeName: String,
        active: Boolean,
    ): Result {
        val rules = modeRules()
        val (id, rule) =
            rules.entries.firstOrNull { it.key == modeId }?.toPair()
                ?: rules.entries.singleOrNull { it.value.name?.toString() == modeName }?.toPair()
                ?: return Result.NOT_FOUND
        val conditionId = rule.conditionId ?: return Result.NO_CONDITION
        val condition =
            Condition(
                conditionId,
                rule.name?.toString().orEmpty(),
                if (active) Condition.STATE_TRUE else Condition.STATE_FALSE,
                Condition.SOURCE_USER_ACTION,
            )
        invoke("setAutomaticZenRuleState", arrayOf(String::class.java, Condition::class.java), id, condition)
        return Result.DONE
    }

    private fun modeRules(): Map<String, AutomaticZenRule> =
        parseZenRules<AutomaticZenRule>(invoke("getAutomaticZenRules", emptyArray()))
            .filterKeys { !it.startsWith(IMPLICIT_RULE_PREFIX) }

    private fun invoke(
        name: String,
        types: Array<Class<*>>,
        vararg args: Any?,
    ): Any? {
        SystemServiceHelper.getSystemService(MODES_SERVICE)?.let { binder ->
            try {
                return invoke(binder, MODES_MANAGER, name, types, *args)
            } catch (_: IllegalStateException) {
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
        val managerClass = Class.forName(managerName)
        val method =
            managerClass.methods.firstOrNull { it.name == name && it.parameterTypes.contentEquals(types) }
                ?: throw NoSuchMethodException(
                    "$managerName.$name(${types.joinToString { it.simpleName }}) not found, available: " +
                        managerClass.methods
                            .filter { it.name == name }
                            .joinToString { m -> "(${m.parameterTypes.joinToString { it.simpleName }})" },
                )
        return try {
            method.invoke(manager, *args)
        } catch (e: InvocationTargetException) {
            throw (e.cause as? Exception ?: e)
        }
    }
}
