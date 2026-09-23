package com.sameerasw.essentials.services.tiles

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException

/**
 * Reads and changes the default data subscription through Shizuku's system-service Binder.
 * Android does not expose a public API for changing it from a regular application.
 */
internal object DataSimController {
    private const val SUB_INTERFACE = "com.android.internal.telephony.ISub"
    private const val PHONE_INTERFACE = "com.android.internal.telephony.ITelephony"
    private var exemptionsApplied = false
    @Volatile var lastKnownState: State? = null
        private set

    data class Sim(val id: Int, val slot: Int, val name: String?)
    data class State(val selected: Sim?, val next: Sim?, val count: Int)

    @Synchronized
    fun read(context: Context): State {
        val subscriptions = service("isub", SUB_INTERFACE)
        return state(context, subscriptions).also { lastKnownState = it }
    }

    @Synchronized
    fun advance(context: Context): State {
        val subscriptions = service("isub", SUB_INTERFACE)
        val previous = state(context, subscriptions)
        lastKnownState = previous
        val target = previous.next ?: return previous
        call(subscriptions, SUB_INTERFACE, "setDefaultDataSubId", arrayOf(Int::class.javaPrimitiveType!!), target.id)
        enableData(context, target.id)
        return state(context, subscriptions).also { lastKnownState = it }
    }

    private fun state(context: Context, subscriptions: Any): State {
        val selectedId = call(subscriptions, SUB_INTERFACE, "getDefaultDataSubId", emptyArray()) as Int
        val packageName = if (Shizuku.getUid() == 2000) "com.android.shell" else context.packageName
        val active = activeSubscriptions(subscriptions, packageName)
            .filter { it.simSlotIndex >= 0 }
            .sortedWith(compareBy({ it.simSlotIndex }, { it.subscriptionId }))
            .map { Sim(it.subscriptionId, it.simSlotIndex, it.displayName?.toString()?.takeIf { name -> name.isNotBlank() } ?: it.carrierName?.toString()) }
        val position = active.indexOfFirst { it.id == selectedId }
        val next = if (active.size > 1 && position >= 0) active[(position + 1) % active.size] else null
        return State(active.getOrNull(position), next, active.size)
    }

    @Suppress("UNCHECKED_CAST")
    private fun activeSubscriptions(subscriptions: Any, packageName: String): List<SubscriptionInfo> {
        val signatures = listOf(
            Pair(arrayOf(String::class.java, String::class.java, Boolean::class.javaPrimitiveType!!), arrayOf<Any?>(packageName, null, true)),
            Pair(arrayOf(String::class.java, String::class.java), arrayOf<Any?>(packageName, null)),
            Pair(arrayOf(String::class.java), arrayOf<Any?>(packageName)),
        )
        for ((types, args) in signatures) {
            try {
                return (call(subscriptions, SUB_INTERFACE, "getActiveSubscriptionInfoList", types, *args) as? List<*>)
                    ?.filterIsInstance<SubscriptionInfo>().orEmpty()
            } catch (_: NoSuchMethodException) {
                // The signature changes with the Android release.
            }
        }
        throw NoSuchMethodException("No supported subscription-list API")
    }

    private fun enableData(context: Context, subId: Int) {
        val phone = service("phone", PHONE_INTERFACE)
        val packageName = if (Shizuku.getUid() == 2000) "com.android.shell" else context.packageName
        val candidates = listOf(
            Pair(arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!, String::class.java), arrayOf<Any?>(subId, TelephonyManager.DATA_ENABLED_REASON_USER, true, if (Shizuku.getUid() == 0) null else packageName)),
            Pair(arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!), arrayOf<Any?>(subId, TelephonyManager.DATA_ENABLED_REASON_USER, true)),
        )
        for ((types, args) in candidates) {
            try {
                call(phone, PHONE_INTERFACE, "setDataEnabledForReason", types, *args)
                return
            } catch (_: NoSuchMethodException) {
                // Older Android releases use a different telephony method.
            }
        }
        for (method in listOf("setUserDataEnabled", "setDataEnabled")) {
            try {
                call(phone, PHONE_INTERFACE, method, arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!), subId, true)
                return
            } catch (_: NoSuchMethodException) {
                // Try the next supported method.
            }
        }
        throw NoSuchMethodException("No supported mobile-data enable API")
    }

    private fun service(name: String, interfaceName: String): Any {
        if (Build.VERSION.SDK_INT >= 28 && !exemptionsApplied) {
            check(HiddenApiBypass.addHiddenApiExemptions(""))
            exemptionsApplied = true
        }
        val binder = requireNotNull(SystemServiceHelper.getSystemService(name)) { "$name unavailable" }
        val stub = Class.forName("$interfaceName\$Stub")
        return requireNotNull(stub.getMethod("asInterface", IBinder::class.java).invoke(null, ShizukuBinderWrapper(binder)))
    }

    private fun call(target: Any, owner: String, name: String, types: Array<out Class<*>>, vararg args: Any?): Any? {
        val method = Class.forName(owner).getMethod(name, *types)
        try {
            return method.invoke(target, *args)
        } catch (e: InvocationTargetException) {
            throw (e.cause as? Exception ?: e)
        }
    }
}
