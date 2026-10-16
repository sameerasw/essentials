package com.sameerasw.essentials.utils.recording

import android.app.PendingIntent
import android.content.Context
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScreenCapture(
    val key: String,
    val packageName: String,
    val startedAt: Long,
    val paused: Boolean,
    val pausedElapsed: Long,
    val stopIntent: PendingIntent?,
    val pauseResumeIntent: PendingIntent?,
    val contentIntent: PendingIntent?,
) {
    val isSystemRecorder get() = packageName == "android" || packageName == "com.android.systemui"

    fun elapsed(now: Long = System.currentTimeMillis()): Long = if (paused) pausedElapsed else now - startedAt
}

class SavedRecording(
    val key: String,
    val duration: Long,
    val openIntent: PendingIntent?,
    val shareIntent: PendingIntent?,
)

// Ongoing screen recorder/cast notifications, which carry the real start time and the recorder's controls.
object ScreenCaptureRepository {
    private const val SAVED_WINDOW_MS = 10_000L

    private val _capture = MutableStateFlow<ScreenCapture?>(null)
    val capture: StateFlow<ScreenCapture?> = _capture.asStateFlow()

    private val _saved = MutableStateFlow<SavedRecording?>(null)
    val saved: StateFlow<SavedRecording?> = _saved.asStateFlow()

    private var ended: ScreenCapture? = null
    private var endedAt = 0L

    fun isTracking(key: String): Boolean = _capture.value?.key == key

    fun update(context: Context, notifications: List<StatusBarNotification>) {
        val previous = _capture.value
        val next = notifications.minByOrNull(::postedStart)?.let { capture(context, it) }
        _capture.value = next
        if (previous != null && next == null) {
            ended = previous
            endedAt = System.currentTimeMillis()
        } else if (next != null) {
            _saved.value = null
        }
    }

    // The recorder's follow-up "saved" notification, posted under the same key or shortly after by the same app.
    fun offerSaved(context: Context, sbn: StatusBarNotification) {
        val last = ended ?: return
        if (sbn.packageName != last.packageName || System.currentTimeMillis() - endedAt > SAVED_WINDOW_MS) return
        if (sbn.isOngoing || sbn.notification.contentIntent == null) return
        ended = null
        _saved.value = SavedRecording(
            key = sbn.key,
            duration = last.elapsed(endedAt),
            openIntent = sbn.notification.contentIntent,
            shareIntent = action(context, sbn, R.string.action_share, "share"),
        )
    }

    fun clearSaved(key: String? = null) {
        if (key == null || _saved.value?.key == key) _saved.value = null
    }

    private fun capture(context: Context, sbn: StatusBarNotification): ScreenCapture {
        val controls = RemoteViewsControls.read(context, sbn)
        val bootEpoch = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        val base = controls?.chronometerBase?.plus(bootEpoch)
        val paused = controls?.chronometerRunning == false
        val startedAt = base ?: postedStart(sbn)
        val system = sbn.packageName == "android" || sbn.packageName == "com.android.systemui"
        return ScreenCapture(
            key = sbn.key,
            packageName = sbn.packageName,
            startedAt = startedAt,
            paused = paused,
            // A stopped chronometer shows the time it had when the notification was last posted.
            pausedElapsed = (sbn.postTime - startedAt).coerceAtLeast(0L),
            stopIntent = controls?.stop ?: action(context, sbn, R.string.action_stop, "stop")
                // The AOSP system recorder stops when its notification is tapped.
                ?: sbn.notification.contentIntent.takeIf { system },
            pauseResumeIntent = controls?.pauseResume
                ?: action(context, sbn, R.string.island_travel_pause, "pause")
                ?: action(context, sbn, R.string.island_travel_resume, "resume"),
            contentIntent = sbn.notification.contentIntent,
        )
    }

    private fun postedStart(sbn: StatusBarNotification): Long = sbn.notification.`when`.takeIf { it > 0L } ?: sbn.postTime

    private fun action(context: Context, sbn: StatusBarNotification, labelRes: Int, keyword: String): PendingIntent? {
        val label = context.getString(labelRes).lowercase()
        return sbn.notification.actions?.firstOrNull { action ->
            val title = action.title?.toString()?.lowercase() ?: return@firstOrNull false
            keyword in title || label in title
        }?.actionIntent
    }
}
