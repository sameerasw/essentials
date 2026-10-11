package com.sameerasw.essentials.island.remote

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri

object OvercastLink {
    const val PACKAGE = "com.sameerasw.overcast"
    private const val INSTALL_URL = "https://github.com/sameerasw/Overcast/releases/latest"

    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun openApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openInstallPage(context: Context) {
        context.startActivity(Intent(Intent.ACTION_VIEW, INSTALL_URL.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
