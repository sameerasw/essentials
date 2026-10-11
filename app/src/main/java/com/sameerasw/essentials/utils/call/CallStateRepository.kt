package com.sameerasw.essentials.utils.call

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.service.notification.StatusBarNotification
import android.telephony.TelephonyManager
import android.telecom.TelecomManager
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
    val packageName: String? = null,
    val answerIntent: PendingIntent? = null,
    val endIntent: PendingIntent? = null,
    val contentIntent: PendingIntent? = null,
    val fromTelephony: Boolean = false,
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
        telephony = try {
            telephonySnapshot(context, state, number, previous)
        } catch (_: Exception) {
            null
        }
        if (telephony == null) dropDialerNotificationCalls(context)
        publish()
    }

    private fun telephonySnapshot(context: Context, state: Int, number: String?, previous: CallSnapshot?): CallSnapshot? =
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> snapshot(context, CallPhase.Ringing, number, previous, incoming = true)
            TelephonyManager.CALL_STATE_OFFHOOK -> snapshot(context, CallPhase.Active, number, previous, incoming = previous?.incoming ?: false)
            else -> null
        }

    @Synchronized
    fun onCallNotificationPosted(context: Context, sbn: StatusBarNotification) {
        val call = try {
            CallNotificationParser.parse(context, sbn)
        } catch (_: Exception) {
            null
        } ?: return
        if (!call.ringing) activeSince.putIfAbsent(call.key, call.startedAt ?: System.currentTimeMillis())
        notificationCalls[call.key] = call
        publish()
    }

    @Synchronized
    fun onCallNotificationRemoved(key: String) {
        activeSince.remove(key)
        if (notificationCalls.remove(key) != null) publish()
    }

    private fun dropDialerNotificationCalls(context: Context) {
        val dialer = defaultDialer(context) ?: return
        val gone = notificationCalls.values.filter { it.packageName == dialer }.map { it.key }
        gone.forEach {
            notificationCalls.remove(it)
            activeSince.remove(it)
        }
    }

    @Synchronized
    fun reconcile(context: Context, notificationKeys: Set<String>?) {
        var changed = false
        if (notificationKeys != null) {
            val gone = notificationCalls.keys.filter { it !in notificationKeys }
            gone.forEach {
                notificationCalls.remove(it)
                activeSince.remove(it)
            }
            if (gone.isNotEmpty()) changed = true
        }
        if (telephony != null && phoneIsIdle(context)) {
            telephony = null
            dropDialerNotificationCalls(context)
            changed = true
        }
        if (changed) publish()
    }

    @Suppress("DEPRECATION")
    private fun phoneIsIdle(context: Context): Boolean = try {
        (context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)?.callState == TelephonyManager.CALL_STATE_IDLE
    } catch (_: Exception) {
        false
    }

    @Synchronized
    fun clearNotificationCalls() {
        notificationCalls.clear()
        activeSince.clear()
        publish()
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
            packageName = call.packageName,
            answerIntent = call.answerIntent,
            endIntent = call.endIntent,
            contentIntent = call.contentIntent,
            fromTelephony = phone != null,
        )
    }

    private fun defaultDialer(context: Context): String? = try {
        (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage
    } catch (_: Exception) {
        null
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
            packageName = defaultDialer(context),
            fromTelephony = true,
        )
    }
}
