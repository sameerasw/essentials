package com.sameerasw.essentials

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

abstract class BaseTileService : TileService() {

    abstract fun onTileClick()

    abstract fun getTileLabel(): String

    abstract fun getTileSubtitle(): String

    abstract fun hasFeaturePermission(): Boolean

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!hasFeaturePermission()) {
            return
        }
        onTileClick()
        updateTile()
    }

    protected fun updateTile() {
        val hasPerm = hasFeaturePermission()
        qsTile.state = if (hasPerm) {
            getTileState()
        } else {
            Tile.STATE_UNAVAILABLE
        }
        qsTile.label = getTileLabel()
        qsTile.subtitle = if (!hasPerm) "Missing permissions" else getTileSubtitle()
        qsTile.updateTile()
    }

    protected abstract fun getTileState(): Int
}
