package com.sameerasw.essentials.services.tiles

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.LauncherUtils
import com.sameerasw.essentials.utils.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultLauncherTileService : BaseTileService() {
    override fun getTileLabel(): String = getString(R.string.tile_default_launcher)

    override fun getTileSubtitle(): String =
        LauncherUtils.currentLauncher(this)?.let { LauncherUtils.label(this, it) } ?: getString(R.string.subtitle_default_launcher)

    override fun hasFeaturePermission(): Boolean = true

    override fun getTileIcon(): Icon = Icon.createWithResource(this, R.drawable.rounded_home_24)

    override fun getTileState(): Int = Tile.STATE_INACTIVE

    private fun canSwitchDirectly(): Boolean = ShellUtils.isAvailable(this) && ShellUtils.hasPermission(this)

    override fun onTileClick() {
        val launchers = LauncherUtils.cycleLaunchers(this)
        if (launchers.size > 1 && canSwitchDirectly()) {
            val current = LauncherUtils.currentLauncher(this)
            val next = launchers[(launchers.indexOf(current) + 1) % launchers.size]
            serviceScope.launch(Dispatchers.IO) {
                ShellUtils.runCommand(
                    this@DefaultLauncherTileService,
                    "cmd role add-role-holder --user 0 android.app.role.HOME $next",
                )
                delay(300)
                ShellUtils.runCommand(this@DefaultLauncherTileService, "input keyevent KEYCODE_HOME")
                withContext(Dispatchers.Main) { updateTile() }
            }
            return
        }
        openPicker()
    }

    private fun openPicker() {
        val intent =
            Intent(Settings.ACTION_HOME_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
