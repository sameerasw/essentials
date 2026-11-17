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
import com.sameerasw.essentials.utils.AppIconUtil
import com.sameerasw.essentials.utils.AppUtil


enum class CallActionRole { Answer, Decline, HangUp, Mute, Speaker, Hold, Video, Other }

data class CallNotificationAction(
    val role: CallActionRole,
    val title: String,
    val intent: PendingIntent,
    val active: Boolean? = null,
)

data class NotificationCall(
    val key: String,
    val packageName: String,
    val appName: String?,
    val caller: String?,
    val photo: Bitmap?,
    val appIcon: Bitmap? = null,
    val ringing: Boolean,
    val startedAt: Long?,
    val answerIntent: PendingIntent?,
    val endIntent: PendingIntent?,
    val contentIntent: PendingIntent?,
    val postedAt: Long,
    val extraActions: List<CallNotificationAction> = emptyList(),
    val allActions: List<CallNotificationAction> = emptyList(),
    val text: String? = null,
    val answerLabel: String? = null,
    val endLabel: String? = null,
)

object CallNotificationParser {
    
    private const val EXTRA_CALL_TYPE = "android.callType"
    private const val EXTRA_CALL_PERSON = "android.callPerson"
    private const val EXTRA_ANSWER_INTENT = "android.answerIntent"
    private const val EXTRA_DECLINE_INTENT = "android.declineIntent"
    private const val EXTRA_HANG_UP_INTENT = "android.hangUpIntent"
    private const val EXTRA_APP_LABEL = "android.appLabel"
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

    private val ICON_WORDS = listOf(
        CallActionRole.HangUp to listOf("call_end", "end_call", "hang_up", "hangup"),
        CallActionRole.Decline to listOf("decline", "reject"),
        CallActionRole.Answer to listOf("answer", "accept"),
        CallActionRole.Mute to listOf("mic", "mute"),
        CallActionRole.Speaker to listOf("speaker", "volume_up", "volume"),
        CallActionRole.Hold to listOf("phone_paused", "pause", "hold"),
        CallActionRole.Video to listOf("videocam", "video"),
    )

