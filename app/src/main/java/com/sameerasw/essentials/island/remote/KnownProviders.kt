package com.sameerasw.essentials.island.remote

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.sameerasw.essentials.R

data class KnownProvider(
    val packageName: String,
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int,
    val installUrl: String,
) {
    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun open(context: Context) {
        val intent =
            if (isInstalled(context)) {
                context.packageManager.getLaunchIntentForPackage(packageName)
            } else {
                Intent(Intent.ACTION_VIEW, installUrl.toUri())
            } ?: return
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

object KnownProviders {
    val all: List<KnownProvider> =
        listOf(
            KnownProvider(
                packageName = OvercastLink.PACKAGE,
                nameRes = R.string.app_overcast,
                iconRes = R.drawable.rounded_partly_cloudy_day_24,
                installUrl = "https://play.google.com/store/apps/details?id=${OvercastLink.PACKAGE}",
            ),
            KnownProvider(
                packageName = AirSyncLink.PACKAGE,
                nameRes = R.string.app_airsync,
                iconRes = R.drawable.rounded_laptop_mac_24,
                installUrl = "https://play.google.com/store/apps/details?id=${AirSyncLink.PACKAGE}",
            ),
        )
}
