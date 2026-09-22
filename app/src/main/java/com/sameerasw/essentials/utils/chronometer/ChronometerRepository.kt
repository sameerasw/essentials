package com.sameerasw.essentials.utils.chronometer

import android.os.SystemClock
import android.os.Build
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.utils.AppUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class ChronometerAction(val title: String, val intent: PendingIntent)

data class ChronometerEntry(
    val key: String,
    val packageName: String,
    val appName: String?,
    val appIcon: Bitmap?,
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

    fun isCandidate(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (n.category == Notification.CATEGORY_CALL || n.category == Notification.CATEGORY_TRANSPORT) return false
        if (n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return false
        val chronometer = n.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || timeMetric(n.extras) != null
        
        return (chronometer && sbn.isOngoing) || synchronized(this) { tracked.containsKey(sbn.key) }
    }

    @Synchronized
    fun onPosted(context: Context, sbn: StatusBarNotification) {
        val n = sbn.notification
        val extras = n.extras
        val previous = tracked[sbn.key]
        if (!sbn.isOngoing) {
            remove(sbn.key)
            return
        }
        val metric = timeMetric(extras)
        val now = System.currentTimeMillis()
        val running: Boolean
        val countDown: Boolean
        val base: Long
        val frozen: Long
        if (metric != null) {
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
            AppUtil.drawableToBitmap(context.packageManager.getApplicationIcon(sbn.packageName))
        } catch (_: Exception) {
            null
        }
        tracked[sbn.key] = ChronometerEntry(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            appIcon = appIcon,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
                ?: metric?.label?.takeIf { it.isNotBlank() },
            countDown = countDown,
            base = base,
            running = running,
            frozenMillis = frozen,
            actions = n.actions.orEmpty().mapNotNull { a ->
                val title = a.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                a.actionIntent?.let { ChronometerAction(title, it) }
            },
            contentIntent = n.contentIntent,
            postedAt = sbn.postTime,
        )
        publish()
    }

    @Synchronized
    fun onRemoved(key: String) = remove(key)

    private fun remove(key: String) {
        if (tracked.remove(key) != null) publish()
    }

    private fun publish() {
        _entries.value = tracked.values.toList()
    }

    
    private fun parseClock(extras: Bundle): Long? {
        val text = listOfNotNull(
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_TITLE),
        ).joinToString(" ")
        val match = Regex("""(\d{1,2}):(\d{2})(?::(\d{2}))?""").find(text) ?: return null
        val (a, b, c) = match.destructured
        val seconds = if (c.isNotEmpty()) a.toLong() * 3600 + b.toLong() * 60 + c.toLong() else a.toLong() * 60 + b.toLong()
        return seconds * 1000L
    }
}
