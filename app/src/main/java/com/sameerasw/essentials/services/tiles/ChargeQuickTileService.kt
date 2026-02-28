package com.sameerasw.essentials.services.tiles

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import androidx.annotation.RequiresApi
import com.sameerasw.essentials.FeatureSettingsActivity
import com.sameerasw.essentials.R
import com.sameerasw.essentials.utils.PermissionUtils

@RequiresApi(Build.VERSION_CODES.N)
class ChargeQuickTileService : BaseTileService() {

    companion object {
        private const val ADAPTIVE_CHARGING_SETTING = "adaptive_charging_enabled"
        private const val CHARGE_OPTIMIZATION_MODE = "charge_optimization_mode"
    }

    override fun onClick() {
        if (!hasFeaturePermission()) {
            com.sameerasw.essentials.utils.HapticUtil.performHapticForService(this)
            val intent = Intent(this, FeatureSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("feature", "Quick settings tiles")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }
        super.onClick()
    }

    override fun onTileClick() {
        val adaptiveChargingEnabled = Settings.Secure.getInt(contentResolver, ADAPTIVE_CHARGING_SETTING, 0) == 1
        val chargeOptimizationEnabled = Settings.Secure.getInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 0) == 1
        
        when {
            adaptiveChargingEnabled -> {
                Settings.Secure.putInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 1)
                Settings.Secure.putInt(contentResolver, ADAPTIVE_CHARGING_SETTING, 0)
            }

            chargeOptimizationEnabled -> {
                Settings.Secure.putInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 0)
                Settings.Secure.putInt(contentResolver, ADAPTIVE_CHARGING_SETTING, 0)
            }

            else -> {
                Settings.Secure.putInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 0)
                Settings.Secure.putInt(contentResolver, ADAPTIVE_CHARGING_SETTING, 1)
            }
        }
    }

    override fun getTileLabel(): String = getString(R.string.tile_charge_optimization)

    override fun getTileSubtitle(): String {
        val adaptiveChargingEnabled = Settings.Secure.getInt(contentResolver, ADAPTIVE_CHARGING_SETTING, 0) == 1
        val chargeOptimizationEnabled = Settings.Secure.getInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 0) == 1
        return when {
            chargeOptimizationEnabled -> getString(R.string.limit_to_80)
            adaptiveChargingEnabled -> getString(R.string.adaptive_charging)
            else -> getString(R.string.deactivated)
        }
    }

    override fun hasFeaturePermission(): Boolean {
        return PermissionUtils.canWriteSecureSettings(this)
    }

    override fun getTileIcon(): Icon {
        val adaptiveChargingEnabled = Settings.Secure.getInt(contentResolver, ADAPTIVE_CHARGING_SETTING, 0) == 1
        val chargeOptimizationEnabled = Settings.Secure.getInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 0) == 1
        val resId = when {
            chargeOptimizationEnabled -> R.drawable.rounded_battery_android_frame_shield_24
            adaptiveChargingEnabled -> R.drawable.rounded_battery_android_frame_plus_24
            else -> R.drawable.outline_battery_android_frame_bolt_24
        }
        return Icon.createWithResource(this, resId)
    }

    override fun getTileState(): Int {
        val adaptiveChargingEnabled = Settings.Secure.getInt(contentResolver, ADAPTIVE_CHARGING_SETTING, 0) == 1
        val chargeOptimizationEnabled = Settings.Secure.getInt(contentResolver, CHARGE_OPTIMIZATION_MODE, 0) == 1
        return if (chargeOptimizationEnabled || adaptiveChargingEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }
}