    private fun roleOfTitle(title: String): CallActionRole? {
        val lowered = title.lowercase()
        return ROLE_WORDS.firstOrNull { (_, words) ->
            words.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(lowered) }
        }?.first
    }

    private fun roleOfIcon(context: Context, packageName: String, action: Notification.Action): CallActionRole? {
        val name = iconName(context, packageName, action)?.lowercase() ?: return null
        return ICON_WORDS.firstOrNull { (_, words) -> words.any { it in name } }?.first
    }

    private fun iconName(context: Context, packageName: String, action: Notification.Action): String? = try {
        val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) action.getIcon() else null
        if (icon == null || icon.type != Icon.TYPE_RESOURCE) {
            null
        } else {
            val pkg = icon.resPackage?.takeIf { it.isNotBlank() } ?: packageName
            context.packageManager.getResourcesForApplication(pkg).getResourceEntryName(icon.resId)
        }
    } catch (_: Exception) {
        null
    }

    private fun classify(
        context: Context,
        packageName: String,
        action: Notification.Action,
        intent: PendingIntent,
        title: String,
        answer: PendingIntent?,
        decline: PendingIntent?,
        hangUp: PendingIntent?,
    ): CallNotificationAction {
        val semantic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) action.semanticAction else Notification.Action.SEMANTIC_ACTION_NONE
        val role = when {
            intent == answer -> CallActionRole.Answer
            intent == decline -> CallActionRole.Decline
            intent == hangUp -> CallActionRole.HangUp
            semantic == Notification.Action.SEMANTIC_ACTION_MUTE || semantic == Notification.Action.SEMANTIC_ACTION_UNMUTE -> CallActionRole.Mute
            semantic == Notification.Action.SEMANTIC_ACTION_CALL -> CallActionRole.Answer
            else -> roleOfIcon(context, packageName, action) ?: roleOfTitle(title) ?: CallActionRole.Other
        }
        val active = when {
            role != CallActionRole.Mute -> null
            semantic == Notification.Action.SEMANTIC_ACTION_UNMUTE -> true
            semantic == Notification.Action.SEMANTIC_ACTION_MUTE -> false
            Regex("\\bunmute\\b").containsMatchIn(title.lowercase()) -> true
            else -> null
        }
        return CallNotificationAction(role, title, intent, active)
    }

    fun isCall(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        if (n.category == Notification.CATEGORY_CALL) return true
        val extras = n.extras ?: return false
        if (extras.getInt(EXTRA_CALL_TYPE, 0) > 0) return true
        return extras.getString(Notification.EXTRA_TEMPLATE)?.endsWith("\$CallStyle") == true
    }

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

        val answerExtra = pendingIntent(extras, EXTRA_ANSWER_INTENT)
        val declineExtra = pendingIntent(extras, EXTRA_DECLINE_INTENT)
        val hangUpExtra = pendingIntent(extras, EXTRA_HANG_UP_INTENT)
        val classified = n.actions.orEmpty().mapNotNull { a ->
            val intent = a.actionIntent ?: return@mapNotNull null
            if (!a.remoteInputs.isNullOrEmpty()) return@mapNotNull null
            val title = a.title?.toString()?.trim().orEmpty()
            classify(context, sbn.packageName, a, intent, title, answerExtra, declineExtra, hangUpExtra)
        }
        fun actionFor(role: CallActionRole) = classified.firstOrNull { it.role == role }?.intent
        fun labelFor(role: CallActionRole) = classified.firstOrNull { it.role == role && it.title.isNotBlank() }?.title
        val answer = answerExtra ?: actionFor(CallActionRole.Answer)
        val hangUp = hangUpExtra ?: actionFor(CallActionRole.HangUp)
        val decline = declineExtra ?: actionFor(CallActionRole.Decline)
        val extraActions = classified.filter {
            it.role !in setOf(CallActionRole.Answer, CallActionRole.Decline, CallActionRole.HangUp) && it.title.isNotBlank()
        }

        val callType = extras.getInt(EXTRA_CALL_TYPE, 0)
        val ringing = when (callType) {
            CALL_TYPE_INCOMING, CALL_TYPE_SCREENING -> true
            CALL_TYPE_ONGOING -> false
            else -> answer != null
        }
        val chronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER) &&
            !extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)
        val startedAt = n.`when`.takeIf { !ringing && chronometer && it > 0L && it <= System.currentTimeMillis() }

        val pm = context.packageManager
        val appInfo = try {
            pm.getApplicationInfo(sbn.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val appName = extras.getCharSequence(EXTRA_APP_LABEL)?.toString()?.takeIf { it.isNotBlank() }
            ?: appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
        val appIcon = runCatching { AppUtil.drawableToBitmap(AppIconUtil.islandAppIcon(context, sbn.packageName)) }.getOrNull()

        return NotificationCall(
            key = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            caller = caller,
            photo = photo,
            appIcon = appIcon,
            ringing = ringing,
            startedAt = startedAt,
            answerIntent = answer,
            endIntent = if (ringing) decline ?: hangUp else hangUp ?: decline,
            contentIntent = n.contentIntent,
            postedAt = sbn.postTime,
            extraActions = extraActions,
            allActions = classified.filter { it.title.isNotBlank() },
            text = (extras.getCharSequence(Notification.EXTRA_TEXT) ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
                ?.toString()?.trim()?.takeIf { it.isNotBlank() && it != caller },
            answerLabel = labelFor(CallActionRole.Answer),
            endLabel = if (ringing) labelFor(CallActionRole.Decline) ?: labelFor(CallActionRole.HangUp) else labelFor(CallActionRole.HangUp) ?: labelFor(CallActionRole.Decline),
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
