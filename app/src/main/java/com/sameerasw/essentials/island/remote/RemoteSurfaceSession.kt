package com.sameerasw.essentials.island.remote

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import com.sameerasw.island.sdk.IIslandProvider
import com.sameerasw.island.sdk.ISurfaceCallback
import com.sameerasw.island.sdk.SurfaceRequest

@RequiresApi(Build.VERSION_CODES.R)
class RemoteSurfaceSession(
    private val provider: () -> IIslandProvider?,
    private val itemKey: String,
) {
    private val main = Handler(Looper.getMainLooper())
    private var released = false

    fun request(view: SurfaceView, widthPx: Int, heightPx: Int) {
        val service = provider() ?: return
        released = false
        val request = SurfaceRequest(
            hostToken = view.hostToken ?: return,
            displayId = view.display?.displayId ?: 0,
            widthPx = widthPx,
            heightPx = heightPx,
        )
        val callback = object : ISurfaceCallback.Stub() {
            override fun onSurface(result: Bundle) {
                result.classLoader = SurfaceControlViewHost.SurfacePackage::class.java.classLoader
                @Suppress("DEPRECATION")
                val surface = result.getParcelable<SurfaceControlViewHost.SurfacePackage>(SurfaceRequest.RESULT_PACKAGE)
                main.post {
                    if (released) surface?.release() else surface?.let(view::setChildSurfacePackage)
                }
            }
        }
        try {
            service.requestSurface(itemKey, request.toBundle(), callback)
        } catch (_: Exception) {
        }
    }

    fun resize(widthPx: Int, heightPx: Int) {
        try {
            provider()?.resizeSurface(itemKey, widthPx, heightPx)
        } catch (_: Exception) {
        }
    }

    fun release() {
        released = true
        try {
            provider()?.releaseSurface(itemKey)
        } catch (_: Exception) {
        }
    }
}
