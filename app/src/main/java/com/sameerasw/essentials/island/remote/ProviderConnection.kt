package com.sameerasw.essentials.island.remote

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.IslandExpandedScope
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriorityEntries
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.ui.IslandLayoutSpec
import com.sameerasw.essentials.utils.DeviceUtils
import kotlin.math.roundToInt
import com.sameerasw.essentials.island.plugins.brief.BriefPlugin
import com.sameerasw.island.sdk.HostInfo
import com.sameerasw.island.sdk.HostRequestType
import com.sameerasw.island.sdk.IIslandHost
import com.sameerasw.island.sdk.IIslandProvider
import com.sameerasw.island.sdk.IslandAction
import com.sameerasw.island.sdk.IslandActionType
import com.sameerasw.island.sdk.IslandCodec
import com.sameerasw.island.sdk.IslandSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.sameerasw.island.sdk.DecodedPayload

class ProviderConnection(
    private val context: Context,
    private val settings: SettingsRepository,
    private val info: ProviderInfo,
    private val scope: CoroutineScope,
    private val request: (PluginRequest) -> Unit,
    private val onItemsChanged: () -> Unit,
    private val onUnauthorized: () -> Unit,
) : RemoteItemHandler {
    private val _items = MutableStateFlow<List<IslandItem>>(emptyList())
    val items: StateFlow<List<IslandItem>> = _items

    private val pending = MutableStateFlow<DecodedPayload?>(null)
    private val scopes = HashMap<String, IslandExpandedScope>()
    private val sessions = HashMap<String, RemoteSurfaceSession>()
    private var provider: IIslandProvider? = null
    private var layout: LayoutSnapshot? = null
    private var lastPopupAt = -MIN_EXPAND_GAP_MS
    private val heights = mutableStateMapOf<String, Int>()
    private val snapshots = mutableStateMapOf<String, ImageBitmap>()
    private var bound = false
    private var mapper: Job? = null
    private var retry: Job? = null
    private var retryDelayMs = INITIAL_RETRY_MS

    val packageName: String get() = info.packageName

    private val host = object : IIslandHost.Stub() {
        override fun publish(schema: Int, json: String, assets: Bundle?) {
            val decoded = IslandCodec.decode(schema, json, assets) ?: return
            pending.value = PayloadValidator.sanitize(decoded)
        }

        override fun snapshot(itemKey: String, bitmap: Bitmap) {
            snapshots[itemKey] = bitmap.asImageBitmap()
        }

        override fun request(itemKey: String, type: String, args: Bundle) {
            scope.launch { handleRequest(itemKey, type, args) }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val stub = IIslandProvider.Stub.asInterface(service)
            scope.launch {
                val status = withContext(Dispatchers.IO) {
                    try {
                        when {
                            stub.schemaVersion > IslandSchema.VERSION -> Status.INCOMPATIBLE
                            !stub.isHostAuthorized -> Status.UNAUTHORIZED
                            else -> Status.READY
                        }
                    } catch (_: Exception) {
                        Status.INCOMPATIBLE
                    }
                }
                if (status == Status.UNAUTHORIZED) {
                    retry?.cancel()
                    unbind()
                    clear()
                    onUnauthorized()
                    return@launch
                }
                if (status != Status.READY) return@launch
                provider = stub
                retryDelayMs = INITIAL_RETRY_MS
                try {
                    stub.attach(host, hostInfo().toBundle())
                } catch (_: Exception) {
                }
                sessions.values.forEach { it.onProviderReady() }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            clear()
        }

        override fun onBindingDied(name: ComponentName) {
            clear()
            unbind()
            scheduleRetry()
        }

        override fun onNullBinding(name: ComponentName) {
            clear()
            unbind()
        }
    }

    fun connect() {
        if (bound) return
        mapper = scope.launch {
            pending.filterNotNull().collect { decoded ->
                apply(decoded)
                delay(MIN_UPDATE_GAP_MS)
            }
        }
        bind()
    }

    fun disconnect() {
        retry?.cancel()
        mapper?.cancel()
        try {
            provider?.detach()
        } catch (_: Exception) {
        }
        unbind()
        clear()
        pending.value = null
    }

    fun refreshHostInfo() {
        try {
            provider?.onHostInfoChanged(hostInfo().toBundle())
        } catch (_: Exception) {
        }
    }

    private fun bind() {
        val intent = Intent(IslandSchema.SERVICE_ACTION).setComponent(info.component)
        bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (_: SecurityException) {
            false
        }
        if (!bound) scheduleRetry()
    }

    private fun unbind() {
        if (!bound) return
        bound = false
        try {
            context.unbindService(connection)
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun scheduleRetry() {
        retry?.cancel()
        retry = scope.launch {
            delay(retryDelayMs)
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
            if (!bound) bind()
        }
    }

    private fun clear() {
        provider = null
        sessions.values.forEach { it.onProviderLost() }
        scopes.clear()
        heights.clear()
        snapshots.clear()
        pending.value = null
        _items.value = emptyList()
        RemoteBriefCards.set(info.packageName, emptyList())
        onItemsChanged()
    }

    private fun apply(decoded: DecodedPayload) {
        val siblings = decoded.payload.items.map { it.key }.toSet()
        _items.value = decoded.payload.items.mapNotNull { RemoteItemMapper.map(it, decoded.assets, siblings, this) }
        RemoteBriefCards.set(info.packageName, decoded.payload.briefCards.map { RemoteBriefCard(info.packageName, it, decoded.assets, this) })
        onItemsChanged()
    }

    private fun hostInfo(): HostInfo {
        val metrics = context.resources.displayMetrics
        val snapshot = layout
        return HostInfo(
            hostPackage = context.packageName,
            accent = (snapshot?.accent ?: Color.White).toArgb(),
            density = snapshot?.density ?: metrics.density,
            fontScale = context.resources.configuration.fontScale,
            expandedWidthPx = snapshot?.expandedWidthPx ?: 0,
            expandedPaddingPx = snapshot?.paddingPx ?: 0,
            cameraClearancePx = snapshot?.cameraClearancePx ?: 0,
            outsetPx = snapshot?.outsetPx ?: 0,
            compactHeightPx = snapshot?.compactHeightPx ?: 0,
            cameraSlotWidthPx = snapshot?.cameraSlotWidthPx ?: 0,
            cornerPx = snapshot?.cornerPx ?: 0,
            powerSave = DeviceUtils.isPowerSaveMode(context),
        )
    }

    private fun handleRequest(rawKey: String, type: String, args: Bundle) {
        sessions[rawKey]?.markHandled()
        val key = namespaced(rawKey)
        when (type) {
            HostRequestType.PEEK -> {
                if (!allowPopup(MIN_PEEK_GAP_MS)) return
                val duration = (args.getLong(HostRequestType.ARG_DURATION_MS).takeIf { it > 0 } ?: settings.getIslandPeekDurationMs())
                    .coerceAtMost(MAX_PEEK_MS)
                request(PluginRequest.Peek(key, duration, sticky = false))
            }
            HostRequestType.EXPAND -> if (allowPopup(MIN_EXPAND_GAP_MS)) request(PluginRequest.Expand(key, sticky = false))
            HostRequestType.COLLAPSE, HostRequestType.DISMISS -> request(PluginRequest.Collapse(key))
            HostRequestType.OPEN_APP -> {
                openApp()
                request(PluginRequest.Collapse(key))
                request(PluginRequest.Collapse(BriefPlugin.ITEM_KEY))
            }
            HostRequestType.RESIZE -> heights[rawKey] = args.getInt(HostRequestType.ARG_HEIGHT_PX)
            HostRequestType.KEEP_ALIVE -> scopes[rawKey]?.keepAlive()
            HostRequestType.TEXT_INPUT -> scopes[rawKey]?.setTextInput(args.getBoolean(HostRequestType.ARG_ACTIVE))
        }
    }

    private fun allowPopup(minGapMs: Long): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPopupAt < minGapMs) return false
        lastPopupAt = now
        return true
    }

    private fun openApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(info.packageName) ?: return
        try {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    override val priorityGroup: String = IslandPriorityEntries.providerId(info.packageName)

    override fun namespaced(rawKey: String): String = "${info.packageName}:$rawKey"

    override fun invoke(action: IslandAction, itemKey: String, siblings: Set<String>): Boolean {
        when (action.type) {
            IslandActionType.OPEN_APP -> openApp()
            IslandActionType.CALLBACK -> {
                val id = action.target ?: return false
                try {
                    provider?.onAction(itemKey, id)
                } catch (_: Exception) {
                    return false
                }
            }
            IslandActionType.EXPAND_ITEM -> {
                val target = action.target ?: return false
                val resolved = if (target in siblings) namespaced(target) else target
                if (resolved == BriefPlugin.ITEM_KEY && !settings.isIslandBriefEnabled()) return false
                request(PluginRequest.Expand(resolved))
            }
            IslandActionType.COLLAPSE -> request(PluginRequest.Collapse(namespaced(itemKey)))
            else -> return false
        }
        return true
    }

    override fun surfaceSession(rawKey: String): RemoteSurfaceSession? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return sessions.getOrPut(rawKey) {
            RemoteSurfaceSession({ provider }, rawKey) {
                openApp()
                request(PluginRequest.Collapse(namespaced(rawKey)))
                request(PluginRequest.Collapse(BriefPlugin.ITEM_KEY))
            }
        }
    }

    override fun onLayout(spec: IslandLayoutSpec, density: Float, accent: Color) {
        val next = LayoutSnapshot(
            accent = accent,
            density = density,
            expandedWidthPx = (spec.expandedWidth.value * density).roundToInt(),
            paddingPx = (spec.expandedPadding.value * density).roundToInt(),
            outsetPx = (spec.expandedOutset.value * density).roundToInt(),
            cameraClearancePx = ((spec.expandedOutset + spec.expandedTopPadding + spec.compactHeight).value * density).roundToInt(),
            compactHeightPx = (spec.compactHeight.value * density).roundToInt(),
            cameraSlotWidthPx = (spec.cameraSlotWidth.value * density).roundToInt(),
            cornerPx = (spec.expandedCorner.value * density).roundToInt(),
        )
        if (next == layout) return
        layout = next
        refreshHostInfo()
    }

    override fun reportedHeightPx(rawKey: String): Int = heights[rawKey] ?: 0

    override fun snapshot(rawKey: String): ImageBitmap? = snapshots[rawKey]

    override fun bindScope(rawKey: String, scope: IslandExpandedScope?) {
        if (scope == null) scopes.remove(rawKey) else scopes[rawKey] = scope
    }

    private enum class Status { READY, UNAUTHORIZED, INCOMPATIBLE }

    private data class LayoutSnapshot(
        val accent: Color,
        val density: Float,
        val expandedWidthPx: Int,
        val paddingPx: Int,
        val cameraClearancePx: Int,
        val outsetPx: Int,
        val compactHeightPx: Int,
        val cameraSlotWidthPx: Int,
        val cornerPx: Int,
    )

    private companion object {
        const val INITIAL_RETRY_MS = 2_000L
        const val MAX_RETRY_MS = 60_000L
        const val MIN_UPDATE_GAP_MS = 150L
        const val MIN_PEEK_GAP_MS = 10_000L
        const val MIN_EXPAND_GAP_MS = 30_000L
        const val MAX_PEEK_MS = 8_000L
    }
}
