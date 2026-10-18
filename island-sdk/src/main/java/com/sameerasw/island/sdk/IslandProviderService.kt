package com.sameerasw.island.sdk

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.view.MotionEvent
import android.view.SurfaceControlViewHost

abstract class IslandProviderService : Service() {
    protected var hostInfo: HostInfo? = null
        private set

    protected var host: IslandHostHandle? = null
        private set

    protected open fun onHostGrantRequested(hostPackage: String): Boolean = true
    protected open fun onHostAttached(host: IslandHostHandle, info: HostInfo) {}
    protected open fun onHostDetached() {}
    protected open fun onHostInfoChanged(info: HostInfo) {}
    protected open fun onAction(itemKey: String, actionId: String) {}
    protected open fun onRequestSurface(itemKey: String, request: SurfaceRequest, deliver: (SurfaceControlViewHost.SurfacePackage?) -> Unit) = deliver(null)
    protected open fun onResizeSurface(itemKey: String, widthPx: Int, heightPx: Int) {}
    protected open fun onReleaseSurface(itemKey: String) {}
    protected open fun onSurfaceTouch(itemKey: String, event: MotionEvent) = event.recycle()

    private val binder = object : IIslandProvider.Stub() {
        override fun getSchemaVersion(): Int = IslandSchema.VERSION

        override fun isHostAuthorized(): Boolean = isCallerAllowed()

        override fun grantHost(): Boolean {
            val hostPackage = callerHost() ?: return false
            if (!onHostGrantRequested(hostPackage)) return false
            val fingerprint = IslandIdentity.fingerprint(this@IslandProviderService, hostPackage) ?: return false
            HostGrantStore(this@IslandProviderService).grant(hostPackage, fingerprint)
            return true
        }

        override fun revokeHost() {
            val hostPackage = callerHost() ?: return
            HostGrantStore(this@IslandProviderService).revoke(hostPackage)
            host = null
            hostInfo = null
            onHostDetached()
        }

        override fun attach(host: IIslandHost, hostInfo: Bundle) {
            if (!isCallerAllowed()) return
            val info = HostInfo.fromBundle(hostInfo)
            val handle = IslandHostHandle(host)
            this@IslandProviderService.host = handle
            this@IslandProviderService.hostInfo = info
            onHostAttached(handle, info)
        }

        override fun detach() {
            if (!isCallerAllowed()) return
            host = null
            hostInfo = null
            onHostDetached()
        }

        override fun onHostInfoChanged(hostInfo: Bundle) {
            if (!isCallerAllowed()) return
            val info = HostInfo.fromBundle(hostInfo)
            this@IslandProviderService.hostInfo = info
            this@IslandProviderService.onHostInfoChanged(info)
        }

        override fun onAction(itemKey: String, actionId: String) {
            if (!isCallerAllowed()) return
            this@IslandProviderService.onAction(itemKey, actionId)
        }

        override fun requestSurface(itemKey: String, request: Bundle, callback: ISurfaceCallback) {
            if (!isCallerAllowed()) return
            val parsed = SurfaceRequest.fromBundle(request)
            if (parsed == null) {
                callback.onSurface(Bundle())
                return
            }
            onRequestSurface(itemKey, parsed) { surface ->
                callback.onSurface(Bundle().apply { putParcelable(SurfaceRequest.RESULT_PACKAGE, surface) })
            }
        }

        override fun resizeSurface(itemKey: String, widthPx: Int, heightPx: Int) {
            if (!isCallerAllowed()) return
            onResizeSurface(itemKey, widthPx, heightPx)
        }

        override fun releaseSurface(itemKey: String) {
            if (!isCallerAllowed()) return
            onReleaseSurface(itemKey)
        }

        override fun onSurfaceTouch(itemKey: String, event: MotionEvent) {
            if (!isCallerAllowed()) {
                event.recycle()
                return
            }
            this@IslandProviderService.onSurfaceTouch(itemKey, event)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean {
        host = null
        hostInfo = null
        onHostDetached()
        return false
    }

    private fun callerHost(): String? =
        packageManager.getPackagesForUid(Binder.getCallingUid())?.firstOrNull { it in IslandSchema.ALLOWED_HOSTS }

    private fun isCallerAllowed(): Boolean {
        val packages = packageManager.getPackagesForUid(Binder.getCallingUid()) ?: return false
        val grants = HostGrantStore(this)
        return packages.any { it in IslandSchema.ALLOWED_HOSTS && grants.isAuthorized(it) }
    }
}
