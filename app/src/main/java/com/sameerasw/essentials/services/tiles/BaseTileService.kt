package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.utils.HapticUtil
import androidx.core.content.edit

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
        getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .edit {
                putBoolean("${this::class.java.name}_is_added", isAdded)
            }
    }

    override fun onClick() {
        super.onClick()
        HapticUtil.performHapticForService(this)
        if (!hasFeaturePermission()) {
            return
        }
        onTileClick()
        updateTile()
    }

    protected fun updateTile() {
        val tile = qsTile ?: return
        val hasPerm = hasFeaturePermission()
        tile.state = if (hasPerm) {
            getTileState()
        } else {
            Tile.STATE_UNAVAILABLE
        }
        tile.label = getTileLabel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (!hasPerm) "Missing permissions" else getTileSubtitle()
        }
        val icon = getTileIcon()
        if (icon != null) {
            tile.icon = icon
        }
        tile.updateTile()
    }

    protected abstract fun getTileState(): Int
}




