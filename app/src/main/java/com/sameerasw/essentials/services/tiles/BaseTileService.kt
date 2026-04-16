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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Handler
import android.os.Looper

@RequiresApi(Build.VERSION_CODES.N)
abstract class BaseTileService : TileService() {

    private val serviceJob = Job()
    protected val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val secureSettingsCache = mutableMapOf<String, Int>()

    protected var isProcessing = false

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

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        setTileAddedState(true)
        updateTile()
    }

    override fun onTileRemoved() {
        super.onTileRemoved()
        setTileAddedState(false)
        secureSettingsCache.clear()
    }

    private fun setTileAddedState(isAdded: Boolean) {
        getSharedPreferences("essentials_prefs", MODE_PRIVATE)
            .edit {
                putBoolean("${this::class.java.name}_is_added", isAdded)
            }
    }

    override fun onClick() {
        super.onClick()
        
        // Immediate feedback 1: Haptics
        HapticUtil.performHapticForService(this)
        
        if (!hasFeaturePermission() || isProcessing) {
            return
        }

        // Immediate feedback 2: Processing state
        isProcessing = true
        updateTile()

        // Offload actual work to background
        serviceScope.launch {
            try {
                onTileClick()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    isProcessing = false
                    updateTile()
                }
            }
        }
    }

    protected fun updateTile() {
        val tile = qsTile ?: return
        val hasPerm = hasFeaturePermission()
        tile.state = when {
            !hasPerm || isProcessing -> Tile.STATE_UNAVAILABLE
            else -> getTileState()
        }
        tile.label = getTileLabel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when {
                !hasPerm -> getString(R.string.permission_missing)
                isProcessing -> "Working..."
                else -> getTileSubtitle()
            }
        }

        val icon = getTileIcon()
        if (icon != null) {
            tile.icon = icon
        }
        tile.updateTile()
    }

    protected abstract fun getTileState(): Int

    protected fun getSecureInt(key: String, def: Int): Int {
        secureSettingsCache[key]?.let { return it }

        try {
            val value = Settings.Secure.getInt(contentResolver, key, -1)
            if (value != -1) {
                secureSettingsCache[key] = value
                return value
            }
        } catch (_: SecurityException) {
            // Only fallback to shell on SecurityException
            return try {
                val output = ShellUtils.runCommandWithOutput(this, "settings get secure $key")
                val result = output?.toIntOrNull() ?: def
                secureSettingsCache[key] = result
                result
            } catch (_: Exception) {
                def
            }
        } catch (_: Exception) {
            return def
        }
        return def
    }

    protected fun putSecureInt(key: String, value: Int) {
        secureSettingsCache[key] = value // Update cache immediately
        try {
            Settings.Secure.putInt(contentResolver, key, value)
        } catch (_: Exception) {
            // Fallback to shell if standard API fails
            ShellUtils.runCommand(this, "settings put secure $key $value")
        }
    }
}




