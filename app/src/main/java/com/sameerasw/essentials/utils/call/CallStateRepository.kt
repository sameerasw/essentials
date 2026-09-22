package com.sameerasw.essentials.utils.call

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallPhase { Ringing, Active }

data class CallSnapshot(
    val phase: CallPhase,
    val number: String?,
    val name: String?,
    val photo: Bitmap?,
    val incoming: Boolean,
    val startedAt: Long,
    
    val appName: String? = null,
    val answerIntent: PendingIntent? = null,
    val endIntent: PendingIntent? = null,
    val contentIntent: PendingIntent? = null,
)


object CallStateRepository {
    private val _state = MutableStateFlow<CallSnapshot?>(null)
    val state: StateFlow<CallSnapshot?> = _state.asStateFlow()

    private var telephony: CallSnapshot? = null
    private val notificationCalls = LinkedHashMap<String, NotificationCall>()
    private val activeSince = HashMap<String, Long>()

    @Synchronized
    fun onCallStateChanged(context: Context, state: Int, number: String?) {
        val previous = telephony
        telephony = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> snapshot(context, CallPhase.Ringing, number, previous, incoming = true)
            TelephonyManager.CALL_STATE_OFFHOOK -> snapshot(context, CallPhase.Active, number, previous, incoming = previous?.incoming ?: false)
            else -> null
        }
        publish()
    }

    @Synchronized
    fun onCallNotificationPosted(context: Context, sbn: StatusBarNotification) {
        val call = CallNotificationParser.parse(context, sbn) ?: return
        if (!call.ringing) activeSince.putIfAbsent(call.key, call.startedAt ?: System.currentTimeMillis())
        notificationCalls[call.key] = call
        publish()
    }

    @Synchronized
    fun onCallNotificationRemoved(key: String) {
        activeSince.remove(key)
        if (notificationCalls.remove(key) != null) publish()
    }

    private fun publish() {
        val fromNotification = notificationCalls.values
            .sortedWith(compareByDescending<NotificationCall> { it.ringing }.thenByDescending { it.postedAt })
            .firstOrNull()
        _state.value = fromNotification?.let { merge(it, telephony) } ?: telephony
    }

    private fun merge(call: NotificationCall, phone: CallSnapshot?): CallSnapshot {
        val phase = if (call.ringing) CallPhase.Ringing else CallPhase.Active
        return CallSnapshot(
            phase = phase,
            number = phone?.number,
            name = call.caller ?: phone?.name ?: phone?.number,
            photo = call.photo ?: phone?.photo,
            incoming = call.ringing || phone?.incoming == true,
            startedAt = activeSince[call.key] ?: phone?.startedAt ?: call.postedAt,
            appName = call.appName,
            answerIntent = call.answerIntent,
            endIntent = call.endIntent,
            contentIntent = call.contentIntent,
        )
    }

    private fun snapshot(context: Context, phase: CallPhase, number: String?, previous: CallSnapshot?, incoming: Boolean): CallSnapshot {
        val sameCall = previous != null && (number == null || previous.number == number)
        val resolvedNumber = number ?: previous?.number
        return CallSnapshot(
            phase = phase,
            number = resolvedNumber,
            name = if (sameCall && previous?.name != null) previous.name else CallerLookup.name(context, resolvedNumber),
            photo = if (sameCall && previous?.photo != null) previous.photo else CallerLookup.photo(context, resolvedNumber),
            incoming = incoming,
            startedAt = if (sameCall && previous?.phase == phase) previous.startedAt else System.currentTimeMillis(),
        )
    }
}
