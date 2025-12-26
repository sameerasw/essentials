package com.sameerasw.essentials.services

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
abstract class BaseTileService : TileService() {

    abstract fun onTileClick()

    abstract fun getTileLabel(): String

    abstract fun getTileSubtitle(): String

    abstract fun hasFeaturePermission(): Boolean

    open fun getTileIcon(): Icon? = null

    override fun onStartListening() {
        super.onStartListening()
        setTileAddedState(true)
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        setTileAddedState(true)
        updateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        setTileAddedState(false)
    }

    private fun setTileAddedState(isAdded: Boolean) {
        getSharedPreferences("essentials_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("${this::class.java.name}_is_added", isAdded)
            .apply()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            qsTile.subtitle = if (!hasPerm) "Missing permissions" else getTileSubtitle()
        }
        val icon = getTileIcon()
        if (icon != null) {
            qsTile.icon = icon
        }
        qsTile.updateTile()
    }

    protected abstract fun getTileState(): Int
}




