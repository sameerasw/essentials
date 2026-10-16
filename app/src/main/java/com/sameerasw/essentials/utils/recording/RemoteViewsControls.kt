package com.sameerasw.essentials.utils.recording

import android.app.PendingIntent
import android.content.Context
import android.service.notification.StatusBarNotification
import android.widget.RemoteViews
import java.lang.reflect.Field

class RecorderControls(
    val stop: PendingIntent?,
    val pauseResume: PendingIntent?,
    // Chronometer base in elapsedRealtime, and whether it is ticking (false while paused).
    val chronometerBase: Long?,
    val chronometerRunning: Boolean?,
)

// Recorders with fully custom notifications (e.g. Motorola) put stop/pause on views inside the layout
// instead of notification actions. Read the click handlers and chronometer calls recorded in the RemoteViews.
object RemoteViewsControls {
    fun read(context: Context, sbn: StatusBarNotification): RecorderControls? {
        val views = sbn.notification.bigContentView ?: sbn.notification.contentView ?: return null
        val names = try {
            context.packageManager.getResourcesForApplication(sbn.packageName)
        } catch (_: Exception) {
            return null
        }
        var stop: PendingIntent? = null
        var pauseResume: PendingIntent? = null
        var base: Long? = null
        var running: Boolean? = null
        actions(views).forEach { action ->
            val viewId = field(action, "mViewId", "viewId") as? Int ?: return@forEach
            val name = try {
                names.getResourceEntryName(viewId).lowercase()
            } catch (_: Exception) {
                return@forEach
            }
            pendingIntent(action, depth = 2)?.let { intent ->
                when {
                    "stop" in name -> stop = intent
                    "pause" in name || "resume" in name -> pauseResume = intent
                }
            }
            if ("chronometer" in name) {
                val value = field(action, "mValue", "value")
                when (field(action, "mMethodName", "methodName")) {
                    "setBase" -> base = value as? Long
                    "setStarted" -> running = value as? Boolean
                }
            }
        }
        if (stop == null && pauseResume == null && base == null) return null
        return RecorderControls(stop, pauseResume, base, running)
    }

    private fun actions(views: RemoteViews): List<Any> =
        (field(views, "mActions") as? List<*>)?.filterNotNull().orEmpty()

    private fun pendingIntent(target: Any, depth: Int): PendingIntent? {
        fields(target.javaClass).forEach { f ->
            val value = try {
                f.get(target)
            } catch (_: Exception) {
                null
            } ?: return@forEach
            if (value is PendingIntent) return value
            if (depth > 0 && value.javaClass.name.startsWith("android.widget.RemoteViews")) {
                pendingIntent(value, depth - 1)?.let { return it }
            }
        }
        return null
    }

    private fun field(target: Any, vararg names: String): Any? {
        val all = fields(target.javaClass)
        val match = names.firstNotNullOfOrNull { name -> all.firstOrNull { it.name == name } } ?: return null
        return try {
            match.get(target)
        } catch (_: Exception) {
            null
        }
    }

    private fun fields(type: Class<*>): List<Field> =
        generateSequence(type) { it.superclass }
            .takeWhile { it != Any::class.java }
            .flatMap { it.declaredFields.asSequence() }
            .onEach { it.isAccessible = true }
            .toList()
}
