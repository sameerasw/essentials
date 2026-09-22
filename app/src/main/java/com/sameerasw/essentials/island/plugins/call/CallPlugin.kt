package com.sameerasw.essentials.island.plugins.call

import com.sameerasw.essentials.island.plugins.sendPendingIntent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sameerasw.essentials.R
import com.sameerasw.essentials.data.repository.SettingsRepository
import com.sameerasw.essentials.island.model.CompactCell
import com.sameerasw.essentials.island.model.CompactPlacement
import com.sameerasw.essentials.island.model.ExpandedContent
import com.sameerasw.essentials.island.model.IslandItem
import com.sameerasw.essentials.island.model.IslandPriority
import com.sameerasw.essentials.island.model.LineContent
import com.sameerasw.essentials.island.model.PluginRequest
import com.sameerasw.essentials.island.plugins.BaseIslandPlugin
import com.sameerasw.essentials.island.ui.components.IslandBitmap
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.utils.CallControlUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.call.CallPhase
import com.sameerasw.essentials.utils.call.CallSnapshot
import com.sameerasw.essentials.utils.call.CallStateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class CallPlugin : BaseIslandPlugin() {
    override val id = "call"

    override val settingKeys = setOf(SettingsRepository.KEY_ISLAND_SHOW_CALLS)

    private var call: CallSnapshot? = null
    private var ticker: Job? = null
    private var observer: Job? = null

    override fun onStart() {
        val c = ctx!!
        observer = c.scope.launch {
            CallStateRepository.state.collect { next ->
                val previous = call
                call = next
                val startedRinging = next?.phase == CallPhase.Ringing && previous?.phase != CallPhase.Ringing
                if (startedRinging) {
                    if (enabled()) c.request(PluginRequest.Expand(ITEM_KEY)) else releaseHeadsUpForCall()
                }
                if (next == null) restoreHeadsUpAfterCall()
                restartTicker()
                render()
            }
        }
    }

    private var headsUpReleased = false

    private fun releaseHeadsUpForCall() {
        if (!settings.isIslandSuppressSystemHeadsUpEnabled()) return
        settings.applyHeadsUpSuppression(false)
        headsUpReleased = true
    }

    private fun restoreHeadsUpAfterCall() {
        if (!headsUpReleased) return
        headsUpReleased = false
        if (settings.isIslandSuppressSystemHeadsUpEnabled()) settings.applyHeadsUpSuppression(true)
    }

    override fun onStop() {
        restoreHeadsUpAfterCall()
        observer?.cancel()
        ticker?.cancel()
        call = null
    }

    override fun refresh() = render()

    private fun enabled() = settings.isIslandShowCallsEnabled() && PermissionUtils.hasCallPermissions(context)

    private fun restartTicker() {
        ticker?.cancel()
        if (call?.phase != CallPhase.Active) return
        ticker = ctx?.scope?.launch {
            while (isActive) {
                delay(1000L - System.currentTimeMillis() % 1000L)
                render()
            }
        }
    }

    private fun render() {
        val snap = call
        if (ctx == null || snap == null || !enabled()) {
            publish(null)
            return
        }
        val ringing = snap.phase == CallPhase.Ringing
        val caller = snap.name ?: snap.number ?: context.getString(R.string.island_call_incoming)
        val incomingLabel = snap.appName?.let { context.getString(R.string.island_call_incoming_app, it) }
            ?: context.getString(R.string.island_call_incoming)
        val status = if (ringing) incomingLabel else elapsed(snap.startedAt)
        val photo = snap.photo
        val actions = CallActions(
            answer = { if (!sendPendingIntent(context, snap.answerIntent)) CallControlUtil.acceptCall(context) },
            end = { if (!sendPendingIntent(context, snap.endIntent)) CallControlUtil.endCall(context) },
            toggleMute = { CallControlUtil.toggleMute(context) },
            toggleSpeaker = { CallControlUtil.toggleSpeaker(context) },
            isMuted = { CallControlUtil.isMuted(context) },
            isSpeakerOn = { CallControlUtil.isSpeakerOn(context) },
        )
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.CALL,
                placement = CompactPlacement.Dynamic,
                compact = listOf(
                    CompactCell("call.avatar") { IslandBitmap(photo, 22.dp, circle = true, fallbackRes = R.drawable.rounded_call_24) },
                    if (ringing) {
                        CompactCell("call.ringing") { RingingIcon() }
                    } else {
                        CompactCell("call.timer") { RollingText(status) }
                    },
                ),
                line = LineContent(
                    icon = { IslandBitmap(photo, 24.dp, circle = true, fallbackRes = R.drawable.rounded_call_24) },
                    start = caller,
                    end = status,
                    endSlot = ringingSlot(ringing),
                ),
                expanded = ExpandedContent { scope ->
                    CallExpanded(
                        caller = caller,
                        number = snap.number?.takeIf { snap.name != null },
                        photo = photo,
                        ringing = ringing,
                        status = status,
                        actions = actions,
                        scope = scope,
                    )
                },
                accent = null,
                onOpen = { if (!sendPendingIntent(context, snap.contentIntent)) CallControlUtil.showInCallScreen(context) },
            ),
        )
    }

    private fun elapsed(startedAt: Long): String {
        val total = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(0L)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    companion object {
        const val ITEM_KEY = "call"
    }
}

private fun ringingSlot(ringing: Boolean): (@Composable () -> Unit)? = if (ringing) ({ RingingIcon() }) else null

@Composable
private fun RingingIcon() {
    IslandIcon(R.drawable.rounded_call_24, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
}
