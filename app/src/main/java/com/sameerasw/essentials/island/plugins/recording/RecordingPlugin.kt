package com.sameerasw.essentials.island.plugins.recording

import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.sameerasw.essentials.island.plugins.launchPackage
import com.sameerasw.essentials.island.plugins.sendPendingIntent
import com.sameerasw.essentials.island.ui.IslandHaptics
import com.sameerasw.essentials.island.ui.IslandTextStyles
import com.sameerasw.essentials.island.ui.components.CameraRow
import com.sameerasw.essentials.island.ui.components.ConnectedButtonRow
import com.sameerasw.essentials.island.ui.components.ConnectedItem
import com.sameerasw.essentials.island.ui.components.IslandIcon
import com.sameerasw.essentials.island.ui.components.MarqueeText
import com.sameerasw.essentials.island.ui.components.RollingText
import com.sameerasw.essentials.utils.recording.ScreenCapture
import com.sameerasw.essentials.utils.recording.SavedRecording
import com.sameerasw.essentials.utils.recording.ScreenCaptureRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class RecordingPlugin : BaseIslandPlugin() {
    override val id = "recording"

    override val settingKeys = setOf(
        SettingsRepository.KEY_ISLAND_SHOW_RECORDING,
        SettingsRepository.KEY_ISLAND_RECORDING_SCREEN,
        SettingsRepository.KEY_ISLAND_RECORDING_CAMERA,
        SettingsRepository.KEY_ISLAND_RECORDING_MICROPHONE,
        SettingsRepository.KEY_ISLAND_RECORDING_SHOW_TIME,
        SettingsRepository.KEY_ISLAND_RECORDING_FOCUS,
    )

    private enum class Source(val label: Int, val shortLabel: Int) {
        Screen(R.string.island_recording_screen, R.string.island_recording_source_screen),
        Camera(R.string.island_recording_camera, R.string.island_recording_source_camera),
        Microphone(R.string.island_recording_microphone, R.string.island_recording_source_microphone),
    }

    private val audio by lazy { context.getSystemService(AudioManager::class.java) }

    // Kept across island restarts so a running recording keeps its real elapsed time.
    private var capture: ScreenCapture? = null
    private val sessions = HashMap<Int, Pair<Source, Long>>()
    private var lastShown: Set<Source> = emptySet()

    private var saved: SavedRecording? = null
    private var observer: Job? = null
    private var savedObserver: Job? = null
    private var savedTimeout: Job? = null
    private var ticker: Job? = null

    private val audioCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            if (ctx == null) return
            syncSessions(configs)
            update()
        }
    }

    override fun onStart() {
        val c = ctx!!
        try {
            audio.registerAudioRecordingCallback(audioCallback, c.mainHandler)
            syncSessions(audio.activeRecordingConfigurations)
        } catch (_: Exception) {
            sessions.clear()
        }
        savedObserver = c.scope.launch {
            ScreenCaptureRepository.saved.collect {
                val fresh = it != null && it.key != saved?.key
                saved = it
                render()
                if (fresh && shown().isEmpty() && savedItem() != null) {
                    c.request(PluginRequest.Peek(SAVED_KEY, settings.getIslandPeekDurationMs()))
                    scheduleSavedTimeout()
                }
            }
        }
        observer = c.scope.launch {
            ScreenCaptureRepository.capture.collect {
                capture = it
                update()
            }
        }
    }

    override fun onStop() {
        observer?.cancel()
        savedObserver?.cancel()
        savedTimeout?.cancel()
        ticker?.cancel()
        ticker = null
        try {
            audio.unregisterAudioRecordingCallback(audioCallback)
        } catch (_: Exception) {
        }
    }

    override fun refresh() {
        if (ctx == null) return
        update()
    }

    private fun syncSessions(configs: List<AudioRecordingConfiguration>) {
        val now = System.currentTimeMillis()
        val active = configs.filter(::isUserRecording).associateBy { it.clientAudioSessionId }
        sessions.keys.retainAll(active.keys)
        active.forEach { (id, config) ->
            val source = if (config.clientAudioSource == MediaRecorder.AudioSource.CAMCORDER) Source.Camera else Source.Microphone
            sessions.getOrPut(id) { source to now }
        }
    }

    // Calls have their own island item; hotword, internal audio capture and silenced clients are not the user recording.
    private fun isUserRecording(config: AudioRecordingConfiguration): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && config.isClientSilenced) return false
        return config.clientAudioSource !in IGNORED_SOURCES
    }

    private fun elapsed(source: Source, now: Long = System.currentTimeMillis()): Long? = when (source) {
        Source.Screen -> capture?.elapsed(now)
        else -> sessions.values.filter { it.first == source }.minOfOrNull { it.second }?.let { now - it }
    }

    private fun isEnabled(source: Source): Boolean = when (source) {
        Source.Screen -> settings.isIslandRecordingScreenEnabled()
        Source.Camera -> settings.isIslandRecordingCameraEnabled()
        Source.Microphone -> settings.isIslandRecordingMicrophoneEnabled()
    }

    private fun shown(): List<Source> =
        if (ctx == null || !settings.isIslandShowRecordingEnabled()) emptyList()
        else Source.entries.filter { isEnabled(it) && elapsed(it) != null }

    private fun update() {
        val shown = shown()
        val started = shown.any { it !in lastShown }
        lastShown = shown.toSet()
        render()
        if (started && settings.isIslandRecordingPeekEnabled()) {
            ctx?.request(PluginRequest.Peek(ITEM_KEY, settings.getIslandPeekDurationMs()))
        }
    }

    private fun syncTicker(running: Boolean) {
        if (!running) {
            ticker?.cancel()
            ticker = null
            return
        }
        if (ticker?.isActive == true) return
        ticker = ctx?.scope?.launch {
            while (isActive) {
                val elapsed = shown().firstOrNull()?.let(::elapsed) ?: return@launch
                delay(1000L - elapsed.mod(1000L) + 2L)
                render()
            }
        }
    }

    private fun render() {
        val shown = shown()
        val primary = shown.firstOrNull()
        val elapsed = primary?.let(::elapsed)
        syncTicker(elapsed != null)
        if (primary == null || elapsed == null) {
            publish(savedItem())
            return
        }
        val screen = capture.takeIf { primary == Source.Screen }
        val paused = screen?.paused == true
        val label = when {
            paused -> context.getString(R.string.island_recording_paused)
            shown.size > 1 -> shown.joinToString(" · ") { context.getString(it.shortLabel) }
            else -> context.getString(primary.label)
        }
        val time = format(elapsed)
        val showTime = settings.isIslandRecordingShowTimeEnabled()
        val stop = screen?.stopIntent
        val pauseResume = screen?.pauseResumeIntent
        val openable = screen?.takeUnless { it.isSystemRecorder }
        publish(
            IslandItem(
                key = ITEM_KEY,
                priority = IslandPriority.RECORDING,
                placement = CompactPlacement.Dynamic,
                compact = listOfNotNull(
                    CompactCell("recording.dot") { RecordingDot(10.dp, paused) },
                    if (showTime) CompactCell("recording.time") { RollingText(time) } else null,
                ),
                line = LineContent(
                    icon = { RecordingDot(10.dp, paused) },
                    start = label,
                    end = time,
                ),
                expanded = ExpandedContent { scope ->
                    val spec = scope.spec
                    val side = spec.expandedPadding + spec.expandedCorner * 0.35f
                    Column(Modifier.padding(spec.expandedOutset)) {
                        Spacer(Modifier.height(spec.expandedTopPadding))
                        scope.CameraRow(
                            horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                            start = { MarqueeText(label, IslandTextStyles.title, Modifier.weight(1f)) },
                            end = {},
                        )
                        Row(
                            modifier = Modifier.padding(horizontal = side),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RecordingDot(12.dp, paused)
                            Spacer(Modifier.width(10.dp))
                            RollingText(
                                text = time,
                                style = IslandTextStyles.compact.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f),
                            )
                            if (pauseResume != null) {
                                RecordingButton(
                                    container = Color.White.copy(alpha = 0.2f),
                                    onClick = {
                                        scope.keepAlive()
                                        sendPendingIntent(context, pauseResume)
                                    },
                                ) {
                                    IslandIcon(if (paused) R.drawable.rounded_play_arrow_24 else R.drawable.rounded_pause_24, size = 22.dp)
                                }
                            }
                            if (stop != null) {
                                Spacer(Modifier.width(8.dp))
                                RecordingButton(
                                    container = RecordRed,
                                    onClick = {
                                        sendPendingIntent(context, stop)
                                        scope.collapse()
                                    },
                                ) {
                                    Box(Modifier.size(14.dp).background(Color.White, RoundedCornerShape(3.dp)))
                                }
                            }
                        }
                        Spacer(Modifier.height(spec.expandedBottomPadding))
                    }
                },
                onOpen = openable?.let { capture -> { if (!sendPendingIntent(context, capture.contentIntent)) launchPackage(context, capture.packageName) } },
                sourcePackage = openable?.packageName,
                exclusive = settings.isIslandRecordingFocusEnabled(),
            ),
        )
    }

    // Kept while the user is looking at it; cleared otherwise so the island doesn't hold on to it.
    private fun scheduleSavedTimeout() {
        savedTimeout?.cancel()
        savedTimeout = ctx?.scope?.launch {
            delay(SAVED_SHOW_MS)
            while (ctx?.focusedKey?.invoke() == SAVED_KEY) delay(1000L)
            ScreenCaptureRepository.clearSaved()
        }
    }

    private fun savedItem(): IslandItem? {
        val recording = saved ?: return null
        if (ctx == null || !settings.isIslandShowRecordingEnabled() || !settings.isIslandRecordingScreenEnabled()) return null
        val label = context.getString(R.string.island_recording_saved)
        val duration = format(recording.duration)
        return IslandItem(
            key = SAVED_KEY,
            priority = IslandPriority.RECORDING,
            placement = CompactPlacement.Dynamic,
            compact = listOf(
                CompactCell("recording.saved") { IslandIcon(R.drawable.rounded_check_circle_24, size = 18.dp, tint = RecordRed) },
            ),
            line = LineContent(
                icon = { IslandIcon(R.drawable.rounded_check_circle_24, tint = RecordRed) },
                start = label,
                end = duration,
            ),
            expanded = ExpandedContent { scope ->
                val spec = scope.spec
                val side = spec.expandedPadding + spec.expandedCorner * 0.35f
                Column(Modifier.padding(spec.expandedOutset)) {
                    Spacer(Modifier.height(spec.expandedTopPadding))
                    scope.CameraRow(
                        horizontalPadding = spec.cameraGap + spec.expandedCorner * 0.35f,
                        start = { MarqueeText(label, IslandTextStyles.title, Modifier.weight(1f)) },
                        end = { Text(duration, style = IslandTextStyles.body, maxLines = 1) },
                    )
                    Spacer(Modifier.height(14.dp))
                    ConnectedButtonRow(
                        items = listOfNotNull(
                            recording.openIntent?.let { intent ->
                                ConnectedItem({
                                    sendPendingIntent(context, intent)
                                    ScreenCaptureRepository.clearSaved()
                                }) { SavedAction(R.drawable.rounded_open_in_new_24, context.getString(R.string.island_tap_action_open)) }
                            },
                            recording.shareIntent?.let { intent ->
                                ConnectedItem({
                                    sendPendingIntent(context, intent)
                                    ScreenCaptureRepository.clearSaved()
                                }) { SavedAction(R.drawable.rounded_share_24, context.getString(R.string.action_share)) }
                            },
                        ),
                        height = 44.dp,
                        modifier = Modifier.padding(horizontal = side),
                        container = Color.White.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(spec.expandedBottomPadding))
                }
            },
            dismissible = true,
            onDismiss = { ScreenCaptureRepository.clearSaved() },
            onOpen = recording.openIntent?.let { intent -> { sendPendingIntent(context, intent) } },
        )
    }

    private fun format(millis: Long): String {
        val total = millis.coerceAtLeast(0L) / 1000L
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        else String.format(Locale.getDefault(), "%d:%02d", m, s)
    }

    companion object {
        const val ITEM_KEY = "recording"
        const val SAVED_KEY = "recording.saved"
        private const val SAVED_SHOW_MS = 8_000L
        private const val AUDIO_SOURCE_HOTWORD = 1999
        private val IGNORED_SOURCES = setOf(
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.VOICE_UPLINK,
            MediaRecorder.AudioSource.VOICE_DOWNLINK,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.REMOTE_SUBMIX,
            AUDIO_SOURCE_HOTWORD,
        )
    }
}

private val RecordRed = Color(0xFFFF3B30)

@Composable
private fun RecordingDot(size: Dp, paused: Boolean = false) {
    val alpha by rememberInfiniteTransition(label = "recordingPulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "recordingAlpha",
    )
    Box(
        Modifier
            .size(size)
            .graphicsLayer { this.alpha = if (paused) 1f else alpha }
            .background(if (paused) Color.White.copy(alpha = 0.6f) else RecordRed, CircleShape),
    )
}

@Composable
private fun RecordingButton(container: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(container)
            .clickable {
                IslandHaptics.button(context)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SavedAction(icon: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IslandIcon(icon, size = 18.dp)
        Text(label, style = IslandTextStyles.line, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
