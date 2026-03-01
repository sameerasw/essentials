package com.sameerasw.essentials.services.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.HapticUtil
import com.sameerasw.essentials.utils.ShellUtils
import com.sameerasw.essentials.utils.PermissionUtils

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
            tile.subtitle = if (!hasPerm) getString(R.string.permission_missing) else getTileSubtitle()
        }

        val icon = getTileIcon()
        if (icon != null) {
            tile.icon = icon
        }
        tile.updateTile()
    }

    protected abstract fun getTileState(): Int

    protected fun getSecureInt(key: String, def: Int): Int {
        try {
            val value = Settings.Secure.getInt(contentResolver, key, -1)
            if (value != -1) return value
        } catch (_: SecurityException) {
            // Only fallback to shell on SecurityException
            return try {
                val output = ShellUtils.runCommandWithOutput(this, "settings get secure $key")
                output?.toIntOrNull() ?: def
            } catch (_: Exception) {
                def
            }
        } catch (_: Exception) {
            return def
        }
        return def
    }

    protected fun putSecureInt(key: String, value: Int) {
        try {
            Settings.Secure.putInt(contentResolver, key, value)
        } catch (_: Exception) {
            // Fallback to shell if standard API fails
            ShellUtils.runCommand(this, "settings put secure $key $value")
        }
    }
}




