package com.sameerasw.island.sdk

import android.content.Context

class HostGrantStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun grant(packageName: String, fingerprint: String) {
        prefs.edit().putString(packageName, fingerprint).apply()
    }

    fun revoke(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }

    fun granted(): Map<String, String> =
        prefs.all.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()

    fun isAuthorized(packageName: String): Boolean {
        val pinned = prefs.getString(packageName, null) ?: return false
        return IslandIdentity.fingerprint(appContext, packageName) == pinned
    }

    private companion object {
        const val PREFS = "island_host_grants"
    }
}
