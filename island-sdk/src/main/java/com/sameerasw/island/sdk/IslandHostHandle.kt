package com.sameerasw.island.sdk

import android.graphics.Bitmap
import android.os.Bundle

class IslandHostHandle internal constructor(private val host: IIslandHost) {
    fun publish(payload: IslandPayload, assets: Map<String, Bitmap> = emptyMap()) {
        val (schema, json, bundle) = IslandCodec.encode(payload, assets)
        host.publish(schema, json, bundle)
    }

    fun snapshot(itemKey: String, bitmap: Bitmap) = host.snapshot(itemKey, bitmap)

    fun peek(itemKey: String, durationMs: Long = 0L, sticky: Boolean = false) = send(
        itemKey, HostRequestType.PEEK,
        Bundle().apply {
            putLong(HostRequestType.ARG_DURATION_MS, durationMs)
            putBoolean(HostRequestType.ARG_STICKY, sticky)
        },
    )

    fun expand(itemKey: String, sticky: Boolean = false) = send(
        itemKey, HostRequestType.EXPAND,
        Bundle().apply { putBoolean(HostRequestType.ARG_STICKY, sticky) },
    )

    fun collapse(itemKey: String) = send(itemKey, HostRequestType.COLLAPSE)
    fun dismiss(itemKey: String) = send(itemKey, HostRequestType.DISMISS)
    fun openApp(itemKey: String) = send(itemKey, HostRequestType.OPEN_APP)
    fun keepAlive(itemKey: String) = send(itemKey, HostRequestType.KEEP_ALIVE)

    fun resize(itemKey: String, heightPx: Int) = send(
        itemKey, HostRequestType.RESIZE,
        Bundle().apply { putInt(HostRequestType.ARG_HEIGHT_PX, heightPx) },
    )

    fun setTextInput(itemKey: String, active: Boolean) = send(
        itemKey, HostRequestType.TEXT_INPUT,
        Bundle().apply { putBoolean(HostRequestType.ARG_ACTIVE, active) },
    )

    private fun send(itemKey: String, type: String, args: Bundle = Bundle()) {
        host.request(itemKey, type, args)
    }
}
