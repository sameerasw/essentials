package com.sameerasw.essentials.external

import android.content.Context
import android.os.Bundle

object ExternalRouter {
    private val handlers = mutableMapOf<String, ExternalHandler>()

    init {
        registerHandler(SettingsExternalHandler())
    }

    fun registerHandler(handler: ExternalHandler) {
        handlers[handler.path] = handler
    }

    private fun getHandlerAndRemainingPath(fullPath: String): Pair<ExternalHandler, String>? {
        val cleanPath = fullPath.trim('/')
        for ((registeredPath, handler) in handlers) {
            if (cleanPath == registeredPath) {
                return Pair(handler, "")
            } else if (cleanPath.startsWith("$registeredPath/")) {
                val remaining = cleanPath.substring(registeredPath.length + 1)
                return Pair(handler, remaining)
            }
        }
        return null
    }

    fun query(context: Context, path: String, extras: Bundle?): Bundle? {
        val (handler, remaining) = getHandlerAndRemainingPath(path) ?: return null
        return handler.onQuery(context, remaining, extras)
    }

    fun update(context: Context, path: String, value: String?, extras: Bundle?): Boolean {
        val (handler, remaining) = getHandlerAndRemainingPath(path) ?: return false
        return handler.onUpdate(context, remaining, value, extras)
    }

    fun action(context: Context, path: String, action: String?, extras: Bundle?): Bundle? {
        val (handler, remaining) = getHandlerAndRemainingPath(path) ?: return null
        return handler.onAction(context, remaining, action, extras)
    }
}
