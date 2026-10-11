package com.sameerasw.essentials.island.remote

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.sameerasw.island.sdk.IIslandProvider
import com.sameerasw.island.sdk.IslandSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object ProviderPairing {
    private const val TIMEOUT_MS = 5_000L

    suspend fun grant(context: Context, info: ProviderInfo): Boolean = call(context, info) { it.grantHost() }

    suspend fun revoke(context: Context, info: ProviderInfo) {
        call(context, info) {
            it.revokeHost()
            true
        }
    }

    private suspend fun call(context: Context, info: ProviderInfo, block: (IIslandProvider) -> Boolean): Boolean {
        val app = context.applicationContext
        var connection: ServiceConnection? = null
        return try {
            withTimeoutOrNull(TIMEOUT_MS) {
                val provider = suspendCancellableCoroutine<IIslandProvider?> { cont ->
                    val callback = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, service: IBinder) {
                            if (cont.isActive) cont.resume(IIslandProvider.Stub.asInterface(service))
                        }

                        override fun onServiceDisconnected(name: ComponentName) {}

                        override fun onNullBinding(name: ComponentName) {
                            if (cont.isActive) cont.resume(null)
                        }
                    }
                    connection = callback
                    val intent = Intent(IslandSchema.SERVICE_ACTION).setComponent(info.component)
                    val bound = try {
                        app.bindService(intent, callback, Context.BIND_AUTO_CREATE)
                    } catch (_: SecurityException) {
                        false
                    }
                    if (!bound && cont.isActive) cont.resume(null)
                }
                provider?.let {
                    withContext(Dispatchers.IO) {
                        try {
                            block(it)
                        } catch (_: Exception) {
                            false
                        }
                    }
                } ?: false
            } ?: false
        } finally {
            connection?.let {
                try {
                    app.unbindService(it)
                } catch (_: IllegalArgumentException) {
                }
            }
        }
    }
}
