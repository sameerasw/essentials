package com.sameerasw.essentials.island.remote

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin

class RemoteProvidersPlugin : BaseIslandPlugin() {
    override val id = "remote_providers"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_PROVIDERS,
        SettingsRepository.KEY_ISLAND_PROVIDERS_HIDDEN,
        SettingsRepository.KEY_ISLAND_PROVIDERS_HIDE_LOCKED,
    )

    private val connections = HashMap<String, ProviderConnection>()
    private var receiver: BroadcastReceiver? = null

    override fun onStart() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = refresh()
        }
        this.receiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    override fun onStop() {
        receiver?.let { context.unregisterReceiver(it) }
        receiver = null
        connections.values.forEach { it.disconnect() }
        connections.clear()
    }

    override fun refresh() {
        val c = ctx ?: return
        val trust = ProviderTrustStore(settings)
        val wanted = ProviderDiscovery.query(context)
            .filter { trust.isEnabled(it.packageName) && trust.isVerified(context, it.packageName) }
            .associateBy { it.packageName }

        connections.keys.filter { it !in wanted }.forEach { connections.remove(it)?.disconnect() }
        wanted.forEach { (pkg, info) ->
            if (pkg in connections) return@forEach
            val connection = ProviderConnection(
                context = context,
                settings = settings,
                info = info,
                scope = c.scope,
                request = c.request,
                onItemsChanged = ::publishAll,
                onUnauthorized = { onProviderUnauthorized(pkg) },
            )
            connections[pkg] = connection
            connection.connect()
        }
        publishAll()
    }

    override fun onScreenStateChanged() {
        connections.values.forEach { it.refreshHostInfo() }
        publishAll()
    }

    private fun isDeviceLocked(): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    private fun onProviderUnauthorized(packageName: String) {
        ProviderTrustStore(settings).disable(packageName)
        connections.remove(packageName)?.disconnect()
        publishAll()
    }

    private fun publishAll() {
        val trust = ProviderTrustStore(settings)
        val hidden = trust.hiddenFromIsland() + if (isDeviceLocked()) trust.hideWhenLocked() else emptySet()
        publish(connections.filterKeys { it !in hidden }.values.flatMap { it.items.value })
    }
}
