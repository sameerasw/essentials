package com.sameerasw.essentials.services

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.service.notification.Condition
import android.service.notification.ConditionProviderService
import android.util.Log

@Suppress("DEPRECATION")
class EssentialsConditionProvider : ConditionProviderService() {

    companion object {
        private const val TAG = "EssentialsCPS"
        val CONDITION_URI: Uri = Uri.parse("condition://com.sameerasw.essentials/focus")
        
        private var instance: EssentialsConditionProvider? = null

        fun setConditionState(context: Context, isActive: Boolean) {
             Log.d(TAG, "Requesting condition state: $isActive")
             val provider = instance
             if (provider != null) {
                 provider.notifyChange(isActive)
             } else {
                 try {
                     requestRebind(ComponentName(context, EssentialsConditionProvider::class.java))
                 } catch (e: Exception) {
                     e.printStackTrace()
                 }
             }
        }
    }

    override fun onConnected() {

        Log.d(TAG, "onConnected")
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        instance = null
    }

    override fun onSubscribe(conditionId: Uri?) {
        Log.d(TAG, "onSubscribe: $conditionId")
        notifyChange(false) 
    }

    override fun onUnsubscribe(conditionId: Uri?) {
         Log.d(TAG, "onUnsubscribe: $conditionId")
    }

    private fun notifyChange(active: Boolean) {
         Log.d(TAG, "notifyChange: active=$active")
         val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
         val condition = Condition(CONDITION_URI, "Essentials Focus", state)
         try {
             notifyCondition(condition)
         } catch (e: Exception) {
             Log.e(TAG, "Failed to notify condition", e)
         }
    }
}
