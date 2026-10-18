package com.sameerasw.essentials.utils.call

import android.app.Notification
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.sameerasw.essentials.utils.AppUtil


enum class CallActionRole { Answer, Decline, HangUp, Mute, Speaker, Hold, Video, Other }

data class CallNotificationAction(
    val role: CallActionRole,
    val title: String,
    val intent: PendingIntent,
)

data class NotificationCall(
    val key: String,
    val packageName: String,
    val appName: String?,
    val caller: String?,
    val photo: Bitmap?,
    val ringing: Boolean,
    val startedAt: Long?,
    val answerIntent: PendingIntent?,
    val endIntent: PendingIntent?,
    val contentIntent: PendingIntent?,
    val postedAt: Long,
    val extraActions: List<CallNotificationAction> = emptyList(),
    val allActions: List<CallNotificationAction> = emptyList(),
)

object CallNotificationParser {
    
    private const val EXTRA_CALL_TYPE = "android.callType"
    private const val EXTRA_CALL_PERSON = "android.callPerson"
    private const val EXTRA_ANSWER_INTENT = "android.answerIntent"
    private const val EXTRA_DECLINE_INTENT = "android.declineIntent"
    private const val EXTRA_HANG_UP_INTENT = "android.hangUpIntent"
    private const val CALL_TYPE_INCOMING = 1
    private const val CALL_TYPE_ONGOING = 2
    private const val CALL_TYPE_SCREENING = 3

    private val ROLE_WORDS = listOf(
        CallActionRole.Mute to listOf("mute", "unmute"),
        CallActionRole.Speaker to listOf("speaker", "speakerphone"),
        CallActionRole.Hold to listOf("hold", "resume"),
        CallActionRole.Video to listOf("video", "camera"),
        CallActionRole.Answer to listOf("answer", "accept", "pick up"),
        CallActionRole.Decline to listOf("decline", "reject", "dismiss"),
        CallActionRole.HangUp to listOf("hang up", "hangup", "end", "leave"),
    )

    private fun roleOf(title: String): CallActionRole {
        val lowered = title.lowercase()
        return ROLE_WORDS.firstOrNull { (_, words) ->
            words.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lowered) }
        }?.first ?: CallActionRole.Other
    }

    fun isCall(sbn: StatusBarNotification): Boolean = sbn.notification.category == Notification.CATEGORY_CALL

    @Suppress("DEPRECATION")
    fun parse(context: Context, sbn: StatusBarNotification): NotificationCall? {
        if (!isCall(sbn)) return null
        val n = sbn.notification
        val extras = n.extras
        val declaredType = extras.getInt(EXTRA_CALL_TYPE, 0)
        if (declaredType == 0 && !sbn.isOngoing && n.fullScreenIntent == null) return null

        val person: Person? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(EXTRA_CALL_PERSON, Person::class.java)
            } else {
                extras.getParcelable(EXTRA_CALL_PERSON)
            }
        } else {
            null
        }
        val caller = person?.name?.toString()?.takeIf { it.isNotBlank() }
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }

        val photo = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) person?.icon else null)?.let { iconBitmap(context, it) }
            ?: n.getLargeIcon()?.let { iconBitmap(context, it) }

        val classified = n.actions.orEmpty().mapNotNull { a ->
            val intent = a.actionIntent ?: return@mapNotNull null
            if (!a.remoteInputs.isNullOrEmpty()) return@mapNotNull null
            val title = a.title?.toString()?.trim().orEmpty()
            CallNotificationAction(roleOf(title), title, intent)
        }
        fun actionFor(role: CallActionRole) = classified.firstOrNull { it.role == role }?.intent
        val answer = pendingIntent(extras, EXTRA_ANSWER_INTENT) ?: actionFor(CallActionRole.Answer)
        val hangUp = pendingIntent(extras, EXTRA_HANG_UP_INTENT) ?: actionFor(CallActionRole.HangUp)
        val decline = pendingIntent(extras, EXTRA_DECLINE_INTENT) ?: actionFor(CallActionRole.Decline)
        val extraActions = classified.filter {
            it.role !in setOf(CallActionRole.Answer, CallActionRole.Decline, CallActionRole.HangUp) && it.title.isNotBlank()
        }

        val callType = extras.getInt(EXTRA_CALL_TYPE, 0)
        val ringing = when (callType) {
            CALL_TYPE_INCOMING, CALL_TYPE_SCREENING -> true
            CALL_TYPE_ONGOING -> false
            else -> answer != null
        }
        val startedAt = if (!ringing && (n.`when` > 0L) && (extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) || n.`when` <= System.currentTimeMillis())) {
            n.`when`
        } else {
            null
        }

        val appName = try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (_: Exception) {
            null
        }

        return NotificationCall(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            caller = caller,
            photo = photo,
            ringing = ringing,
            startedAt = startedAt,
            answerIntent = answer,
            endIntent = if (ringing) decline ?: hangUp else hangUp ?: decline,
            contentIntent = n.contentIntent,
            postedAt = sbn.postTime,
            extraActions = extraActions,
            allActions = classified.filter { it.title.isNotBlank() },
        )
    }

    @Suppress("DEPRECATION")
    private fun pendingIntent(extras: Bundle, key: String): PendingIntent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(key, PendingIntent::class.java)
        } else {
            extras.getParcelable(key)
        }

    private fun iconBitmap(context: Context, icon: Icon): Bitmap? = try {
        icon.loadDrawable(context)?.let { AppUtil.drawableToBitmap(it) }
    } catch (_: Exception) {
        null
    }
}
