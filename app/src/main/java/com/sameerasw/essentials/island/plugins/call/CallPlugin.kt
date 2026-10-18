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
import com.sameerasw.essentials.services.LiveUpdateSnoozer
import com.sameerasw.essentials.services.NotificationListener
import com.sameerasw.essentials.utils.CallControlUtil
import com.sameerasw.essentials.utils.PermissionUtils
import com.sameerasw.essentials.utils.call.CallActionRole
import com.sameerasw.essentials.utils.call.CallNotificationAction
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

    private fun popUp() {
        val c = ctx ?: return
        val sticky = settings.getBoolean(SettingsRepository.KEY_ISLAND_CALL_STICKY, false)
        val mode = settings.getString(SettingsRepository.KEY_ISLAND_CALL_SHOW_MODE, SettingsRepository.ISLAND_CALL_SHOW_PEEK)
        when {
            mode == SettingsRepository.ISLAND_CALL_SHOW_COMPACT -> Unit
            mode == SettingsRepository.ISLAND_CALL_SHOW_PEEK && settings.isIslandLineStageEnabled() ->
                c.request(PluginRequest.Peek(ITEM_KEY, settings.getIslandTimeoutMs(), sticky))
            else -> c.request(PluginRequest.Expand(ITEM_KEY, sticky))
        }
    }

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
                if (next == null) restoreHeadsUpAfterCall()
                restartTicker()
                render()
                if (startedRinging) {
                    if (enabled(next)) popUp() else releaseHeadsUpForCall()
                }
            }
        }
    }

    private var headsUpReleased = false

    private fun releaseHeadsUpForCall() {
        if (!settings.isIslandSuppressSystemHeadsUpEnabled()) return
        try {
            settings.applyHeadsUpSuppression(false)
            headsUpReleased = true
        } catch (_: Exception) {
        }
    }

    private fun restoreHeadsUpAfterCall() {
        if (!headsUpReleased) return
        headsUpReleased = false
        try {
            if (settings.isIslandSuppressSystemHeadsUpEnabled()) settings.applyHeadsUpSuppression(true)
        } catch (_: Exception) {
        }
    }

    override fun onStop() {
        restoreHeadsUpAfterCall()
        observer?.cancel()
        ticker?.cancel()
        call = null
    }

    override fun refresh() = render()

    // App (notification) calls carry their own controls and need no phone permissions; plain telephony calls do.
    private fun enabled(snap: CallSnapshot? = call): Boolean {
        if (!settings.isIslandShowCallsEnabled()) return false
        val fromNotification = snap?.appName != null || snap?.answerIntent != null || snap?.endIntent != null
        return fromNotification || PermissionUtils.hasCallPermissions(context)
    }

    private fun restartTicker() {
        ticker?.cancel()
        if (call == null) return
        ticker = ctx?.scope?.launch {
            while (isActive) {
                delay(1000L - System.currentTimeMillis() % 1000L)
                CallStateRepository.reconcile(context, activeNotificationKeys())
                if (call?.phase == CallPhase.Active) render()
            }
        }
    }

    private fun activeNotificationKeys(): Set<String>? = try {
        val listener = NotificationListener.instance ?: return null
        val keys = listener.activeNotifications.orEmpty().map { it.key }.toMutableSet()
        LiveUpdateSnoozer.snoozedNotifications().forEach { keys += it.key }
        keys
    } catch (_: Exception) {
        null
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
        val buttons = buttonsFor(snap, ringing)
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
                        buttons = buttons,
                        scope = scope,
                    )
                },
                accent = null,
                onOpen = { guarded { if (!sendPendingIntent(context, snap.contentIntent)) CallControlUtil.showInCallScreen(context) } },
                sourcePackage = snap.packageName,
            ),
        )
    }

    private fun buttonsFor(snap: CallSnapshot, ringing: Boolean): List<CallButtonSpec> {
        val telephony = snap.fromTelephony
        val canAnswer = snap.answerIntent != null || telephony
        val canEnd = snap.endIntent != null || telephony
        val buttons = mutableListOf<CallButtonSpec>()
        fun end(label: Int) {
            if (!canEnd) return
            buttons += CallButtonSpec(
                key = "end",
                icon = R.drawable.rounded_call_end_24,
                label = context.getString(label),
                style = CallButtonStyle.End,
                onClick = {
                    guarded {
                        if (telephony) CallControlUtil.endCall(context)
                        else if (!sendPendingIntent(context, snap.endIntent)) CallControlUtil.endCall(context)
                    }
                    null
                },
            )
        }
        fun answer() {
            if (!canAnswer) return
            buttons += CallButtonSpec(
                key = "answer",
                icon = R.drawable.rounded_call_24,
                label = context.getString(R.string.island_call_answer),
                style = CallButtonStyle.Answer,
                onClick = {
                    guarded {
                        if (telephony) CallControlUtil.acceptCall(context)
                        else if (!sendPendingIntent(context, snap.answerIntent)) CallControlUtil.acceptCall(context)
                    }
                    null
                },
            )
        }
        if (ringing) {
            end(R.string.island_call_decline)
            answer()
        } else {
            val own = snap.extraActions
            val mute = own.firstOrNull { it.role == CallActionRole.Mute }
            val speaker = own.firstOrNull { it.role == CallActionRole.Speaker }
            if (telephony) {
                buttons += CallButtonSpec(
                    key = "mute",
                    icon = R.drawable.rounded_mic_24,
                    onIcon = R.drawable.rounded_mic_off_24,
                    label = context.getString(R.string.island_call_mute),
                    initialOn = guarded(false) { CallControlUtil.isMuted(context) },
                    onClick = { guarded<Boolean?>(null) { CallControlUtil.toggleMute(context) } },
                )
                buttons += CallButtonSpec(
                    key = "speaker",
                    icon = R.drawable.rounded_volume_up_24,
                    label = context.getString(R.string.island_call_speaker),
                    initialOn = guarded(false) { CallControlUtil.isSpeakerOn(context) },
                    onClick = { guarded<Boolean?>(null) { CallControlUtil.toggleSpeaker(context) } },
                )
            } else {
                if (mute != null) {
                    buttons += ownButton(mute, R.drawable.rounded_mic_24, on = mute.title.lowercase().contains("unmute"), onIcon = R.drawable.rounded_mic_off_24)
                }
                if (speaker != null) {
                    buttons += ownButton(speaker, R.drawable.rounded_volume_up_24, on = false)
                }
            }
            own.filter { it.role != CallActionRole.Mute && it.role != CallActionRole.Speaker }
                .take(MAX_EXTRA_BUTTONS)
                .forEach { action ->
                    val icon = when (action.role) {
                        CallActionRole.Hold -> R.drawable.rounded_pause_24
                        CallActionRole.Video -> R.drawable.rounded_videocam_24
                        else -> null
                    }
                    buttons += ownButton(action, icon, on = false)
                }
            end(R.string.island_call_end)
        }
        if (buttons.isEmpty()) {
            buttons += CallButtonSpec(
                key = "open",
                icon = R.drawable.rounded_open_in_new_24,
                label = context.getString(R.string.island_call_open),
                onClick = {
                    guarded { if (!sendPendingIntent(context, snap.contentIntent)) CallControlUtil.showInCallScreen(context) }
                    null
                },
            )
        }
        return buttons
    }

    private fun ownButton(action: CallNotificationAction, icon: Int?, on: Boolean, onIcon: Int? = null) = CallButtonSpec(
        key = "own:${action.role}:${action.title}",
        icon = icon,
        onIcon = onIcon,
        label = action.title,
        initialOn = on,
        onClick = {
            guarded { sendPendingIntent(context, action.intent) }
            null
        },
    )

    private fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
        }
    }

    private fun <T> guarded(fallback: T, block: () -> T): T = try {
        block()
    } catch (_: Exception) {
        fallback
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
        private const val MAX_EXTRA_BUTTONS = 2
    }
}

private fun ringingSlot(ringing: Boolean): (@Composable () -> Unit)? = if (ringing) ({ RingingIcon() }) else null

@Composable
private fun RingingIcon() {
    IslandIcon(R.drawable.rounded_call_24, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
}
