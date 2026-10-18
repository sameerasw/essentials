package com.sameerasw.island.sdk.compose

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.SurfaceControlViewHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.sameerasw.island.sdk.HostInfo
import com.sameerasw.island.sdk.IslandHostHandle
import com.sameerasw.island.sdk.IslandProviderService
import com.sameerasw.island.sdk.SurfaceRequest

abstract class ComposeIslandProviderService : IslandProviderService() {
    private val main = Handler(Looper.getMainLooper())
    private val surfaces = HashMap<String, IslandSurface>()

    protected val hostInfoState = mutableStateOf<HostInfo?>(null)

    @Composable
    protected abstract fun ExpandedContent(itemKey: String)

    protected open fun onAttached(host: IslandHostHandle, info: HostInfo) {}
    protected open fun onInfoChanged(info: HostInfo) {}

    final override fun onHostAttached(host: IslandHostHandle, info: HostInfo) {
        hostInfoState.value = info
        onAttached(host, info)
    }

    final override fun onHostInfoChanged(info: HostInfo) {
        hostInfoState.value = info
        onInfoChanged(info)
    }

    protected open fun hasExpandedContent(itemKey: String): Boolean = true

    override fun onRequestSurface(
        itemKey: String,
        request: SurfaceRequest,
        deliver: (SurfaceControlViewHost.SurfacePackage?) -> Unit,
    ) {
        main.post {
            surfaces.remove(itemKey)?.release()
            if (!hasExpandedContent(itemKey)) {
                deliver(null)
                return@post
            }
            val surface = IslandSurface(this, request) { ExpandedContent(itemKey) }
            surfaces[itemKey] = surface
            deliver(surface.surfacePackage)
        }
    }

    override fun onResizeSurface(itemKey: String, widthPx: Int, heightPx: Int) {
        main.post { surfaces[itemKey]?.resize(widthPx, heightPx) }
    }

    override fun onReleaseSurface(itemKey: String) {
        main.post { surfaces.remove(itemKey)?.release() }
    }

    override fun onSurfaceTouch(itemKey: String, event: MotionEvent) {
        main.post {
            surfaces[itemKey]?.dispatchTouch(event)
            event.recycle()
        }
    }

    override fun onHostDetached() {
        main.post {
            surfaces.values.forEach { it.release() }
            surfaces.clear()
        }
    }

    override fun onDestroy() {
        surfaces.values.forEach { it.release() }
        surfaces.clear()
        super.onDestroy()
    }
}
