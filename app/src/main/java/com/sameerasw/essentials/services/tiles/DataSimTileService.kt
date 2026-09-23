package com.sameerasw.essentials.services.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.util.Log
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.ShizukuUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class DataSimTileService : BaseTileService() {
    private var current: DataSimController.State? = null
    private var failed = false
    private var isSwitching = false
    private var revision = 0
    private val binderListener = Shizuku.OnBinderReceivedListener {
        serviceScope.launch { refresh() }
    }
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        serviceScope.launch {
            current = null
            updateTile()
        }
    }

    override val isSensitiveTile = true

    override fun onCreate() {
        super.onCreate()
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch { refresh() }
    }

    private suspend fun refresh() {
        if (isSwitching) return
        val startedAt = revision
        if (!hasFeaturePermission()) {
            current = null
            updateTile()
            return
        }
        val state = try {
            withContext(Dispatchers.IO) { DataSimController.read(this@DataSimTileService) }
        } catch (e: Exception) {
            Log.w("DataSimTile", "Cannot read active subscriptions", e)
            null
        }
        if (revision != startedAt) return
        current = state
        failed = state == null
        updateTile()
    }

    override fun onClick() {
        if (isSwitching) return
        if (!hasFeaturePermission()) {
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        super.onClick()
    }

    override fun getTileLabel() = getString(R.string.tile_data_sim)
    override fun getTileSubtitle(): String {
        if (isSwitching) return "Working..."
        if (failed) return getString(R.string.tile_data_sim_failed)
        val state = current ?: return getString(R.string.tile_data_sim_loading)
        val selected = state.selected ?: return getString(R.string.tile_data_sim_no_selection)
        return selected.name?.takeIf { it.isNotBlank() } ?: getString(R.string.tile_data_sim_slot, selected.slot + 1)
    }

    override fun hasFeaturePermission() = ShizukuUtils.hasPermission()
    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.outline_sim_card_24)
    override fun getTileState() =
        when {
            isSwitching -> Tile.STATE_UNAVAILABLE
            current?.next != null -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }

    override fun onTileClick() {
        if (isSwitching) return
        isSwitching = true
        revision++
        updateTile()
        serviceScope.launch {
            try {
                current = withContext(Dispatchers.IO) { DataSimController.advance(this@DataSimTileService) }
                failed = false
            } catch (e: Exception) {
                Log.e("DataSimTile", "Cannot switch the default data SIM", e)
                failed = true
                current = runCatching {
                    withContext(Dispatchers.IO) { DataSimController.read(this@DataSimTileService) }
                }.getOrNull()
            } finally {
                isSwitching = false
                updateTile()
            }
        }
    }
}
