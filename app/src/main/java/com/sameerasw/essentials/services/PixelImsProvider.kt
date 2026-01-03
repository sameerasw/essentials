package com.sameerasw.essentials.services

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.Process
import android.system.Os
import android.util.Log
import androidx.annotation.RequiresApi
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper

class PixelImsProvider : rikka.shizuku.ShizukuProvider() {
    @RequiresApi(Build.VERSION_CODES.R)
    companion object {
        const val TAG = "PixelIms"
        init {
            try {
                HiddenApiBypass.setHiddenApiExemptions("")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to set hidden api exemptions", e)
            }
        }
    }

    private var skip = false

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val uid = Os.getuid()
        var sdkUid = uid
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            try {
                val toSdkSandboxUidMethod = Process::class.java.getDeclaredMethod("toSdkSandboxUid", Int::class.javaPrimitiveType ?: Int::class.java)
                sdkUid = toSdkSandboxUidMethod.invoke(null, uid) as Int
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call toSdkSandboxUid", e)
            }
        }
        
        val callingUid = Binder.getCallingUid()
        if (callingUid != sdkUid && callingUid != Process.SHELL_UID && callingUid != 0) {
            return Bundle()
        }

        if (METHOD_SEND_BINDER == method) {
            Shizuku.addBinderReceivedListener {
                if (!skip && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    context?.let { startInstrument(it) }
                }
            }
        } else if (METHOD_GET_BINDER == method && callingUid == sdkUid && extras != null) {
            skip = true
            Shizuku.addBinderReceivedListener {
                val binder = extras.getBinder("binder")
                if (binder != null && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    startShellPermissionDelegate(binder, sdkUid)
                }
            }
        }
        return super.call(method, arg, extras)
    }

    private fun startShellPermissionDelegate(binder: IBinder, sdkUid: Int) {
        try {
            val am = getActivityManager() ?: return
            val startDelegateMethod = am.javaClass.getDeclaredMethod("startDelegateShellPermissionIdentity", Int::class.javaPrimitiveType ?: Int::class.java, Array<String>::class.java)
            startDelegateMethod.invoke(am, sdkUid, null)
            
            val data = Parcel.obtain()
            binder.transact(1, data, null, 0)
            data.recycle()
            
            val stopDelegateMethod = am.javaClass.getDeclaredMethod("stopDelegateShellPermissionIdentity")
            stopDelegateMethod.invoke(am)
        } catch (e: Exception) {
            Log.e(TAG, "Error in startShellPermissionDelegate: ${Log.getStackTraceString(e)}")
        }
    }

    private fun getActivityManager(): Any? {
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(null, Context.ACTIVITY_SERVICE) as IBinder
            
            val iActivityManagerStubClass = Class.forName("android.app.IActivityManager\$Stub")
            val asInterfaceMethod = iActivityManagerStubClass.getDeclaredMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.invoke(null, ShizukuBinderWrapper(binder))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ActivityManager", e)
            null
        }
    }

    private fun startInstrument(context: Context) {
        try {
            val am = getActivityManager() ?: return
            val iActivityManagerClass = Class.forName("android.app.IActivityManager")
            val iInstrumentationWatcherClass = Class.forName("android.app.IInstrumentationWatcher")
            val iUiAutomationConnectionClass = Class.forName("android.app.IUiAutomationConnection")
            
            val name = ComponentName(context, PixelImsInstrumentation::class.java)
            val flags = 1 or (1 shl 3)
            
            val uiAutomationConnectionClass = Class.forName("android.app.UiAutomationConnection")
            val connection = uiAutomationConnectionClass.getDeclaredConstructor().newInstance()
            
            // Standard reflection since we already granted exemptions
            val startInstrumentationMethod = iActivityManagerClass.getDeclaredMethod(
                "startInstrumentation",
                ComponentName::class.java,
                String::class.java,
                Int::class.javaPrimitiveType ?: Int::class.java,
                Bundle::class.java,
                iInstrumentationWatcherClass,
                iUiAutomationConnectionClass,
                Int::class.javaPrimitiveType ?: Int::class.java,
                String::class.java
            )
            
            startInstrumentationMethod.invoke(am, name, null, flags, Bundle(), null, connection, 0, null)
            Log.d(TAG, "Successfully started instrumentation")
        } catch (e: Exception) {
            Log.e(TAG, "Error in startInstrument: ${Log.getStackTraceString(e)}")
        }
    }
}
