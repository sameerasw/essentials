package com.sameerasw.essentials.island.remote

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.sameerasw.island.sdk.IslandSchema

class ProviderInfo(
    val packageName: String,
    val serviceName: String,
    val label: String,
    val priority: Int?,
    val settingsActivity: String?,
    val description: String?,
) {
    val component: ComponentName get() = ComponentName(packageName, serviceName)
}

object ProviderDiscovery {
    fun query(context: Context): List<ProviderInfo> {
        val pm = context.packageManager
        val intent = Intent(IslandSchema.SERVICE_ACTION)
        return pm.queryIntentServices(intent, PackageManager.GET_META_DATA).mapNotNull { resolved ->
            val service = resolved.serviceInfo ?: return@mapNotNull null
            if (!service.exported) return@mapNotNull null
            val meta = service.metaData
            ProviderInfo(
                packageName = service.packageName,
                serviceName = service.name,
                label = meta?.getString(IslandSchema.META_LABEL) ?: resolved.loadLabel(pm).toString(),
                priority = meta?.takeIf { it.containsKey(IslandSchema.META_PRIORITY) }?.getInt(IslandSchema.META_PRIORITY),
                settingsActivity = meta?.getString(IslandSchema.META_SETTINGS_ACTIVITY),
                description = meta?.getString(IslandSchema.META_DESCRIPTION),
            )
        }
    }

    fun find(context: Context, packageName: String): ProviderInfo? = query(context).firstOrNull { it.packageName == packageName }

    fun openSettings(context: Context, info: ProviderInfo) {
        val activity = info.settingsActivity
        val intent = if (activity != null) {
            Intent().setComponent(ComponentName(info.packageName, activity))
        } else {
            context.packageManager.getLaunchIntentForPackage(info.packageName)
        } ?: return
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}
