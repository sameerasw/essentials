package com.sameerasw.essentials.external

import android.content.Context
import android.os.Bundle

interface ExternalHandler {
    val path: String

    fun onQuery(context: Context, remainingPath: String, extras: Bundle?): Bundle?
    fun onUpdate(context: Context, remainingPath: String, value: String?, extras: Bundle?): Boolean
    fun onAction(context: Context, remainingPath: String, action: String?, extras: Bundle?): Bundle?
}
