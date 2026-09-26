package com.sameerasw.essentials.services.tiles

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.sameerasw.essentials.utils.RootUtils
import com.sameerasw.essentials.utils.ShellUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException

internal object DataSimController {
    private const val SUB_INTERFACE = "com.android.internal.telephony.ISub"
    private const val PHONE_INTERFACE = "com.android.internal.telephony.ITelephony"
    private var exemptionsApplied = false
    @Volatile var lastKnownState: State? = null
        private set

    data class Sim(val id: Int, val slot: Int, val name: String?)
    data class State(val selected: Sim?, val next: Sim?, val count: Int, val dataEnableFailed: Boolean = false)

    @Synchronized
    fun read(context: Context): State {
        return try {
            state(context).also { lastKnownState = it }
        } catch (e: Exception) {
            lastKnownState = null
            throw e
        }
    }

    @Synchronized
    fun advance(context: Context): State {
        val previous = state(context)
        lastKnownState = previous
        val target = previous.next ?: return previous
        val useRoot = ShellUtils.isRootEnabled(context)
        if (useRoot) {
            switchWithRoot(target.id)
        } else {
            val subscriptions = service("isub", SUB_INTERFACE)
            call(subscriptions, SUB_INTERFACE, "setDefaultDataSubId", arrayOf(Int::class.javaPrimitiveType!!), target.id)
        }
        val updated = state(context)
        check(updated.selected?.id == target.id) { "Default data SIM did not change" }
        lastKnownState = updated
        val dataEnableFailed =
            try {
                if (useRoot) {
                    check(RootUtils.runCommand("svc data enable")) { "Could not enable mobile data" }
                } else {
                    enableData(context, target.id)
                }
                false
            } catch (e: Exception) {
                Log.w("DataSimController", "Default data SIM changed, but mobile data could not be enabled", e)
                true
            }
        return updated.copy(dataEnableFailed = dataEnableFailed).also { lastKnownState = it }
    }

    private fun state(context: Context): State {
        val selectedId = SubscriptionManager.getDefaultDataSubscriptionId()
        val subscriptions = requireNotNull(context.getSystemService(SubscriptionManager::class.java))
        val active = subscriptions.activeSubscriptionInfoList.orEmpty()
            .filter { it.simSlotIndex >= 0 }
            .sortedWith(compareBy({ it.simSlotIndex }, { it.subscriptionId }))
            .map { Sim(it.subscriptionId, it.simSlotIndex, it.displayName?.toString()?.takeIf { name -> name.isNotBlank() } ?: it.carrierName?.toString()) }
        val position = active.indexOfFirst { it.id == selectedId }
        val next = if (active.size > 1 && position >= 0) active[(position + 1) % active.size] else null
        return State(active.getOrNull(position), next, active.size)
    }

    private fun switchWithRoot(subId: Int) {
        applyHiddenApiExemptions()
        val code = Class.forName("$SUB_INTERFACE\$Stub")
            .getDeclaredField("TRANSACTION_setDefaultDataSubId")
            .apply { isAccessible = true }
            .getInt(null)
        val process = requireNotNull(RootUtils.newProcess(arrayOf("service", "call", "isub", code.toString(), "i32", subId.toString()))) {
            "Could not start root service call"
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0 && output.contains("Result: Parcel(")) { "Root service call failed: $output" }
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
            }
        }
        for (method in listOf("setUserDataEnabled", "setDataEnabled")) {
            try {
                call(phone, PHONE_INTERFACE, method, arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!), subId, true)
                return
            } catch (_: NoSuchMethodException) {
            }
        }
        throw NoSuchMethodException("No supported mobile-data enable API")
    }

    private fun service(name: String, interfaceName: String): Any {
        applyHiddenApiExemptions()
        val binder = requireNotNull(SystemServiceHelper.getSystemService(name)) { "$name unavailable" }
        val stub = Class.forName("$interfaceName\$Stub")
        return requireNotNull(stub.getMethod("asInterface", IBinder::class.java).invoke(null, ShizukuBinderWrapper(binder)))
    }

    private fun applyHiddenApiExemptions() {
        if (Build.VERSION.SDK_INT >= 28 && !exemptionsApplied) {
            check(HiddenApiBypass.addHiddenApiExemptions("Lcom/android/internal/telephony/"))
            exemptionsApplied = true
        }
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
