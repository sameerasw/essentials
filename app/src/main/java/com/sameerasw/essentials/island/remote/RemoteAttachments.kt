package com.sameerasw.essentials.island.remote

import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.island.model.IslandAttachment
import com.sameerasw.essentials.island.model.IslandBeat
import com.sameerasw.island.sdk.IslandAttachmentPayload
import com.sameerasw.island.sdk.IslandSurfaceKey

object RemoteAttachments {
    fun map(
        packageName: String,
        payload: IslandAttachmentPayload,
        handler: RemoteItemHandler
    ): IslandAttachment {
        val surfaceKey = IslandSurfaceKey.attachment(payload.key)
        return IslandAttachment(
            key = "$packageName:${payload.key}",
            anchor = payload.anchor,
            matchTitle = payload.matchTitle,
            beat = payload.beatMs?.let { IslandBeat(it, payload.beatOffsetMs ?: 0f) },
        ) { widthPx, settled ->
            val density = LocalDensity.current
            val reported = handler.reportedHeightPx(surfaceKey)
            val heightPx =
                if (reported > 0) reported else with(density) { payload.heightDp.dp.roundToPx() }
            val size = with(density) { Modifier
                .width(widthPx.toDp())
                .height(heightPx.toDp()) }
            val session =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) handler.surfaceSession(
                    surfaceKey
                ) else null
            if (session == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                Spacer(size)
            } else {
                val interactive by rememberUpdatedState(settled)
                RemoteSurfaceBox(
                    session = session,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    cornerPx = minOf(
                        with(density) { IslandAttachment.CORNER.toPx() },
                        heightPx / 2f
                    ),
                    showLive = settled && session.hasSurface,
                    snapshot = handler.snapshot(surfaceKey),
                    aboveHost = true,
                    modifier = size.pointerInput(session) {
                        detectTapGestures { if (interactive) session.tap(it.x, it.y) }
                    },
                )
            }
        }
    }
}
