package com.sameerasw.essentials.utils.chronometer

import com.sameerasw.essentials.utils.AppIconUtil
import android.os.SystemClock
import android.os.Build
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.TextView
import com.sameerasw.essentials.R
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.utils.AppUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class ChronometerAction(val title: String, val intent: PendingIntent?, val click: (() -> Unit)? = null) {
    fun perform(context: Context) {
        click?.invoke() ?: intent?.let { runCatching { it.send() } }
    }
}

data class ChronometerEntry(
    val key: String,
    val packageName: String,
    val appName: String?,
    val appIcon: Bitmap?,
    val glyph: Bitmap?,
    val title: String?,

    val countDown: Boolean,
    val base: Long,
    val running: Boolean,
    
    val frozenMillis: Long,
    val actions: List<ChronometerAction>,
    val contentIntent: PendingIntent?,
    val postedAt: Long,
) {
    fun displayMillis(now: Long = System.currentTimeMillis()): Long = when {
        !running -> frozenMillis
        countDown -> (base - now).coerceAtLeast(0L)
        else -> (now - base).coerceAtLeast(0L)
    }
}


object ChronometerRepository {
    private const val EXTRA_COUNT_DOWN = "android.chronometerCountDown"

    
    private const val EXTRA_METRICS = "android.metrics"
    private const val EXTRA_METRICS_CRITICAL_INDEX = "android.metrics.criticalIndex"
    private const val KEY_LABEL = "label"
    private const val KEY_VALUE = "value"
    private const val KEY_TYPE = "_type"
    private const val TYPE_TIME_DIFFERENCE = 1
    private const val KEY_ZERO_TIME = "zeroTime"
    private const val KEY_ZERO_ELAPSED_REALTIME = "zeroElapsedRealtime"
    private const val KEY_PAUSED_DURATION = "pausedDuration"
    private const val KEY_METRIC_COUNT_DOWN = "countDown"

    private class TimeMetric(val label: String?, val countDown: Boolean, val zeroEpochMillis: Long?, val pausedMillis: Long?)

