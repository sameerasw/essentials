package com.sameerasw.essentials.services.tiles

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import com.sameerasw.essentials.AppFreezingActivity

class AppFreezingTileService : BaseTileService() {

    override fun onTileClick() {
        val intent = Intent(this, AppFreezingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    override fun getTileLabel(): String = "App Freezing"

    override fun getTileSubtitle(): String = "Launch grid"

    override fun hasFeaturePermission(): Boolean = true

    override fun getTileState(): Int = Tile.STATE_INACTIVE
}
