/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: External Control
 * File: CaffeinateExternalHandler.kt
 * Description: External control handler for Caffeinate.
 */

package com.sameerasw.essentials.external

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import com.sameerasw.essentials.domain.controller.CaffeinateController

class CaffeinateExternalHandler : ExternalHandler {
    override val path: String = "caffeinate"

    override fun onQuery(
        context: Context,
        remainingPath: String,
        extras: Bundle?,
    ): Cursor? {
        CaffeinateController.check(context)
        val cursor = MatrixCursor(arrayOf("key", "value", "type"))
        cursor.addRow(arrayOf<Any?>("active", CaffeinateController.isActive.value, "Boolean"))
        return cursor
    }

    override fun onUpdate(
        context: Context,
        remainingPath: String,
        value: String?,
        extras: Bundle?,
    ): Boolean {
        val enable = value?.toBooleanStrictOrNull() ?: return false
        apply(context, if (enable) "on" else "off")
        return true
    }

    override fun onAction(
        context: Context,
        remainingPath: String,
        action: String?,
        extras: Bundle?,
    ): Bundle? {
        val result = apply(context, action ?: remainingPath) ?: return null
        return Bundle().apply { putBoolean("value", result) }
    }

    private fun apply(
        context: Context,
        action: String,
    ): Boolean? {
        CaffeinateController.check(context)
        val running = CaffeinateController.isActive.value || CaffeinateController.isStarting.value
        when (action) {
            "toggle" -> CaffeinateController.toggle(context)
            "on", "start" -> if (!running) CaffeinateController.start(context)
            "off", "stop" -> if (running) CaffeinateController.cancelAll(context)
            else -> return null
        }
        return CaffeinateController.isActive.value || CaffeinateController.isStarting.value
    }
}