    @Suppress("DEPRECATION")
    private fun timeMetric(extras: Bundle): TimeMetric? {
        val list: List<Bundle> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelableArrayList(EXTRA_METRICS, Bundle::class.java)
            } else {
                extras.getParcelableArrayList(EXTRA_METRICS)
            }
        } catch (_: Exception) {
            null
        } ?: return null
        val critical = list.getOrNull(extras.getInt(EXTRA_METRICS_CRITICAL_INDEX, 0))
        val metric = (listOfNotNull(critical) + list).firstOrNull {
            it.getBundle(KEY_VALUE)?.getInt(KEY_TYPE) == TYPE_TIME_DIFFERENCE
        } ?: return null
        val value = metric.getBundle(KEY_VALUE) ?: return null
        val zeroEpoch = when {
            value.containsKey(KEY_ZERO_TIME) -> value.getLong(KEY_ZERO_TIME)
            
            value.containsKey(KEY_ZERO_ELAPSED_REALTIME) ->
                System.currentTimeMillis() + (value.getLong(KEY_ZERO_ELAPSED_REALTIME) - SystemClock.elapsedRealtime())
            else -> null
        }
        val paused = if (value.containsKey(KEY_PAUSED_DURATION)) value.getLong(KEY_PAUSED_DURATION) else null
        if (zeroEpoch == null && paused == null) return null
        return TimeMetric(metric.getString(KEY_LABEL), value.getBoolean(KEY_METRIC_COUNT_DOWN), zeroEpoch, paused)
    }

    private val _entries = MutableStateFlow<List<ChronometerEntry>>(emptyList())
    val entries: StateFlow<List<ChronometerEntry>> = _entries.asStateFlow()

    private val tracked = LinkedHashMap<String, ChronometerEntry>()

    // Google Recorder posts a custom media-style view with no chronometer extras; `when` is the recording start.
    private const val RECORDER_PACKAGE = "com.google.android.apps.recorder"
    private const val RECORDER_CHANNEL = "Record"

    private fun isRecorderSession(sbn: StatusBarNotification): Boolean =
        sbn.packageName == RECORDER_PACKAGE && sbn.notification.channelId == RECORDER_CHANNEL && sbn.isOngoing

    fun isCandidate(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (isRecorderSession(sbn)) return true
        if (n.category == Notification.CATEGORY_CALL || n.category == Notification.CATEGORY_TRANSPORT) return false
        if (n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return false
        val chronometer = n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || timeMetric(n.extras) != null
        
        return (chronometer && sbn.isOngoing && !hasProgress(n.extras)) || synchronized(this) { tracked.containsKey(sbn.key) }
    }

    private fun hasProgress(extras: Bundle): Boolean =
        extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0 ||
            extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)

    @Synchronized
    fun onPosted(context: Context, sbn: StatusBarNotification) {
        val n = sbn.notification
        val extras = n.extras
        val previous = tracked[sbn.key]
        if (!sbn.isOngoing || hasProgress(extras)) {
            remove(sbn.key)
            return
        }
        val metric = timeMetric(extras)
        val recorder = isRecorderSession(sbn)
        val now = System.currentTimeMillis()
        val running: Boolean
        val countDown: Boolean
        val base: Long
        val frozen: Long
        val custom = if (recorder || n.actions.isNullOrEmpty()) inflateCustom(context, n) else null
        val clock = custom?.let { findChronometer(it) }
        if (recorder && clock != null) {
            // The custom view's Chronometer is the real counter; `when` is reset on every pause/resume repost.
            val elapsed = (SystemClock.elapsedRealtime() - clock.base).coerceAtLeast(0L)
            running = chronometerStarted(clock) ?: !hasResumeButton(custom)
            countDown = false
            base = now - elapsed
            frozen = when {
                running -> 0L
                previous?.running == true -> previous.displayMillis(now)
                else -> elapsed
            }
        } else if (recorder) {
            running = previous?.running ?: true
            countDown = false
            base = previous?.base ?: n.`when`
            frozen = previous?.frozenMillis ?: 0L
        } else if (metric != null) {
            running = metric.zeroEpochMillis != null
            countDown = metric.countDown
            base = metric.zeroEpochMillis ?: previous?.base ?: now
            frozen = metric.pausedMillis ?: 0L
        } else {
            running = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
            countDown = if (running) extras.getBoolean(EXTRA_COUNT_DOWN) else previous?.countDown ?: false
            base = if (running) n.`when` else previous?.base ?: n.`when`
            frozen = when {
                running -> 0L
                previous?.running == true -> previous.displayMillis(now)
                else -> previous?.frozenMillis ?: parseClock(extras) ?: 0L
            }
        }
        
        if (running && !countDown && abs(now - base) > 7L * 24 * 3600 * 1000) return

        val appName = previous?.appName ?: try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) {
            null
        }
        val appIcon = previous?.appIcon ?: try {
            AppUtil.drawableToBitmap(AppIconUtil.islandAppIcon(context, sbn.packageName))
        } catch (_: Exception) {
            null
        }
        val glyph = previous?.glyph ?: try {
            n.smallIcon?.loadDrawable(context)?.let { AppUtil.drawableToBitmap(it) }
        } catch (_: Exception) {
            null
        } ?: runCatching { AppIconUtil.monochromeAppIcon(context, sbn.packageName) }.getOrNull()
        val notifActions = n.actions.orEmpty().mapNotNull { a ->
            val title = a.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            a.actionIntent?.let { ChronometerAction(title, it) }
        }
        tracked[sbn.key] = ChronometerEntry(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            appIcon = appIcon,
            glyph = glyph,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
                ?: metric?.label?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.island_recording_default).takeIf { recorder },
            countDown = countDown,
            base = base,
            running = running,
            frozenMillis = frozen,
            actions = notifActions.ifEmpty { custom?.let { customViewActions(it) }.orEmpty() },
            contentIntent = n.contentIntent,
            postedAt = sbn.postTime,
        )
        publish()
    }

    // Apps like Recorder put their buttons only in a custom RemoteViews layout; inflate it and reuse its click handlers.
    private fun inflateCustom(context: Context, n: Notification): View? {
        val remote = n.bigContentView ?: n.contentView ?: return null
        if (Looper.myLooper() != Looper.getMainLooper()) return null
        return runCatching { remote.apply(context, FrameLayout(context)) }.getOrNull()
    }

    private fun findChronometer(v: View): Chronometer? {
        if (v is Chronometer) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findChronometer(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun chronometerStarted(c: Chronometer): Boolean? = runCatching {
        Chronometer::class.java.getDeclaredField("mStarted").apply { isAccessible = true }.getBoolean(c)
    }.getOrNull()

    private val RESUME_WORDS = listOf("resume", "continue", "play")

    private fun hasResumeButton(root: View): Boolean {
        val names = ArrayList<String>()
        fun walk(v: View) {
            if (v !== root && v.isClickable && v.visibility == View.VISIBLE && v.id != View.NO_ID) {
                runCatching { v.resources.getResourceEntryName(v.id) }.getOrNull()?.let { names += it.lowercase() }
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
        val labels = customViewActions(root).map { it.title.lowercase() }
        val resume = names.any { n -> RESUME_WORDS.any { n.contains(it) } } || labels.any { l -> RESUME_WORDS.any { l.contains(it) } }
        val pause = names.any { it.contains("pause") } || labels.any { it.contains("pause") }
        return resume && !pause
    }

    private fun customViewActions(root: View): List<ChronometerAction> {
        val found = ArrayList<ChronometerAction>()
        fun walk(v: View) {
            if (v !== root && v.isClickable && v.hasOnClickListeners() && v.visibility == View.VISIBLE) {
                val label = v.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                    ?: (v as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }
                if (label != null) found += ChronometerAction(label, null) { v.performClick() }
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
        return found.distinctBy { it.title }
    }

    @Synchronized
    fun onRemoved(key: String) = remove(key)

    @Synchronized
    fun clear() {
        if (tracked.isEmpty()) return
        tracked.clear()
        publish()
    }

    private fun remove(key: String) {
        if (tracked.remove(key) != null) publish()
    }

    private fun publish() {
        _entries.value = tracked.values.toList()
    }

    
    private val CLOCK_PATTERN = Regex("""(\p{Nd}{1,2})[:\uFF1A\u0589\u2236](\p{Nd}{2})(?:[:\uFF1A\u0589\u2236](\p{Nd}{2}))?""")

    private fun parseClock(extras: Bundle): Long? {
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_TITLE),
        ).joinToString(" ")
        val match = CLOCK_PATTERN.find(text) ?: return null
        val (a, b, c) = match.destructured
        val seconds = if (c.isNotEmpty()) number(a) * 3600 + number(b) * 60 + number(c) else number(a) * 60 + number(b)
        return seconds * 1000L
    }

    private fun number(digits: String): Long = digits.fold(0L) { acc, ch -> acc * 10 + Character.digit(ch, 10) }
}
