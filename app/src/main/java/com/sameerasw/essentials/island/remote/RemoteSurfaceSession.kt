package com.sameerasw.essentials.island.remote

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceControlViewHost
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sameerasw.island.sdk.IIslandProvider
import com.sameerasw.island.sdk.ISurfaceCallback
import com.sameerasw.island.sdk.SurfaceRequest

@RequiresApi(Build.VERSION_CODES.R)
class RemoteSurfaceSession(
    private val provider: () -> IIslandProvider?,
    private val itemKey: String,
    private val onUnhandledTap: () -> Unit = {},
) {
    private val main = Handler(Looper.getMainLooper())
    private var released = false
    private var target: SurfaceView? = null
    private var targetWidthPx = 0
    private var targetHeightPx = 0
    private var attempts = 0
    private val retryRunnable = Runnable { attempt() }

    var hasSurface by mutableStateOf(false)
        private set

    @Volatile
    private var handledAt = 0L

    fun markHandled() {
        handledAt = SystemClock.uptimeMillis()
    }

    fun request(view: SurfaceView, widthPx: Int, heightPx: Int) {
        released = false
        target = view
        targetWidthPx = widthPx
        targetHeightPx = heightPx
        attempts = 0
        attempt()
    }

    fun onProviderReady() {
        if (released || target == null || hasSurface) return
        attempts = 0
        attempt()
    }

    fun onProviderLost() {
        main.removeCallbacks(retryRunnable)
        hasSurface = false
    }

    private fun attempt() {
        val view = target ?: return
        if (released) return
        val service = provider()
        val hostToken = view.hostToken
        if (service == null || hostToken == null) {
            scheduleRetry(NOT_READY_RETRY_MS)
            return
        }
        attempts++
        val request = SurfaceRequest(
            hostToken = hostToken,
            displayId = view.display?.displayId ?: 0,
            widthPx = targetWidthPx,
            heightPx = targetHeightPx,
        )
        val callback = object : ISurfaceCallback.Stub() {
            override fun onSurface(result: Bundle) {
                result.classLoader = SurfaceControlViewHost.SurfacePackage::class.java.classLoader
                @Suppress("DEPRECATION")
                val surface = result.getParcelable<SurfaceControlViewHost.SurfacePackage>(SurfaceRequest.RESULT_PACKAGE)
                main.post {
                    if (released || surface == null) {
                        surface?.release()
                        return@post
                    }
                    main.removeCallbacks(retryRunnable)
                    view.setChildSurfacePackage(surface)
                    hasSurface = true
                }
            }
        }
        try {
            service.requestSurface(itemKey, request.toBundle(), callback)
        } catch (_: Exception) {
        }
        scheduleRetry(NO_SURFACE_RETRY_MS)
    }

    private fun scheduleRetry(delayMs: Long) {
        main.removeCallbacks(retryRunnable)
        if (released || hasSurface || attempts >= MAX_ATTEMPTS) return
        main.postDelayed(retryRunnable, delayMs)
    }

    fun resize(widthPx: Int, heightPx: Int) {
        try {
            provider()?.resizeSurface(itemKey, widthPx, heightPx)
        } catch (_: Exception) {
        }
    }

    fun tap(x: Float, y: Float) {
        if (!hasSurface) {
            onUnhandledTap()
            return
        }
        val downTime = SystemClock.uptimeMillis()
        send(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0))
        main.postDelayed({ if (handledAt < downTime) onUnhandledTap() }, UNHANDLED_TAP_MS)
        main.postDelayed(
            { send(MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)) },
            TAP_HOLD_MS,
        )
    }

    private fun send(event: MotionEvent) {
        try {
            provider()?.onSurfaceTouch(itemKey, event)
        } catch (_: Exception) {
        } finally {
            event.recycle()
        }
    }

    fun release() {
        released = true
        target = null
        hasSurface = false
        main.removeCallbacks(retryRunnable)
        try {
            provider()?.releaseSurface(itemKey)
        } catch (_: Exception) {
        }
    }

    private companion object {
        const val TAP_HOLD_MS = 50L
        const val UNHANDLED_TAP_MS = 350L
        const val NOT_READY_RETRY_MS = 250L
        const val NO_SURFACE_RETRY_MS = 1200L
        const val MAX_ATTEMPTS = 8
    }
}
