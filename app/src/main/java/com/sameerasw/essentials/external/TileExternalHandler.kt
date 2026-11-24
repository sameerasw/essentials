/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: External Control
 * File: TileExternalHandler.kt
 * Description: External control handler that triggers any Quick Settings tile by name.
 */

package com.sameerasw.essentials.external

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Bundle
import com.sameerasw.essentials.domain.registry.QSTileRegistry
import com.sameerasw.essentials.services.receivers.QsTileActionRouter

class TileExternalHandler : ExternalHandler {
    override val path: String = "tile"

    private fun normalize(name: String): String =
        name.substringAfterLast('.').removeSuffix("TileService").replace("_", "").lowercase()

    private fun resolve(
        context: Context,
        name: String,
    ): Class<*>? {
        val target = normalize(name)
        return QSTileRegistry.getAllTiles(context).map { it.serviceClass }.firstOrNull { normalize(it.name) == target }
    }

    override fun onQuery(
        context: Context,
        remainingPath: String,
        extras: Bundle?,
    ): Cursor? {
        val cursor = MatrixCursor(arrayOf("name"))
        QSTileRegistry.getAllTiles(context).forEach { cursor.addRow(arrayOf(normalize(it.serviceClass.name))) }
        return cursor
    }

    override fun onUpdate(
        context: Context,
        remainingPath: String,
        value: String?,
        extras: Bundle?,
    ): Boolean = false

    override fun onAction(
        context: Context,
        remainingPath: String,
        action: String?,
        extras: Bundle?,
    ): Bundle? {
        if (action != "toggle" && action != "trigger") return null
        val clazz = resolve(context, remainingPath) ?: return null
        val intent =
            Intent(QsTileActionRouter.ACTION_TRIGGER_TILE).apply {
                setPackage(context.packageName)
                putExtra(QsTileActionRouter.EXTRA_SERVICE_CLASS_NAME, clazz.name)
            }
        context.sendBroadcast(intent)
        return Bundle().apply { putBoolean("success", true) }
    }
}
